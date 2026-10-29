#!/usr/bin/env bash
# DM-04 (RF-20 a RF-24): jornada de pesaje como la haría el ESP32. Se repite sin internet para RNF-01.
#
# Precondición: la app con sesión y el servicio de pesaje activo; el celular y esta máquina en la misma
# red local. Para la prueba sin internet, esa red no debe tener salida (por ejemplo, el hotspot de otro
# celular sin datos); este script no corta la conexión.
#
# Envía, con pausa entre cada paso para que se vea en un video:
#   1. un pesaje de un animal con chapeta activa                  -> 201
#   2. un pesaje con un código sin chapeta                        -> 202, motivo CHIP_NO_ASOCIADO
#   3. el mismo envío del paso 1 (reintento)                      -> la misma respuesta, sin otro pesaje
#   4. un cuerpo sin `peso`                                       -> 400
#   5. un pesaje con antiguedad_ms = 60000                        -> la hora queda un minuto antes
# y después copia la base del celular y revisa lo guardado.
#
# Uso: dm04-jornada-de-pesaje.sh <CHIP con chapeta activa> [pausa en segundos=3]
set -u
cd "$(dirname "$0")" && source ./comun.sh

CHIP="${1:?falta el código de una chapeta activa}"; PAUSA="${2:-3}"
PREFIJO="dm04-$(date +%Y%m%d%H%M%S)"
SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"; REG="$SALIDA/registro.log"; : >"$REG"
IP=$(ip_del_celular)

paso() { # paso <descripción> <cuerpo JSON>
  echo "[$(date +%T.%3N)] $1" | tee -a "$REG"
  curl -s --max-time 5 -X POST "http://$IP:$PUERTO/pesaje" -H 'Content-Type: application/json' -d "$2" \
    -w '\nHTTP=%{http_code}\n' | tee -a "$REG"
  sleep "$PAUSA"
}

paso "paso 1: chapeta activa $CHIP" "{\"id_lectura\":\"$PREFIJO-1\",\"id_chip\":\"$CHIP\",\"peso\":451.5}"
paso "paso 2: código sin chapeta" "{\"id_lectura\":\"$PREFIJO-2\",\"id_chip\":\"DM04SINCHAPETA\",\"peso\":300.0}"
paso "paso 3: reintento del paso 1 con el mismo id_lectura" "{\"id_lectura\":\"$PREFIJO-1\",\"id_chip\":\"$CHIP\",\"peso\":451.5}"
paso "paso 4: cuerpo sin peso" "{\"id_lectura\":\"$PREFIJO-4\",\"id_chip\":\"$CHIP\"}"
T=$(ahora_ms)
paso "paso 5: antiguedad_ms=60000 (envío en epoch ms=$T)" "{\"id_lectura\":\"$PREFIJO-5\",\"id_chip\":\"$CHIP\",\"peso\":452.5,\"antiguedad_ms\":60000}"

sacar_base "$SALIDA/base"
python3 -I - "$SALIDA/base/app-bovina.db" "$T" "$PREFIJO" <<'PY' | tee -a "$REG"
import sqlite3, sys
c = sqlite3.connect(sys.argv[1]); t = int(sys.argv[2]); p = sys.argv[3] + "-%"
print("registros:", c.execute("select idLectura, peso, sincronizado from registro where idLectura like ? order by idLectura", (p,)).fetchall())
print("pendientes:", c.execute("select idLectura, codigo, motivo, sincronizado from pesaje_pendiente where idLectura like ?", (p,)).fetchall())
r = c.execute("select timestamp from registro where idLectura like ?", (sys.argv[3] + "-5",)).fetchone()
print("paso 5: envío - hora guardada =", t - r[0], "ms (esperado ≈ 60000)")
PY
