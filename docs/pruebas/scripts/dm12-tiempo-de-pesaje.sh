#!/usr/bin/env bash
# DM-12 (RNF-05): tiempo desde que la app recibe un POST /pesaje válido hasta que el pesaje es visible
# en el detalle abierto del animal.
#
# Precondición: la app con sesión, el servicio de pesaje activo y el detalle del animal cuya chapeta se
# da abierto en pantalla (con la pantalla encendida). El detalle muestra «Último peso registrado: <peso> kg».
#
# Cada pesaje lleva un peso distinto (601.5, 602.5, …). Tras enviarlo, el script consulta la pantalla
# con `uiautomator dump` hasta ver ese peso y calcula, con el reloj de esta máquina, cuánto pasó desde
# el envío. El tiempo es una cota superior: incluye lo que tarda la consulta de la pantalla (unos 2,5 s).
# Si la cota ya es ≤ 5 s, el pesaje fue visible en ≤ 5 s. Se puede grabar la pantalla además
# (`adb shell screenrecord`) como evidencia.
#
# Uso: dm12-tiempo-de-pesaje.sh <CHIP del animal con el detalle abierto> [N=30] [pausa en segundos=2]
set -u
cd "$(dirname "$0")" && source ./comun.sh

CHIP="${1:?falta el código de la chapeta del animal con el detalle abierto}"; N="${2:-30}"; PAUSA="${3:-2}"
PREFIJO="dm12-$(date +%Y%m%d%H%M%S)"
SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"
echo "n,id_lectura,peso,hora_envio,codigo,respuesta_ms,visible_ms" >"$SALIDA/tiempos.csv"
LIMITE_MS=5000; ESPERA_MAX_MS=15000

for i in $(seq 1 "$N"); do
  peso="$((600 + i)).5"
  hora=$(date +%H:%M:%S.%3N)
  t0=$(ahora_ms)
  read -r codigo ms < <(envio "$PREFIJO-$i" "$CHIP" "$peso")
  visible=-1
  while [ $(( $(ahora_ms) - t0 )) -lt $ESPERA_MAX_MS ]; do
    if "$ADB" exec-out 'uiautomator dump /sdcard/dm12-ui.xml >/dev/null 2>&1; cat /sdcard/dm12-ui.xml' | grep -qF "$peso kg"; then
      visible=$(( $(ahora_ms) - t0 )); break
    fi
  done
  echo "$i,$PREFIJO-$i,$peso,$hora,$codigo,$ms,$visible" | tee -a "$SALIDA/tiempos.csv"
  sleep "$PAUSA"
done

python3 -I - "$SALIDA/tiempos.csv" "$LIMITE_MS" <<'PY' | tee "$SALIDA/resumen.txt"
import csv, sys
limite = int(sys.argv[2])
f = list(csv.DictReader(open(sys.argv[1])))
vis = sorted(int(r["visible_ms"]) for r in f if int(r["visible_ms"]) >= 0)
no_vistos = [r["n"] for r in f if int(r["visible_ms"]) < 0]
print(f"envios={len(f)} respondidos_201={sum(r['codigo']=='201' for r in f)} visibles={len(vis)} no_vistos={no_vistos}")
if vis:
    print(f"visible_ms (cota superior): min={vis[0]} mediana={vis[len(vis)//2]} max={vis[-1]} limite={limite}")
ok = len(vis) == len(f) and vis[-1] <= limite
print("RESULTADO:", "APROBADO (30 de 30 ≤ 5 s)" if ok and len(f) >= 30 else ("APROBADO con menos de 30 envíos" if ok else "NO APROBADO"))
PY
