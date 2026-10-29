#!/usr/bin/env bash
# DM-10 (RNF-08) y DM-05: crea N filas en el celular enviándole N pesajes, como el ESP32, sin esperar respuesta lenta.
#
# Con la red local sin DNS cada petición tarda 4,3 s (DEF-02); por eso este script se puede usar con
# `adb forward` (IP=127.0.0.1 PUERTO=18080), que llega como conexión local y responde en 0,04 s.
#
# Uso: IP=127.0.0.1 PUERTO=18080 dm10-crear-filas.sh <N> <CHIP> [prefijo]
#   Con una chapeta activa cada pesaje queda como Registro (201); con un código sin chapeta, como Pesaje pendiente (202).
# Imprime el prefijo de los id_lectura, que dm10-comparar-sincronizacion.py necesita.
set -u
cd "$(dirname "$0")" && source ./comun.sh

N="${1:?cantidad de pesajes}"; CHIP="${2:?código de chapeta}"
PREFIJO="${3:-dm10-$(date +%Y%m%d%H%M%S)}"
SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"; : >"$SALIDA/envios.log"
for i in $(seq 1 "$N"); do
  read -r codigo ms < <(envio "$PREFIJO-$(printf %04d "$i")" "$CHIP" "$((300 + i % 700)).5")
  echo "$PREFIJO-$(printf %04d "$i") $codigo $ms" >>"$SALIDA/envios.log"
done
echo "prefijo=$PREFIJO enviados=$N 201=$(grep -c ' 201 ' "$SALIDA/envios.log") 202=$(grep -c ' 202 ' "$SALIDA/envios.log") otros=$(grep -vcE ' (201|202) ' "$SALIDA/envios.log")"
