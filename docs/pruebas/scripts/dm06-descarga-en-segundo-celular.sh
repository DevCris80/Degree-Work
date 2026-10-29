#!/usr/bin/env bash
# DM-06 (RF-36, RNF-09): lo que un celular sube, el otro lo descarga.
#
# Precondición: dos celulares con la app de la misma finca (cada uno con su usuario), ya sincronizados,
# con la pantalla de Animales abierta y desbloqueados; el celular A con el servicio de pesaje activo.
# El script, sin tocar nada a mano:
#   1. envía al celular A 4 pesajes (3 a Canela, 1 a otro animal) y 1 que queda como pendiente;
#   2. sincroniza el celular A («Sincronizar ahora») y espera a que no queden cambios por subir;
#   3. sincroniza el celular B y espera a que vea el último peso de Canela y un pendiente más;
#   4. sincroniza el celular B otra vez, ahora sin cambios en el servidor;
#   5. copia las dos bases y las compara con dm06-comparar-bases.py.
# Los tiempos de cada paso quedan en la línea de tiempo. Graba las dos pantallas con
# `adb -s <serial> shell screenrecord` antes de empezar.
#
# Uso: dm06-descarga-en-segundo-celular.sh <serial A> <paquete A> <IP A> <serial B> <paquete B> [CHIP_CANELA=FP009] [CHIP_OTRO=FP001]
set -u
cd "$(dirname "$0")" && source ./comun.sh

SA="${1:?serial del celular A}"; PA="${2:?paquete del celular A}"; IPA="${3:?IP del celular A}"
SB="${4:?serial del celular B}"; PB="${5:?paquete del celular B}"
CHIP1="${6:-FP009}"; CHIP2="${7:-FP001}"
BASE=$((700 + RANDOM % 200))   # pesos distintos en cada corrida: BASE+1.5, +2.5 y +3.5 para el animal 1
PREFIJO="dm06-$(date +%Y%m%d%H%M%S)"; SALIDA="resultados/$PREFIJO"; mkdir -p "$SALIDA"; LOG="$SALIDA/linea-de-tiempo.log"
PESO3="$((BASE + 3)).5"
T0=$(ahora_ms)
t() { echo "[+$(( ($(ahora_ms) - T0) / 1000 )) s] $*" | tee -a "$LOG"; }

# texto_en_pantalla <serial> <texto exacto o fragmento>
texto_en_pantalla() { "$ADB" -s "$1" exec-out 'uiautomator dump /sdcard/dm06.xml >/dev/null 2>&1; cat /sdcard/dm06.xml' | grep -qF "$2"; }
# esperar <serial> <fragmento> <máx s>: devuelve 0 si lo ve; imprime los segundos que tardó
esperar() { local t1; t1=$(ahora_ms); while [ $(( $(ahora_ms) - t1 )) -lt $(($3 * 1000)) ]; do
    texto_en_pantalla "$1" "$2" && { echo $(( ($(ahora_ms) - t1) / 1000 )); return 0; }; done; echo "no"; return 1; }
# tocar <serial> <texto o descripción exacta>
tocar() { local x; x=$("$ADB" -s "$1" exec-out 'uiautomator dump /sdcard/dm06.xml >/dev/null 2>&1; cat /sdcard/dm06.xml' | python3 -I ./uibuscar.py "$2")
  [ -n "$x" ] && "$ADB" -s "$1" shell input tap $x; }
enviar() { curl -s -o /dev/null --max-time 5 -w '%{http_code}' -X POST "http://$IPA:$PUERTO/pesaje" -H 'Content-Type: application/json' \
    -d "{\"id_lectura\":\"$PREFIJO-$1\",\"id_chip\":\"$2\",\"peso\":$3}"; }

t "inicio; A=$SA, B=$SB"
t "paso 1: pesajes al celular A: $(enviar 1 "$CHIP1" "$((BASE + 1)).5") $(enviar 2 "$CHIP1" "$((BASE + 2)).5") $(enviar 3 "$CHIP1" "$PESO3") $(enviar 4 "$CHIP2" "$((BASE + 4)).5") y uno sin chapeta $(enviar 5 DM06SINCHAPETA 305.0)"
PENDIENTES_B=$(PAQUETE="$PB" ANDROID_SERIAL="$SB" sacar_base "$SALIDA/base-B-antes"; python3 -I -c "import sqlite3,sys; print(sqlite3.connect(sys.argv[1]).execute('select count(*) from pesaje_pendiente').fetchone()[0])" "$SALIDA/base-B-antes/app-bovina.db")
ESPERADOS=$((PENDIENTES_B + 1))
sleep 2
t "paso 2: A toca «Sincronizar ahora»"; tocar "$SA" "Sincronizar ahora"
t "   A sin cambios por subir tras $(esperar "$SA" "Todos los datos sincronizados" 90) s (A pasó antes por «cambios guardados, por subir»)"
t "paso 3: B toca «Sincronizar ahora»"; tocar "$SB" "Sincronizar ahora"
t "   B ve '$ESPERADOS pesajes pendientes' tras $(esperar "$SB" "$ESPERADOS pesajes pendientes" 90) s"
t "   B ve el último peso de Canela, $PESO3 kg, tras $(esperar "$SB" "Último peso: $PESO3 kg" 30) s"
sleep 3
t "paso 4: B toca «Sincronizar ahora» otra vez, sin cambios en el servidor"; tocar "$SB" "Sincronizar ahora"; sleep 8
t "   B sigue con '$ESPERADOS pesajes pendientes' y $PESO3 kg: $(texto_en_pantalla "$SB" "$ESPERADOS pesajes pendientes" && echo sí || echo NO) / $(texto_en_pantalla "$SB" "Último peso: $PESO3 kg" && echo sí || echo NO)"

t "paso 5: copiar y comparar las bases"
PAQUETE="$PA" ANDROID_SERIAL="$SA" sacar_base "$SALIDA/base-A"
PAQUETE="$PB" ANDROID_SERIAL="$SB" sacar_base "$SALIDA/base-B"
python3 -I ./dm06-comparar-bases.py "$SALIDA/base-A/app-bovina.db" "$SALIDA/base-B/app-bovina.db" | tee "$SALIDA/comparacion.txt" | tee -a "$LOG"
