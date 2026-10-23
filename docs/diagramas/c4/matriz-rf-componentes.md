# Matriz de trazabilidad RF – componentes C4

Corte: 23 de octubre de 2026. Fuente de los RF: SRS v0.2 (23-10-2026). Componentes: diagramas C3 `C3_componentes_app.puml` y `C3_componentes_bd.puml`.

KPI de OE2: 100 % de los RF asignados a al menos un componente del modelo C4.

## 1. Resultado del KPI

| Indicador | Valor |
|---|---|
| RF en la SRS | 43 |
| RF asignados a al menos un componente | 43 |
| Cobertura | **100 %** |
| RF que solo usan componentes existentes (implementados o parciales) | 25 |
| RF que dependen de al menos un componente planificado | 18 |

La cobertura mide que la arquitectura asigna cada RF a un componente. No mide implementación: un RF puede quedar asignado a un componente planificado. La implementación se mide con el KPI de OE3.

## 2. Matriz RF → componentes

| RF | Requisito (resumen) | Estado del RF (SRS) | Componentes | Contenedores |
|---|---|---|---|---|
| RF-01 | Crear cuenta con finca y primer Ganadero | Parcial | UI-01, DOM-01, DAT-03, DAT-04, BD-06 | App, Supabase |
| RF-02 | Iniciar sesión con Supabase Auth | Pendiente | UI-01, DAT-03 | App |
| RF-03 | Conservar la sesión de forma segura | Pendiente | DAT-03 | App |
| RF-04 | Autorización de datos personales (Ley 1581) | Pendiente | UI-01, DOM-01, DAT-04, BD-06 | App, Supabase |
| RF-05 | Generar y canjear código de invitación | Pendiente | UI-06, UI-01, DAT-04, BD-06 | App, Supabase |
| RF-06 | Permisos por rol en celular y servidor | Pendiente | DOM-01, UI-02, UI-04, BD-03, BD-05, BD-06 | App, Supabase |
| RF-07 | Recuperar contraseña | Pendiente | UI-01, DAT-03 | App |
| RF-08 | Registrar animal | Implementado | UI-02, DOM-02, DAT-01, DAT-06 | App |
| RF-09 | Listar animales activos | Implementado | UI-02, DAT-01 | App |
| RF-10 | Detalle del animal con historial | Implementado | UI-02, DAT-01 | App |
| RF-11 | Dar de baja un animal | Implementado | UI-02, DOM-02, DAT-01 | App |
| RF-12 | Editar animal | Pendiente | UI-02, DOM-02, DAT-01 | App |
| RF-13 | Registrar padre y madre | Implementado | UI-02, DOM-02, DAT-01 | App |
| RF-14 | Ubicación de la finca | Pendiente | UI-06, DAT-01 | App |
| RF-15 | Leer y normalizar el UID NFC | Implementado | UI-03, DOM-02 | App |
| RF-16 | Decidir el destino del código escaneado | Implementado | UI-03, DOM-02 | App |
| RF-17 | Asociar chapeta al registrar el animal | Implementado | UI-02, DOM-02, DAT-01 | App |
| RF-18 | Liberar chapeta | Implementado | UI-02, DOM-02, DAT-01 | App |
| RF-19 | Asociar chapeta a animal existente | Pendiente | UI-02, UI-03, DOM-02, DAT-01 | App |
| RF-20 | Recibir POST /pesaje en servicio en primer plano | Implementado | DAT-02 | App |
| RF-21 | Validar el pesaje y responder 400 | Implementado | DAT-02, DOM-03 | App |
| RF-22 | Idempotencia por id_lectura | Implementado | DOM-03, DAT-01 | App |
| RF-23 | Guardar como Registro (201) o pendiente (202) | Implementado | DOM-03, DAT-01 | App |
| RF-24 | Hora = recepción − antiguedad_ms | Implementado | DOM-03 | App |
| RF-25 | Iniciar y detener el servicio de pesaje | Implementado | UI-04, DAT-02 | App |
| RF-26 | Conciliar pesajes pendientes | Implementado | UI-04, DOM-03, DAT-01 | App |
| RF-27 | Pesaje manual | Pendiente | UI-04, DOM-03, DAT-01 | App |
| RF-28 | Registrar evento con fecha real | Parcial | UI-05, DAT-01 | App |
| RF-29 | Subtipos vacunación y parto | Pendiente | UI-05, DOM-04, DAT-01 | App |
| RF-30 | Editar eventos y pesajes como revisiones | Pendiente | UI-02, UI-05, DOM-04, DAT-01, BD-02 | App, Supabase |
| RF-31 | Baja lógica de eventos y pesajes | Pendiente | UI-02, DOM-04, DAT-01 | App |
| RF-32 | Subir cambios locales | Pendiente | DAT-05, BD-01, BD-05, BD-07 | App, Supabase |
| RF-33 | Detectar concurrencia con rev_base (siblings) | Pendiente | DAT-05, BD-02, BD-07 | App, Supabase |
| RF-34 | Mostrar y resolver conflictos | Pendiente | UI-07, DAT-05, BD-02 | App, Supabase |
| RF-35 | Bandeja de rechazos | Pendiente | UI-07, DAT-05, BD-03 | App, Supabase |
| RF-36 | Descargar cambios por cursor | Pendiente | DAT-05, BD-04, BD-05, BD-07 | App, Supabase |
| RF-37 | Quitar a un Operario | Pendiente | UI-06, DAT-04, BD-06 | App, Supabase |
| RF-38 | Subir y descargar fotos | Pendiente | DAT-06, DAT-05 | App |
| RF-39 | Inventario activo | Implementado | UI-08, DOM-05, DAT-01 | App |
| RF-40 | Evolución de peso y ganancia diaria | Pendiente | UI-08, DOM-05, DAT-01 | App |
| RF-41 | Pesajes pendientes y antigüedad | Implementado | UI-08, DOM-05, DAT-01 | App |
| RF-42 | Eventos por tipo y mes | Pendiente | UI-08, DOM-05, DAT-01 | App |
| RF-43 | Completitud del historial | Implementado | UI-08, DOM-05, DAT-01 | App |

## 3. Componentes → RF

| Componente | Nombre | Contenedor | Estado | RF |
|---|---|---|---|---|
| UI-01 | Cuenta y acceso | App | Parcial | RF-01, RF-02, RF-04, RF-05, RF-07 |
| UI-02 | Animales | App | Parcial | RF-06, RF-08, RF-09, RF-10, RF-11, RF-12, RF-13, RF-17, RF-18, RF-19, RF-30, RF-31 |
| UI-03 | Escanear | App | Implementado | RF-15, RF-16, RF-19 |
| UI-04 | Pesajes | App | Parcial | RF-06, RF-25, RF-26, RF-27 |
| UI-05 | Eventos | App | Parcial | RF-28, RF-29, RF-30 |
| UI-06 | Finca y usuarios | App | Planificado | RF-05, RF-14, RF-37 |
| UI-07 | Sincronización | App | Planificado | RF-34, RF-35 |
| UI-08 | Indicadores | App | Parcial | RF-39, RF-40, RF-41, RF-42, RF-43 |
| DOM-01 | Cuenta y permisos | App | Parcial | RF-01, RF-04, RF-06 |
| DOM-02 | Animales y chapetas | App | Parcial | RF-08, RF-11, RF-12, RF-13, RF-15, RF-16, RF-17, RF-18, RF-19 |
| DOM-03 | Pesajes | App | Parcial | RF-21, RF-22, RF-23, RF-24, RF-26, RF-27 |
| DOM-04 | Eventos y revisiones | App | Planificado | RF-29, RF-30, RF-31 |
| DOM-05 | Indicadores | App | Parcial | RF-39, RF-40, RF-41, RF-42, RF-43 |
| DAT-01 | Repositorios locales | App | Parcial | RF-08, RF-09, RF-10, RF-11, RF-12, RF-13, RF-14, RF-17, RF-18, RF-19, RF-22, RF-23, RF-26, RF-27, RF-28, RF-29, RF-30, RF-31, RF-39, RF-40, RF-41, RF-42, RF-43 |
| DAT-02 | Servidor de pesajes | App | Implementado | RF-20, RF-21, RF-25 |
| DAT-03 | Sesión y autenticación | App | Planificado | RF-01, RF-02, RF-03, RF-07 |
| DAT-04 | Funciones de finca | App | Planificado | RF-01, RF-04, RF-05, RF-37 |
| DAT-05 | Motor de sincronización | App | Planificado | RF-32, RF-33, RF-34, RF-35, RF-36, RF-38 |
| DAT-06 | Gestor de fotos | App | Parcial | RF-08, RF-38 |
| BD-01 | Función subir_cambios | Supabase | Planificado | RF-32 |
| BD-02 | Control de revisiones | Supabase | Planificado | RF-30, RF-33, RF-34 |
| BD-03 | Clasificador de resultados | Supabase | Planificado | RF-06, RF-35 |
| BD-04 | Función descargar_cambios | Supabase | Planificado | RF-36 |
| BD-05 | Políticas RLS | Supabase | Planificado | RF-06, RF-32, RF-36 |
| BD-06 | Funciones de finca | Supabase | Planificado | RF-01, RF-04, RF-05, RF-06, RF-37 |
| BD-07 | Tablas (datos, revisiones, siblings, cursor) | Supabase | Planificado | RF-32, RF-33, RF-36 |

## 4. Notas

- RF-06 tiene dos barreras: la verificación de rol en el dominio (DOM-01), que funciona sin señal, y las políticas RLS (BD-05), que aplican porque las funciones de sincronización corren como el usuario (`SECURITY INVOKER`). Las funciones de finca (BD-06) corren con `SECURITY DEFINER` y validan rol y finca dentro de la función.
- RF-01, RF-04, RF-05 y RF-37 se ejecutan en el servidor mediante las funciones de finca (BD-06), llamadas desde DAT-04, no mediante la sincronización.
- No hay un servicio de sincronización propio: PostgREST verifica el JWT y ejecuta las funciones de Postgres (ADR-0005).
- El estado de cada componente corresponde a la rama `main` del repositorio en la fecha de corte.
