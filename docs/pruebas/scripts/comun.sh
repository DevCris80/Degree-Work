#!/usr/bin/env bash
# Funciones comunes de los scripts de medición. Se cargan con `source comun.sh`.
#
# Variables que se pueden cambiar desde fuera:
#   ADB        ruta de adb (por defecto, la del SDK en ~/Android/Sdk)
#   PAQUETE    app que recibe los pesajes; la copia de prueba lleva el sufijo .prueba
#   IP         IP del celular en el wifi; si no se da, se lee con adb
#   PUERTO     puerto del servidor de pesajes (contrato-pesaje.md)
#   SERIAL     celular al que apunta adb, si hay más de uno

ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PAQUETE="${PAQUETE:-com.devcris80.prototipo.prueba}"
PUERTO="${PUERTO:-8080}"
[ -n "${SERIAL:-}" ] && export ANDROID_SERIAL="$SERIAL"

# IP del celular en la red local. El ESP32 real también llega por la red, no por adb.
ip_del_celular() {
  if [ -n "${IP:-}" ]; then echo "$IP"; return; fi
  "$ADB" shell ip -f inet addr show wlan0 | awk '/inet /{sub("/.*","",$2); print $2; exit}'
}

ahora_ms() { date +%s%3N; }

# envio <id_lectura> <id_chip> <peso> [antiguedad_ms]
# Imprime "<código HTTP> <milisegundos que tardó la respuesta>". Código 000 = sin respuesta.
envio() {
  local cuerpo
  cuerpo=$(printf '{"id_lectura":"%s","id_chip":"%s","peso":%s%s}' "$1" "$2" "$3" "${4:+,\"antiguedad_ms\":$4}")
  curl -s -o /dev/null --max-time 10 -X POST "http://$(ip_del_celular):$PUERTO/pesaje" \
    -H 'Content-Type: application/json' -d "$cuerpo" \
    -w '%{http_code} %{time_total}\n' | awk '{printf "%s %d\n", $1, $2*1000}'
}

# sacar_base <carpeta>: copia la base Room de la app de prueba (solo funciona con la app de depuración).
sacar_base() {
  mkdir -p "$1"
  for f in app-bovina.db app-bovina.db-wal app-bovina.db-shm; do
    "$ADB" exec-out run-as "$PAQUETE" cat "databases/$f" >"$1/$f" 2>/dev/null || rm -f "$1/$f"
  done
}

# contar_lecturas <carpeta> <prefijo>: cuántos registros y cuántos pesajes pendientes tienen un id_lectura con ese prefijo.
contar_lecturas() {
  python3 -I - "$1/app-bovina.db" "$2" <<'PY'
import sqlite3, sys
con = sqlite3.connect(sys.argv[1])
p = sys.argv[2] + "%"
r = con.execute("select count(*), count(distinct idLectura) from registro where idLectura like ?", (p,)).fetchone()
q = con.execute("select count(*), count(distinct idLectura) from pesaje_pendiente where idLectura like ?", (p,)).fetchone()
print(f"registros={r[0]} registros_distintos={r[1]} pendientes={q[0]} pendientes_distintos={q[1]} total={r[0]+q[0]}")
PY
}

estado_celular() {
  echo "bateria=$("$ADB" shell dumpsys battery | awk '/level/{print $2}')%"
  echo "pantalla=$("$ADB" shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*' | head -1)"
  echo "servicio_activo=$("$ADB" shell dumpsys activity services "$PAQUETE" | grep -c 'PesajeHttpForegroundService')"
}
