#!/usr/bin/env python3
"""DM-06 (RF-36, RNF-08): compara las bases Room de dos celulares de la misma finca, tabla por tabla.

Uso: dm06-comparar-bases.py <base del celular A> <base del celular B>

Las bases son copias de `app-bovina.db` (ver `sacar_base` en comun.sh). Por cada tabla compara las
llaves primarias y, de las filas que están en las dos, el contenido de todas las columnas, salvo las
que son propias de cada celular: `sincronizado`, la ruta de la foto, `fotoPorSubir`, `revBase`, `idVersion` y `delDispositivo` (cada celular tiene su propio usuario).
Sale con código 0 si las dos bases son iguales.
"""
import sqlite3
import sys

PROPIAS_DEL_CELULAR = {"sincronizado", "fotoUri", "fotoPorSubir", "revBase", "idVersion", "delDispositivo"}
TABLAS = ["perfil_finca", "usuario", "animal", "chapeta", "evento", "registro", "pesaje_pendiente", "sibling"]


def filas(con, tabla):
    info = con.execute(f"pragma table_info({tabla})").fetchall()
    if not info:
        return None, None
    columnas = [c[1] for c in info]
    llaves = [c[1] for c in sorted((c for c in info if c[5]), key=lambda c: c[5])] or [columnas[0]]
    utiles = [c for c in columnas if c not in PROPIAS_DEL_CELULAR or c in llaves]
    cur = con.execute(f"select {','.join(utiles)} from {tabla}")
    por_llave = {}
    for fila in cur:
        reg = dict(zip(utiles, fila))
        por_llave[tuple(reg[k] for k in llaves)] = reg
    return por_llave, utiles


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    a, b = (sqlite3.connect(r) for r in sys.argv[1:3])
    iguales = True
    print(f"{'tabla':<18}{'A':>6}{'B':>6}  solo_A solo_B distintas")
    for t in TABLAS:
        fa, _ = filas(a, t)
        fb, _ = filas(b, t)
        if fa is None or fb is None:
            print(f"{t:<18}  (no existe en alguna base)")
            continue
        solo_a = sorted(set(fa) - set(fb))
        solo_b = sorted(set(fb) - set(fa))
        distintas = sorted(k for k in set(fa) & set(fb) if fa[k] != fb[k])
        print(f"{t:<18}{len(fa):>6}{len(fb):>6}  {len(solo_a):>6} {len(solo_b):>6} {len(distintas):>9}")
        for k in distintas[:3]:
            # Solo los nombres de las columnas: los valores pueden ser datos personales (por ejemplo, la ubicación de la finca).
            print(f"   {k}: difieren {[c for c in fa[k] if fa[k][c] != fb[k][c]]}")
        if solo_a or solo_b or distintas:
            iguales = False
    print("RESULTADO:", "LAS BASES SON IGUALES" if iguales else "HAY DIFERENCIAS")
    sys.exit(0 if iguales else 1)


main()
