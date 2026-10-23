---
status: accepted
---

# Sincronización con funciones de Postgres, sin un servicio FastAPI

La arquitectura planificada (modelo C4, contenedor "API de sincronización") ponía un servicio FastAPI entre la app y Supabase para verificar el JWT, comparar `rev_base`, guardar siblings, clasificar rechazos y entregar los cambios por cursor (ADR-0004). Decidimos quitarlo: todo eso lo hacen dos funciones de Postgres que la app llama por PostgREST con el JWT del usuario, `subir_cambios(lote)` y `descargar_cambios(cursor, limite)`, declaradas `SECURITY INVOKER` para que las políticas RLS por finca y rol apliquen sin más. Supabase ya es el backend: Auth emite y verifica el JWT, PostgREST expone las funciones (igual que `crear_finca` o `canjear_invitacion`) y Postgres hace la comparación de revisiones de forma atómica dentro de la transacción de la función (`SELECT … FOR UPDATE` sobre la fila).

## Considered Options

- **Servicio FastAPI que escribe con el JWT del usuario** (`SET LOCAL ROLE authenticated` y los claims): descartado. Duplica lo que Supabase ya hace, es un despliegue más con su propio lenguaje, y los planes gratuitos duermen el servicio y tardan decenas de segundos en despertar, justo lo peor para un celular con señal débil. Con el plazo del trabajo de grado, no paga su costo.
- **Escribir tabla por tabla con PostgREST, sin funciones**: descartado. La comparación de `rev_base` con la revisión vigente y la creación del sibling tienen que pasar en la misma transacción, o dos celulares pueden aceptarse los dos como `rev + 1`.

## Consequences

- La lógica de revisiones, siblings y rechazos vive en SQL (PL/pgSQL), en migraciones versionadas en el repositorio, y se prueba con SQL contra una base de Supabase.
- Una función procesa el lote fila por fila y devuelve un resultado por fila (aceptada, sibling o rechazada); un error de unicidad o de permisos se convierte en rechazo de esa fila sin abortar el lote.
- El modelo C4 (C2, el C3 de la API, DS-01, DS-02, DE-02 y DE-03) y la SRS dejan de nombrar FastAPI.
