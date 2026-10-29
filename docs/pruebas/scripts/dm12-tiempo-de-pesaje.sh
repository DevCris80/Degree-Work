#!/usr/bin/env bash
# DM-12 (RNF-05): tiempo desde que la app recibe un POST /pesaje válido hasta que el pesaje es visible.
#
# Envía 30 pesajes seguidos al animal cuya chapeta se indica y anota la hora de cada envío con
# milisegundos. Con la pantalla del detalle de ese animal abierta y grabada (adb shell screenrecord),
# la hora de envío de cada línea se ubica en el video y se lee en qué instante aparece el pesaje.
#
# El script mide además la respuesta HTTP: la app responde 201 después de confirmar la transacción
# en Room, así que ese tiempo es una cota inferior de "visible" y sirve para ver si hay algún envío lento.
# La cota del requisito (≤ 5 s) se aprueba o no con lo que se ve en el video.
#
# Uso: dm12-tiempo-de-pesaje.sh <CHIP de un animal activo> [N=30] [pausa en segundos=3]
set -u
cd "$(dirname "$0")" && source ./comun.sh

CHIP="${1:?falta el código de la chapeta de un animal activo}"; N="${2:-30}"; PAUSA="${3:-3}"
PREFIJO="dm12-$(date +%Y%m%d%H%M%S)"
SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"
echo "n,id_lectura,hora_envio,codigo,respuesta_ms" >"$SALIDA/tiempos.csv"

for i in $(seq 1 "$N"); do
  hora=$(date +%H:%M:%S.%3N)
  read -r codigo ms < <(envio "$PREFIJO-$i" "$CHIP" "$((300 + i)).5")
  echo "$i,$PREFIJO-$i,$hora,$codigo,$ms" | tee -a "$SALIDA/tiempos.csv"
  sleep "$PAUSA"
done

python3 -I - "$SALIDA/tiempos.csv" <<'PY' | tee "$SALIDA/resumen.txt"
import csv, sys
f = list(csv.DictReader(open(sys.argv[1])))
ok = [r for r in f if r["codigo"] == "201"]
ms = sorted(int(r["respuesta_ms"]) for r in ok)
print(f"envios={len(f)} respondidos_201={len(ok)} otros={[r['codigo'] for r in f if r['codigo'] != '201']}")
if ms:
    print(f"respuesta_ms: min={ms[0]} mediana={ms[len(ms)//2]} max={ms[-1]} (limite del requisito: 5000)")
PY
