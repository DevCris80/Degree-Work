#!/usr/bin/env python3
"""DM-10 (RNF-08): compara las filas de un celular con las del servidor y con las de otro celular.

Uso:
  dm10-comparar-sincronizacion.py <prefijo de id_lectura> <base del celular A> <servidor.csv> [<base del celular B>]

- Las bases son copias de `app-bovina.db` (ver `sacar_base` en comun.sh).
- `servidor.csv` sale de la consulta, en el editor SQL de Supabase o con psql, exportada a CSV:
    select id_registro, id_lectura, peso, fecha_medicion from public.registro
    where id_lectura like '<prefijo>%' order by id_lectura;
  Los pesajes que quedaron como pendientes viajan en `pesaje_pendiente` y se exportan igual,
  con `id_pesaje_pendiente as id_registro` y `fecha_medicion` en lugar de `timestamp`.

Cuenta las filas del prefijo en cada lugar y compara por id_lectura el peso de cada una.
Aprobado si coinciden en cantidad y en contenido y el porcentaje llegado es ≥ 95 % (RNF-08).
"""
import csv
import sqlite3
import sys


def del_celular(ruta, prefijo):
    con = sqlite3.connect(ruta)
    filas = {}
    for tabla in ("registro", "pesaje_pendiente"):
        for id_lectura, peso in con.execute(
            f"select idLectura, peso from {tabla} where idLectura like ?", (prefijo + "%",)
        ):
            filas[id_lectura] = round(float(peso), 3)
    return filas


def del_servidor(ruta):
    with open(ruta, newline="") as f:
        return {r["id_lectura"]: round(float(r["peso"]), 3) for r in csv.DictReader(f)}


def comparar(nombre, origen, destino):
    faltan = sorted(set(origen) - set(destino))
    sobran = sorted(set(destino) - set(origen))
    alteradas = sorted(k for k in origen if k in destino and origen[k] != destino[k])
    llegan = len(origen) - len(faltan)
    pct = 100.0 * (llegan - len(alteradas)) / len(origen) if origen else 0.0
    print(f"{nombre}: origen={len(origen)} destino={len(destino)} faltan={len(faltan)} "
          f"sobran={len(sobran)} alteradas={len(alteradas)} -> {pct:.1f} % sin pérdida ni alteración")
    for k in (faltan + alteradas)[:10]:
        print(f"   {k}")
    return pct


def main():
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    prefijo, base_a, servidor = sys.argv[1:4]
    a = del_celular(base_a, prefijo)
    resultados = [comparar("celular A -> servidor", a, del_servidor(servidor))]
    if len(sys.argv) > 4:
        resultados.append(comparar("celular A -> celular B", a, del_celular(sys.argv[4], prefijo)))
    if len(a) < 100:
        print(f"AVISO: el requisito pide al menos 100 filas y el prefijo tiene {len(a)}.")
    ok = all(p >= 95.0 for p in resultados) and len(a) >= 100
    print("RESULTADO:", "APROBADO" if ok else "NO APROBADO")
    sys.exit(0 if ok else 1)


main()
