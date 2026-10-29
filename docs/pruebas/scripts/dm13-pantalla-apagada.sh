#!/usr/bin/env bash
# DM-13 (RNF-03): el servicio de pesaje sigue recibiendo pesajes con la pantalla apagada ≥ 15 min.
#
# Precondición: la app de prueba abierta, con sesión, el servicio de pesaje iniciado y el celular
# conectado al mismo wifi que esta máquina. Apaga la pantalla, envía un pesaje cada INTERVALO
# segundos durante MINUTOS minutos, enciende la pantalla y compara lo recibido con lo enviado.
# Los pesajes viajan por el wifi, igual que los del ESP32, no por adb.
#
# Uso: dm13-pantalla-apagada.sh [CHIP=DM13SINASOCIAR] [MINUTOS=15] [INTERVALO=30]
# Aprobado si todos los envíos reciben 201 o 202, todos quedan en la base y el servicio sigue activo.
set -u
cd "$(dirname "$0")" && source ./comun.sh

CHIP="${1:-DM13SINASOCIAR}"; MINUTOS="${2:-15}"; INTERVALO="${3:-30}"
PREFIJO="dm13-$(date +%Y%m%d%H%M%S)"
SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"

{ echo "DM-13 | chip=$CHIP minutos=$MINUTOS intervalo=${INTERVALO}s"; echo "--- antes"; estado_celular; } | tee "$SALIDA/resumen.txt"

"$ADB" shell input keyevent KEYCODE_SLEEP
sleep 3
echo "pantalla apagada: $("$ADB" shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*' | head -1)" | tee -a "$SALIDA/resumen.txt"

inicio=$(date +%s); enviados=0; respondidos=0
echo "n,hora,codigo,respuesta_ms" >"$SALIDA/envios.csv"
while [ $(( $(date +%s) - inicio )) -lt $((MINUTOS * 60)) ]; do
  enviados=$((enviados + 1))
  read -r codigo ms < <(envio "$PREFIJO-$enviados" "$CHIP" "$((400 + enviados)).5")
  echo "$enviados,$(date +%H:%M:%S),$codigo,$ms" | tee -a "$SALIDA/envios.csv"
  case "$codigo" in 201|202) respondidos=$((respondidos + 1));; esac
  sleep "$INTERVALO"
done

"$ADB" shell input keyevent KEYCODE_WAKEUP
sleep 2
{ echo "--- después"; estado_celular; } | tee -a "$SALIDA/resumen.txt"

sacar_base "$SALIDA/base"
cuenta=$(contar_lecturas "$SALIDA/base" "$PREFIJO"); echo "$cuenta" | tee -a "$SALIDA/resumen.txt"
guardados=$(echo "$cuenta" | sed -E 's/.*total=([0-9]+)/\1/')
echo "enviados=$enviados respondidos_201_o_202=$respondidos guardados=$guardados" | tee -a "$SALIDA/resumen.txt"
if [ "$respondidos" = "$enviados" ] && [ "$guardados" = "$enviados" ]; then
  echo "RESULTADO: APROBADO" | tee -a "$SALIDA/resumen.txt"
else
  echo "RESULTADO: FALLÓ" | tee -a "$SALIDA/resumen.txt"; exit 1
fi
