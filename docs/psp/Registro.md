# Registro PSP — MiGanado

**Desarrolladores:** Cristian Villamil (C) y Mariana Alvarez (M).
**Periodo:** 6 de julio a 25 de octubre de 2026.

## Nota de método

Clasificación de defectos (PSP): 10 Documentación, 20 Sintaxis, 30 Construcción, 40 Asignación, 50 Interfaz, 60 Comprobación, 70 Datos, 80 Función, 90 Sistema, 100 Entorno.

---

## 1. Registro de tiempos (por semana)

| Semana | Fechas | Dev. | Fase | Horas | Actividades | Fuente |
|---|---|---|---|---|---|---|
| S28 | 06/07–12/07 | C | Planeación | 15 | Visita de campo a la finca piloto (08/07): observación y entrevista al ganadero; definición del alcance y del stack | Visita de campo 08/07 (capítulo OE1) |
| S28 | 06/07–12/07 | M | Planeación | 15 | Visita de campo (08/07) y registro de hallazgos sobre los registros manuales | Visita de campo 08/07 (capítulo OE1) |
| S29 | 13/07–19/07 | C | Planeación | 15 | Requisitos funcionales y no funcionales a partir de la visita | Plan de fases (Fases 1–2 hasta fin de julio) |
| S29 | 13/07–19/07 | M | Planeación | 15 | Requisitos funcionales y no funcionales a partir de la visita | Plan de fases |
| S30 | 20/07–26/07 | C | Planeación | 15 | Alcance, prioridades y cronograma | Plan de fases |
| S30 | 20/07–26/07 | M | Planeación | 15 | Roles TSP y plan de calidad | Plan de fases  |
| S31 | 27/07–02/08 | C | Planeación | 15 | Cierre de la definición de requisitos | Plan de fases |
| S31 | 27/07–02/08 | M | Planeación | 15 | Cierre de la definición de requisitos | Plan de fases |
| S32–S35 | 03/08–30/08 | C | Diseño | 60 | Arquitectura del prototipo (Room, NanoHTTPD en servicio en primer plano, ESP32 por HTTP) y especificación técnica inicial | Plan de fases (Fase 3 en agosto); `docs/spec_app_android_prototipo.md` |
| S32–S35 | 03/08–30/08 | M | Diseño | 60 | Diseño | Dedicación declarada |
| S36 | 31/08–06/09 | C | Diseño | 15 | Especificación técnica del prototipo (subida al repositorio el 10/09) | `docs/spec_app_android_prototipo.md` |
| S36 | 31/08–06/09 | M | Codigo | 15 | Entidades | Dedicación declarada |
| S37 | 07/09–13/09 | C | Código | 13 | Estructura de la app: Room, NanoHTTPD en servicio en primer plano, cinco pantallas y pruebas instrumentadas | Commits del 10/09 |
| S37 | 07/09–13/09 | C | Pruebas | 2 | Estructura de la app: Room, NanoHTTPD en servicio en primer plano, cinco pantallas y pruebas instrumentadas | Commits del 10/09 |
| S37 | 07/09–13/09 | M | Codigo | 15 | ESP32 | Dedicación declarada |
| S38 | 14/09–20/09 | C | Pruebas | 15 | Prueba extremo a extremo ESP32 → hotspot → NanoHTTPD → Room (rango de IP del hotspot, alias de curl en PowerShell) | Prueba registrada en el capítulo de construcción |
| S38 | 14/09–20/09 | M | Diseño | 15 | Si el diseño | Dedicación declarada |
| S39 | 21/09–27/09 | C | Código | 15 | Interfaces | Commit del 25/09 |
| S39 | 21/09–27/09 | M | Código | 15 | Primera tanda de pantallas Compose: Registro, Login, Lista, Registrar y Detalle | Commit del 26/09 |
| S40 | 28/09–04/10 | C | Codigo | 15 | Requerimientos | Dedicación declarada |
| S40 | 28/09–04/10 | M | Codigo | 15 | Sistemas | Dedicación declarada |
| S41 | 05/10–11/10 | C | Diseño | 3 | ADR-0001 a 0003 y contrato del pesaje; módulo :domain y repositorios; pesaje pendiente y conciliación | 31 commits del 07 al 09/10 |
| S41 | 05/10–11/10 | C | Código | 10,5 | ADR-0001 a 0003 y contrato del pesaje; módulo :domain y repositorios; pesaje pendiente y conciliación | 31 commits del 07 al 09/10 |
| S41 | 05/10–11/10 | C | Revisión de código | 1 | ADR-0001 a 0003 y contrato del pesaje; módulo :domain y repositorios; pesaje pendiente y conciliación | 31 commits del 07 al 09/10 |
| S41 | 05/10–11/10 | C | Pruebas | 0,5 | ADR-0001 a 0003 y contrato del pesaje; módulo :domain y repositorios; pesaje pendiente y conciliación | 31 commits del 07 al 09/10 |
| S41 | 05/10–11/10 | M | Código | 15 | Escanear con NFC, chapeta por código, liberar chapeta, registro de animal y chapeta en una transacción | 6 commits del 06 al 09/10 |
| S42 | 12/10–18/10 | C | Diseño | 5,5 | Migraciones de Room con esquema exportado, genealogía, guías de arquitectura (AGENTS.md) | 16 commits del 14/10 |
| S42 | 12/10–18/10 | C | Código | 8,5 | Migraciones de Room con esquema exportado, genealogía, guías de arquitectura (AGENTS.md) | 16 commits del 14/10 |
| S42 | 12/10–18/10 | C | Pruebas | 1 | Migraciones de Room con esquema exportado, genealogía, guías de arquitectura (AGENTS.md) | 16 commits del 14/10 |
| S42 | 12/10–18/10 | M | Diseño | 4 | Prototipo de interfaz con Google Stitch y sistema de diseño; ViewModels y rediseño de las pantallas | 17 commits del 13 al 17/10 |
| S42 | 12/10–18/10 | M | Código | 11 | Prototipo de interfaz con Google Stitch y sistema de diseño; ViewModels y rediseño de las pantallas | 17 commits del 13 al 17/10 |
| S43 | 19/10–25/10 | C | Diseño | 4 | Modelo C4, ADR-0004 y 0005, esquema central y RLS; sincronización, Supabase Auth, indicadores y pesaje manual; revisión de diseño | 68 commits del 23 y 24/10; diagramas del 22 al 25/10 |
| S43 | 19/10–25/10 | C | Revisión de diseño | 2 | Modelo C4, ADR-0004 y 0005, esquema central y RLS; sincronización, Supabase Auth, indicadores y pesaje manual; revisión de diseño | 68 commits del 23 y 24/10; diagramas del 22 al 25/10 |
| S43 | 19/10–25/10 | C | Código | 6 | Modelo C4, ADR-0004 y 0005, esquema central y RLS; sincronización, Supabase Auth, indicadores y pesaje manual; revisión de diseño | 68 commits del 23 y 24/10; diagramas del 22 al 25/10 |
| S43 | 19/10–25/10 | C | Revisión de código | 1 | Modelo C4, ADR-0004 y 0005, esquema central y RLS; sincronización, Supabase Auth, indicadores y pesaje manual; revisión de diseño | 68 commits del 23 y 24/10; diagramas del 22 al 25/10 |
| S43 | 19/10–25/10 | C | Pruebas | 2 | Modelo C4, ADR-0004 y 0005, esquema central y RLS; sincronización, Supabase Auth, indicadores y pesaje manual; revisión de diseño | 68 commits del 23 y 24/10; diagramas del 22 al 25/10 |
| S43 | 19/10–25/10 | M | Código | 15 | Programar evento, editar animal y resolver conflictos | 6 commits del 22 y 24/10 |

### Resumen por fase

| Fase | Horas | % del total |
|---|---|---|
| Planeación | 120 | 25,0 % |
| Diseño | 91,5 | 19,1 % |
| Revisión de diseño | 2 | 0,4 % |
| Código | 109 | 22,7 % |
| Revisión de código | 2 | 0,4 % |
| Pruebas | 20,5 | 4,3 % |
| Validacion | 135 | 28,1 % |
| **Total** | **480** | 100 % |

---

## 2. Registro de defectos

### 2.1 Defectos encontrados en la revisión de diseño (22 al 25/10/26)

| N.º  | Fecha    | Dev. | Tipo | Inyectado en | Removido en        | Tiempo solución (min) | Estado    | Descripción                                                                                                                                               | Corrección / evidencia                                                     |
| ---- | -------- | ---- | ---- | ------------ | ------------------ | --------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| D-01 | 22/10/26 | C    | 10   | Planeación   | Revisión de diseño | **10**                | Abierto   | La propuesta (sección de herramientas) declara Lucidchart y StarUML y compromete diagramas de despliegue y de clases que no se elaboraron.                | Actualizar la sección de herramientas a PlantUML con C4-PlantUML.          |
| D-02 | 22/10/26 | C    | 50   | Diseño       | Revisión de diseño | **5**                 | Corregido | "Supabase solo como autenticador" contradice la autorización con RLS y las fotos en Storage.                                                              | Supabase cubre Auth, Postgres y Storage (ADR-0005; C2).                    |
| D-03 | 22/10/26 | C    | 70   | Diseño       | Revisión de diseño | **25**                | Corregido | El diagrama de relaciones del repositorio no coincidía con el esquema Room implementado (genealogía y subtipos inexistentes; faltaba `pesaje_pendiente`). | Commit del 23/10, 12:19.                                                   |
| D-04 | 22/10/26 | C    | 80   | Planeación   | Revisión de diseño | **10**                | Corregido | Ambigüedad en el código de la chapeta: el ingreso manual podía ser el número impreso o el UID, dos espacios de identificadores en un mismo campo.         | Decisión: el ingreso manual es el UID (RN-02).                             |
| D-05 | 22/10/26 | C    | 80   | Diseño       | Revisión de diseño | **30**                | Abierto   | Crear la finca y canjear una invitación no tienen vía autorizada bajo RLS; una política abierta permitiría darse rol de Ganadero en una finca ajena.      | Diseñadas como funciones `SECURITY DEFINER` (BD-06); falta implementarlas. |
| D-06 | 22/10/26 | C    | 90   | Diseño       | Revisión de diseño | **20**                | Corregido | Un servicio que escribiera con la `service_role` anularía las políticas RLS.                                                                              | ADR-0005: funciones `SECURITY INVOKER` sin FastAPI.                        |
| D-07 | 22/10/26 | C    | 10   | Diseño       | Revisión de diseño | **15**                | Corregido | Diagrama de contexto: SINIGAN con flecha "No se integra", leyenda que mezcla tipos y estados, rótulos ambiguos de la báscula y de la chapeta.             | C1 actualizado el 22/10.                                                   |
| D-08 | 22/10/26 | C    | 50   | Planeación   | Revisión de diseño | **10**                | Abierto   | La postcondición de CU-01 crea la finca y el usuario en el celular y los sube después; bajo RLS el servidor los rechazaría.                               | Cambiar CU-01 para que use `crear_finca`.                                  |
| D-09 | 22/10/26 | C    | 10   | Planeación   | Revisión de diseño | **5**                 | Abierto   | La SRS (2.1) lista Supabase como sistema externo; el C4 lo ubica dentro de MiGanado.                                                                      | Ajustar la tabla de actores de la SRS.                                     |
| D-10 | 22/10/26 | C    | 10   | Planeación   | Revisión de diseño | **5**                 | Corregido | La interfaz de software de la SRS no incluía PostgREST.                                                                                                   | SRS v0.2 (23/10).                                                          |
| D-11 | 22/10/26 | C    | 80   | Diseño       | Revisión de diseño | **20**                | Corregido | Sin regla para descargar una fila con cambios locales sin subir: la edición local se perdía sin aviso.                                                    | ADR-0004, reglas 3 y 7.                                                    |
| D-12 | 22/10/26 | C    | 70   | Diseño       | Revisión de diseño | **15**                | Corregido | El esquema local no podía representar ediciones concurrentes (sin `rev`, siblings ni rechazos).                                                           | Room v15.                                                                  |
| D-13 | 23/10/26 | C    | 70   | Diseño       | Revisión de diseño | **10**                | Abierto   | Las claves foráneas de Room borran en cascada, en contra de la baja lógica (Ley 1659).                                                                    | Cambiar a `RESTRICT` en una migración.                                     |
| D-14 | 23/10/26 | C    | 70   | Diseño       | Revisión de diseño | **15**                | Corregido | Los valores de dominio usaban dos formatos (etiquetas con tilde y códigos en mayúsculas) sin regla para el servidor.                                      | El servidor adopta los textos de Room con `CHECK`.                         |
| D-15 | 23/10/26 | C    | 70   | Diseño       | Revisión de diseño | **20**                | Abierto   | `tipoEvento` es texto libre: el indicador de eventos por tipo agrupa variantes del mismo tipo.                                                            | Catálogo de tipos (RF-29).                                                 |
| D-16 | 23/10/26 | C    | 70   | Diseño       | Revisión de diseño | **25**                | Corregido | Las tablas hijas no tenían finca: las políticas RLS tendrían que unir con `animal`.                                                                       | Columna `id_perfil_finca` llenada por trigger.                             |
| D-17 | 23/10/26 | C    | 70   | Diseño       | Revisión de diseño | **10**                | Abierto   | No hay restricción de una sola chapeta activa por animal.                                                                                                 | Índice único parcial sobre `idAnimal` al implementar RF-19.                |
| D-18 | 24/10/26 | C    | 80   | Diseño       | Revisión de diseño | **10**                | Abierto   | El código digitado no se valida: un error crea una chapeta que ningún tag producirá.                                                                      | Advertencia si no son 14 caracteres hexadecimales.                         |
| D-19 | 24/10/26 | C    | 90   | Diseño       | Revisión de diseño | **5**                 | Abierto   | El UID de la NTAG215 se puede clonar y la app no verifica la firma de originalidad.                                                                       | Declarar como limitación.                                                  |
| D-20 | 25/10/26 | C    | 10   | Diseño       | Revisión de diseño | **10**                | Corregido | Los diagramas C4 marcaban como planificado lo que ya estaba implementado (Supabase, sincronización).                                                      | Estados al 24/10.                                                          |
| D-21 | 25/10/26 | C    | 70   | Diseño       | Revisión de diseño | **15**                | Corregido | El ER documentaba el esquema v11; el implementado es v15.                                                                                                 | `ER_local_room_v15.puml`.                                                  |
| D-22 | 25/10/26 | C    | 80   | Diseño       | Revisión de diseño | **5**                 | Abierto   | Una chapeta nueva rechazada por el servidor se borra físicamente; es una excepción a RNF-04 que la SRS no declara.                                        | Declararla en RNF-04.                                                      |
| D-23 | 25/10/26 | C    | 80   | Diseño       | Revisión de diseño | **20**                | Corregido | La máquina de estados NFC no incluía las transiciones causadas por la sincronización.                                                                     | NFC-02 actualizado.                                                        |

### 2.2 Defectos de construcción registrados en el historial de commits

| N.º | Fecha | Dev. | Tipo | Inyectado en | Removido en | Tiempo solución (min) | Descripción | Commit |
|---|---|---|---|---|---|---|---|---|
| C-01 | 08/10/26 | C | 50 | Código | Revisión de código | **20** | Correcciones al contrato del pesaje encontradas en la revisión. | `82527b7` |
| C-02 | 08/10/26 | C | 80 | Diseño | Revisión de código | **45** | La especificación inicial respondía 404 y no guardaba el pesaje de un código sin chapeta: se perdían pesajes. Pasa a 202 con Pesaje pendiente (ADR-0003). | `1b91b36` |
| C-03 | 08/10/26 | C | 70 | Código | Revisión de código | **15** | Dar de baja y liberar chapeta no volvían a marcar la fila como pendiente de sincronizar. | `cc650d1` |
| C-04 | 08/10/26 | C | 80 | Código | Revisión de código | **30** | El reintento de un pesaje ya conciliado no respondía lo mismo que la primera vez (idempotencia). | `432de81` |
| C-05 | 14/10/26 | C | 70 | Código | Revisión de código | **40** | La migración destructiva de Room borraba los datos al cambiar el esquema. | `11563eb` |
| C-06 | 14/10/26 | C | 40 | Código | Revisión de código | **10** | fechaModificacion tenía valor por defecto; debía asignarla solo el repositorio. | `e6e6cde` |
| C-07 | 23/10/26 | C | 100 | Código | Pruebas | **25** | Versión de kotlinx-serialization incompatible impedía correr MigrationTestHelper. | `74f7f0f` |
| C-08 | 23/10/26 | C | 50 | Código | Revisión de código | **15** | Indicadores: área de toque menor a 52 dp y valor mostrado junto al aviso de sin datos. | `0fed35b` |
| C-09 | 23/10/26 | C | 80 | Código | Revisión de código | **30** | Indicadores: observaciones pendientes de la revisión del PR #98. | `b76323c` |
| C-10 | 23/10/26 | C | 70 | Código | Revisión de código | **15** | SeedData creaba un animal de prueba en fincas reales. | `b401a83` |
| C-11 | 23/10/26 | C | 80 | Código | Revisión de código | **35** | Pesaje manual: validación RN-07 duplicada, sin comprobar animal activo y sin transacción. | `56b50b5` |
| C-12 | 23/10/26 | C | 80 | Código | Pruebas | **40** | La pantalla de inicio se recalculaba: iniciar sesión saltaba a la lista y volvía al Login. | `7dea430` |
| C-13 | 23/10/26 | C | 60 | Código | Pruebas | **60** | subir_cambios buscaba el id_version antes de bloquear la fila (condición de carrera) y rechazaba una segunda baja. | `357b159` |
| C-14 | 23/10/26 | C | 80 | Código | Pruebas | **50** | Un rechazo no se aplicaba si la fila cambiaba mientras subía; una cría ya subida salía junto con su madre rechazada. | `f052362` |
| C-15 | 24/10/26 | C | 70 | Código | Pruebas | **45** | Al descargar una chapeta activa del servidor, el código local chocaba con el índice único. | `5ea00fe` |
| C-16 | 24/10/26 | M | 80 | Código | Revisión de código | **55** | Un conflicto resuelto se cerraba aunque el servidor no aceptara la resolución, y un sibling ilegible cerraba la app. | `d0fa36d` |
| C-17 | 24/10/26 | C | 90 | Código | Pruebas | **45** | El trabajo de sincronización bloqueaba los disparos siguientes al fallar repetidamente. | `1351c55` |


---
