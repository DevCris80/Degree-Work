#!/usr/bin/env bash
# DM-11 (RNF-02): cada pesaje válido queda guardado exactamente una vez, aunque el ESP32 reintente.
#
# Envía N pesajes con id_lectura distinto y repite cada uno D veces con el mismo id_lectura,
# como haría el ESP32 si no recibe la respuesta. Después saca la base del celular y compara.
#
# Uso: dm11-una-sola-vez.sh [N=50] [D=2] [CHIP=DM11SINASOCIAR]
#   Con un CHIP sin chapeta activa cada pesaje queda como pendiente (202); con el código de una
#   chapeta activa queda como registro del animal (201). Aprobado si:
#     1. cada id_lectura tiene el mismo código en el envío y en sus reintentos, y
#     2. la base guarda N filas, una por id_lectura, sin repetidos.
set -u
cd "$(dirname "$0")" && source ./comun.sh

N="${1:-50}"; D="${2:-2}"; CHIP="${3:-DM11SINASOCIAR}"
PREFIJO="dm11-$(date +%Y%m%d%H%M%S)"
SALIDA="resultados/dm11-$PREFIJO"; mkdir -p "$SALIDA"

echo "DM-11 | celular $(ip_del_celular):$PUERTO | N=$N reintentos=$D chip=$CHIP | prefijo=$PREFIJO" | tee "$SALIDA/resumen.txt"
estado_celular | tee -a "$SALIDA/resumen.txt"

distintos=0
for i in $(seq 1 "$N"); do
  id="$PREFIJO-$i"
  primero=$(envio "$id" "$CHIP" "$((400 + i % 100)).5" | cut -d' ' -f1)
  echo "$id envio $primero" >>"$SALIDA/envios.log"
  for r in $(seq 1 "$D"); do
    de_nuevo=$(envio "$id" "$CHIP" "$((400 + i % 100)).5" | cut -d' ' -f1)
    echo "$id reintento$r $de_nuevo" >>"$SALIDA/envios.log"
    [ "$de_nuevo" != "$primero" ] && { distintos=$((distintos + 1)); echo "DISTINTO $id: $primero vs $de_nuevo" | tee -a "$SALIDA/resumen.txt"; }
  done
done

sacar_base "$SALIDA/base"
cuenta=$(contar_lecturas "$SALIDA/base" "$PREFIJO")
echo "$cuenta" | tee -a "$SALIDA/resumen.txt"
echo "respuestas distintas entre envío y reintento: $distintos" | tee -a "$SALIDA/resumen.txt"

total=$(echo "$cuenta" | sed -E 's/.*total=([0-9]+)/\1/')
dist=$(echo "$cuenta" | sed -E 's/registros=[0-9]+ registros_distintos=([0-9]+) pendientes=[0-9]+ pendientes_distintos=([0-9]+).*/\1 \2/' | awk '{print $1+$2}')
if [ "$total" = "$N" ] && [ "$dist" = "$N" ] && [ "$distintos" = 0 ]; then
  echo "RESULTADO: APROBADO ($N de $N guardados una sola vez)" | tee -a "$SALIDA/resumen.txt"
else
  echo "RESULTADO: FALLÓ (guardados=$total distintos=$dist esperados=$N)" | tee -a "$SALIDA/resumen.txt"; exit 1
fi
