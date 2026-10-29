#!/usr/bin/env bash
# Graba la pantalla de un celular que no deja usar `screenrecord` por adb (por ejemplo, ColorOS de Oppo),
# tomando capturas seguidas (unas 1,4 por segundo) y armando un video a la velocidad real.
#
# Uso: grabar-por-capturas.sh <serial> <salida.mp4> <segundos máximos>
# Se detiene al cumplirse el tiempo o al crearse el archivo `<salida.mp4>.detener`.
set -u
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
SERIAL="${1:?serial}"; SALIDA="${2:?salida.mp4}"; MAX="${3:-120}"
TMP=$(mktemp -d); LISTA="$TMP/lista.txt"; : >"$LISTA"
inicio=$(date +%s%3N); anterior_f=""; anterior_t=$inicio; n=0
while [ $(( $(date +%s%3N) - inicio )) -lt $((MAX * 1000)) ] && [ ! -e "$SALIDA.detener" ]; do
  f="$TMP/$(printf %05d $n).png"
  "$ADB" -s "$SERIAL" exec-out screencap -p >"$f" 2>/dev/null || continue
  ahora=$(date +%s%3N)
  [ -n "$anterior_f" ] && printf "file '%s'\nduration %s\n" "$anterior_f" "$(awk "BEGIN{print ($ahora - $anterior_t)/1000}")" >>"$LISTA"
  anterior_f="$f"; anterior_t=$ahora; n=$((n + 1))
done
[ -n "$anterior_f" ] && printf "file '%s'\nduration 0.7\nfile '%s'\n" "$anterior_f" "$anterior_f" >>"$LISTA"
ffmpeg -loglevel error -y -f concat -safe 0 -i "$LISTA" -vf "scale=720:-2,fps=15" -c:v libopenh264 -b:v 250k -an -movflags +faststart "$SALIDA"
rm -rf "$TMP" "$SALIDA.detener"
echo "$SALIDA: $n capturas"
