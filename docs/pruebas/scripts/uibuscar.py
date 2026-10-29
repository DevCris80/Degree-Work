#!/usr/bin/env python3
"""Lee por la entrada estándar un volcado de `uiautomator dump` e imprime el centro «x y» del primer
elemento cuyo texto o descripción es exactamente el que se da. Sin coincidencia, no imprime nada.

Uso: adb exec-out '...uiautomator dump...' | uibuscar.py "Sincronizar ahora"
"""
import sys
import xml.etree.ElementTree as ET

buscado = sys.argv[1]
for nodo in ET.fromstring(sys.stdin.read()).iter("node"):
    if buscado in (nodo.get("text"), nodo.get("content-desc")):
        x1, y1, x2, y2 = (int(v) for v in nodo.get("bounds").replace("][", ",").strip("[]").split(","))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
