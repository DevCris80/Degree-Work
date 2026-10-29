# Especificación de Requisitos de Software (SRS)

## MiGanado — Sistema de trazabilidad bovina offline-first

| Campo | Valor |
|---|---|
| Proyecto | Trabajo de grado ATG202601008 — Universidad Católica de Colombia |
| Autores | Cristian Steven Villamil Rodríguez, Mariana Alvarez |
| Director | Juan Guillermo Hurtado |
| Repositorio | `github.com/DevCris80/Degree-Work` |
| Versión de este documento | 0.5 (borrador) |
| Fecha de corte del estado | 28 de octubre de 2026 |


<!-- índice:inicio -->
## Índice

**Secciones**

- [1. Introducción](#1-introducción)
  - [1.1 Objetivo](#11-objetivo)
  - [1.2 Destinatarios](#12-destinatarios)
  - [1.3 Uso previsto](#13-uso-previsto)
  - [1.4 Alcance del producto](#14-alcance-del-producto)
  - [1.5 Definiciones y siglas](#15-definiciones-y-siglas)
  - [1.6 Convenciones de este documento](#16-convenciones-de-este-documento)
- [2. Descripción general](#2-descripción-general)
  - [2.1 Actores y matriz de permisos](#21-actores-y-matriz-de-permisos)
  - [2.2 Necesidades de los usuarios](#22-necesidades-de-los-usuarios)
  - [2.3 Supuestos y dependencias](#23-supuestos-y-dependencias)
  - [2.4 Restricciones y limitaciones declaradas](#24-restricciones-y-limitaciones-declaradas)
  - [2.5 Reglas de negocio](#25-reglas-de-negocio)
- [3. Requisitos](#3-requisitos)
  - [3.1 Requisitos funcionales](#31-requisitos-funcionales)
    - [3.1.1 Catálogo](#311-catálogo)
    - [3.1.2 Requisito transversal](#312-requisito-transversal)
    - [3.1.3 Fichas de casos de uso](#313-fichas-de-casos-de-uso)
  - [3.2 Requisitos de interfaz externa](#32-requisitos-de-interfaz-externa)
    - [3.2.1 Interfaz de usuario](#321-interfaz-de-usuario)
    - [3.2.2 Interfaz de hardware](#322-interfaz-de-hardware)
    - [3.2.3 Interfaz de software](#323-interfaz-de-software)
    - [3.2.4 Interfaz de comunicaciones](#324-interfaz-de-comunicaciones)
  - [3.3 Requisitos no funcionales](#33-requisitos-no-funcionales)
    - [3.3.1 Rendimiento y eficiencia](#331-rendimiento-y-eficiencia)
    - [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión)
    - [3.3.3 Seguridad y privacidad](#333-seguridad-y-privacidad)
    - [3.3.4 Usabilidad](#334-usabilidad)
    - [3.3.5 Compatibilidad](#335-compatibilidad)
    - [3.3.6 Mantenibilidad y verificabilidad](#336-mantenibilidad-y-verificabilidad)
- [4. Matriz de trazabilidad](#4-matriz-de-trazabilidad)
  - [4.1 Requisitos funcionales](#41-requisitos-funcionales)
  - [4.2 Requisitos no funcionales](#42-requisitos-no-funcionales)

**Casos de uso** (fichas en la sección [3.1.3](#313-fichas-de-casos-de-uso))

| Caso de uso | Nombre |
|---|---|
| [CU-01](#cu-01) | Crear cuenta |
| [CU-02](#cu-02) | Iniciar sesión |
| [CU-03](#cu-03) | Recuperar contraseña |
| [CU-04](#cu-04) | Invitar y unir a un Operario |
| [CU-05](#cu-05) | Quitar a un Operario |
| [CU-06](#cu-06) | Registrar animal |
| [CU-07](#cu-07) | Consultar animales |
| [CU-08](#cu-08) | Editar animal |
| [CU-09](#cu-09) | Dar de baja animal |
| [CU-10](#cu-10) | Escanear chapeta |
| [CU-11](#cu-11) | Asociar chapeta a un animal existente |
| [CU-12](#cu-12) | Liberar chapeta |
| [CU-13](#cu-13) | Recibir pesaje del ESP32 |
| [CU-14](#cu-14) | Controlar el servicio de pesaje |
| [CU-15](#cu-15) | Conciliar pesajes pendientes |
| [CU-16](#cu-16) | Registrar pesaje manual |
| [CU-17](#cu-17) | Registrar evento |
| [CU-18](#cu-18) | Editar evento o pesaje |
| [CU-19](#cu-19) | Dar de baja evento o pesaje |
| [CU-20](#cu-20) | Sincronizar |
| [CU-21](#cu-21) | Resolver conflicto |
| [CU-22](#cu-22) | Corregir rechazos |
| [CU-23](#cu-23) | Registrar ubicación de la finca |
| [CU-24](#cu-24) | Consultar indicadores |

**Requisitos no funcionales** (sección [3.3](#33-requisitos-no-funcionales))

| Requisito | Nombre | Categoría |
|---|---|---|
| [RNF-01](#rnf-01) | Operación sin señal | [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión) |
| [RNF-02](#rnf-02) | Ningún pesaje válido se pierde ni se duplica | [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión) |
| [RNF-03](#rnf-03) | Servicio activo con la pantalla apagada | [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión) |
| [RNF-04](#rnf-04) | Sin borrado físico e integridad del esquema | [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión) |
| [RNF-05](#rnf-05) | Tiempo de registro de un pesaje | [3.3.1 Rendimiento y eficiencia](#331-rendimiento-y-eficiencia) |
| [RNF-06](#rnf-06) | Plataforma y dispositivos | [3.3.5 Compatibilidad](#335-compatibilidad) |
| [RNF-07](#rnf-07) | Pruebas del prototipo | [3.3.6 Mantenibilidad y verificabilidad](#336-mantenibilidad-y-verificabilidad) |
| [RNF-08](#rnf-08) | Integridad de la sincronización | [3.3.2 Fiabilidad y disponibilidad sin conexión](#332-fiabilidad-y-disponibilidad-sin-conexión) |
| [RNF-09](#rnf-09) | Descarga incremental | [3.3.1 Rendimiento y eficiencia](#331-rendimiento-y-eficiencia) |
| [RNF-10](#rnf-10) | Tamaño de las fotos | [3.3.1 Rendimiento y eficiencia](#331-rendimiento-y-eficiencia) |
| [RNF-11](#rnf-11) | Autenticación y sesión | [3.3.3 Seguridad y privacidad](#333-seguridad-y-privacidad) |
| [RNF-12](#rnf-12) | Autorización por finca y rol | [3.3.3 Seguridad y privacidad](#333-seguridad-y-privacidad) |
| [RNF-13](#rnf-13) | Tratamiento de datos personales | [3.3.3 Seguridad y privacidad](#333-seguridad-y-privacidad) |
| [RNF-14](#rnf-14) | Auditoría | [3.3.3 Seguridad y privacidad](#333-seguridad-y-privacidad) |
| [RNF-15](#rnf-15) | Eficiencia frente al registro manual | [3.3.4 Usabilidad](#334-usabilidad) |
| [RNF-16](#rnf-16) | Uso en campo | [3.3.4 Usabilidad](#334-usabilidad) |

**Otros identificadores**

| Identificador | Dónde está |
|---|---|
| RF-01 a RF-43, requisitos funcionales | Catálogo [3.1.1](#311-catálogo); estado y verificación en la matriz [4.1](#41-requisitos-funcionales) |
| N-xx, necesidades, y E-xx, escenarios | [2.2](#22-necesidades-de-los-usuarios) |
| SUP-xx, supuestos y dependencias | [2.3](#23-supuestos-y-dependencias) |
| RES-xx y LIM-xx, restricciones y limitaciones | [2.4](#24-restricciones-y-limitaciones-declaradas) |
| RN-xx, reglas de negocio | [2.5](#25-reglas-de-negocio) |
| Matriz de permisos por rol | [2.1](#21-actores-y-matriz-de-permisos) |
| Requisitos no funcionales, estado y verificación | [4.2](#42-requisitos-no-funcionales) |
<!-- índice:fin -->

---

## 1. Introducción

### 1.1 Objetivo

Especificar los requisitos funcionales y no funcionales de MiGanado, el prototipo de sistema de trazabilidad bovina offline-first del trabajo de grado, con su fuente, prioridad, estado y forma de verificación, de modo que cada requisito pueda rastrearse hasta los objetivos del proyecto y hasta su evidencia de implementación.

### 1.2 Destinatarios

- Jurado y director del trabajo de grado: para verificar la cobertura de los objetivos y KPI.
- Equipo de desarrollo: como referencia para implementar, probar y priorizar.
- Equipo del proyecto IoT complementario (ESP32): para el contrato de la interfaz de pesaje.

### 1.3 Uso previsto

- Fuente de verdad de *qué* debe hacer el sistema. El *cómo* está en la arquitectura (modelo C4, Fase 3) y en los ADR (`docs/adr/`).
- Base para los KPI de OE1 (requisitos levantados), OE2 (trazabilidad a módulos) y OE3 (porcentaje de requisitos implementados y verificables).
- Este documento es un anexo de la tesis. La tesis cita la versión del tag `srs-v1.0`, no la rama `main`.

### 1.4 Alcance del producto

**Incluido**

- App Android nativa, offline-first, para fincas pequeñas y medianas (20–50 animales).
- Registro, consulta, edición y baja lógica de animales, con genealogía y foto.
- Identificación individual con chapetas NFC (lectura del UID) o código digitado.
- Recepción de pesajes desde un ESP32 por HTTP local, pesaje manual y conciliación de pesajes no asignados.
- Registro y edición de eventos sanitarios y de manejo, con subtipos.
- Cuentas con Supabase Auth, dos roles (Ganadero y Operario) e invitación por código.
- Sincronización entre dispositivos vía Supabase (funciones de Postgres), con revisiones, conflictos y rechazos.
- Tablero de indicadores (BI) dentro de la app, calculado sobre los datos locales.

**Excluido**

| Fuera de alcance | Motivo |
|---|---|
| Versión iOS | Decisión de alcance del equipo |
| Panel web de administración | Decisión de alcance del equipo |
| Integración con SINIGAN | Decisión de alcance del equipo; ver también LIM-01 |
| Potreros y lotes | Decisión de alcance del equipo |
| Desarrollo del firmware del ESP32 y de la báscula | Lo desarrolla el proyecto IoT complementario |
| Escritura de tags NFC | Solo se lee el UID |
| Lectura de chapetas LF 134,2 kHz (ISO 11784/11785) | Ver LIM-01 |
| Reportes exportables (PDF, Excel) | Decisión de alcance del equipo |

### 1.5 Definiciones y siglas

**Términos del dominio** (fuente: `CONTEXT.md`)

| Término | Definición |
|---|---|
| Animal | Bovino individual registrado, identificado por `idAnimal` (UUID). |
| Chapeta | Identificador físico colocado en un Animal, identificado por su `codigo` (UID del tag NFC normalizado). Está *activa* mientras no tiene fecha de desasociación. Un mismo código puede tener varias Chapetas en el tiempo, pero solo una activa a la vez. |
| Liberar | Desasociar una Chapeta de su Animal. Nunca ocurre en silencio. |
| Pesaje | Medición (código y peso) que reporta el ESP32 cuando un bovino pasa por la báscula. |
| Registro | Pesaje ya asignado a un Animal activo. El sustantivo se reserva para pesajes. |
| Pesaje pendiente | Pesaje válido que no pudo asignarse a un Animal, porque su código no tiene Chapeta activa o porque el Animal está dado de baja. |
| Conciliar | Resolver un Pesaje pendiente: *asignarlo* a un Animal (crea el Registro) o *descartarlo*. Es definitivo. |
| Evento | Suceso clínico o de manejo de un Animal (vacunación, tratamiento, parto, inseminación), con fecha y detalle. |
| Dar de baja | Marcar un Animal, Evento o Registro con `fechaBaja`. Deja de aparecer en las listas, pero se conserva (baja lógica). |
| Usuario | Persona que opera la app. Pertenece a una sola finca y tiene un rol. |
| Ganadero | Rol del Usuario responsable de la finca. Puede hacer todo. |
| Operario | Rol del Usuario que trabaja con el ganado, con permisos restringidos (ver 2.1). |
| PerfilFinca | La finca a la que pertenecen los Usuarios y los Animales. |
| Crear cuenta | Acto por el que una persona se convierte en Usuario. |

**Términos de sincronización**

| Término | Definición |
|---|---|
| `sincronizado` | Booleano de cada fila local: `false` significa que tiene cambios sin subir. |
| Revisión (`rev`) | Número que asigna el servidor a cada versión aceptada de una fila editable (Animal, Evento, Registro). |
| `rev_base` | Revisión de la que partió una edición hecha en el celular. |
| `id_version` | Identificador (UUID) de cada escritura local de una fila editable. Permite reconocer una edición reenviada. |
| Sibling | Versión concurrente de una fila: una edición cuyo `rev_base` es menor que la revisión vigente se guarda como sibling, no se descarta ni se fusiona. Cuenta la fila entera, aunque los campos editados no se pisen. |
| Conflicto | Estado de una fila que tiene uno o más siblings sin resolver. |
| Cursor de descarga | Valor de una secuencia global del servidor que aumenta con cualquier escritura sincronizada. El celular guarda el último que recibió y pide lo posterior, desde un margen de solapamiento antes, porque dos escrituras pueden confirmar en orden distinto al de sus cursores. |
| Rechazo | Cambio local que el servidor no aceptó por violar una regla de unicidad o de permisos. Queda en la bandeja de rechazos hasta que alguien lo corrige; la fila local vuelve a la versión del servidor, o sale de su tabla si el servidor no la tiene (RN-24). |

**Siglas**

| Sigla | Significado |
|---|---|
| BI | Inteligencia de negocios (*business intelligence*) |
| CU | Caso de uso |
| ESP32 | Microcontrolador del proyecto IoT complementario que reporta los pesajes |
| GDP | Ganancia diaria de peso |
| LIM | Limitación declarada |
| NFC | *Near Field Communication* (13,56 MHz) |
| RES | Restricción |
| RF / RNF | Requisito funcional / no funcional |
| RLS | *Row Level Security* (políticas por fila de Postgres/Supabase) |
| RN | Regla de negocio |
| SUP | Supuesto o dependencia |
| UID | Identificador único de fábrica de un tag NFC |

### 1.6 Convenciones de este documento

| Campo | Valores |
|---|---|
| Prioridad (MoSCoW) | **M** Must · **S** Should · **C** Could |
| Estado al 28-10-2026 | **Implementado** (hay código en `main`) · **Parcial** (existe, pero no cumple todo el enunciado) · **Pendiente** (decidido, sin código) |
| Fuente | **CAMPO**: confirmado por el ganadero en la visita de campo (ver 2.2) · **EQUIPO**: decisión del equipo (con su ADR o issue cuando existe) · **NORMA**: exigido por una norma |
| Objetivo | **OG** objetivo general · **OE1–OE4** objetivos específicos de la propuesta |
| Verificación | **P** prueba automatizada · **D** demostración manual · **I** inspección · **A** análisis |

Un requisito *implementado* cuenta para el KPI de OE3 solo si también es *verificable*: tiene una prueba automatizada o un registro de demostración.

---

## 2. Descripción general

### 2.1 Actores y matriz de permisos

| Actor | Tipo | Descripción |
|---|---|---|
| Ganadero | Persona | Responsable de la finca. Crea la cuenta y la finca, invita y quita Operarios. |
| Operario | Persona | Trabaja con el ganado. Se une a una finca con un código de invitación. |
| ESP32 | Sistema externo | Envía cada pesaje por HTTP al celular que comparte el hotspot. |
| Supabase Auth | Sistema externo | Identidad: crear cuenta, iniciar sesión, recuperar contraseña. |
| Supabase (PostgREST, Postgres y Storage) | Sistema externo | Base de datos central con RLS; funciones de sincronización que reciben y entregan cambios, asignan revisiones y detectan conflictos; almacenamiento de fotos. |

**Matriz de permisos** (✓ permitido · ✗ no permitido)

| Acción | Ganadero | Operario | RF |
|---|---|---|---|
| Ver animales, detalle e indicadores | ✓ | ✓ | RF-09, RF-10, RF-39–43 |
| Escanear chapeta | ✓ | ✓ | RF-15, RF-16 |
| Registrar animal | ✓ | ✓ | RF-08 |
| Editar animal | ✓ | ✓ | RF-12 |
| Dar de baja animal | ✓ | ✗ | RF-11 |
| Asociar chapeta a un animal sin chapeta activa | ✓ | ✓ | RF-19 |
| Liberar chapeta | ✓ | ✗ | RF-18 |
| Registrar evento o pesaje manual | ✓ | ✓ | RF-27, RF-28 |
| Editar evento o pesaje | ✓ | ✓ | RF-30 |
| Dar de baja evento o pesaje | ✓ | ✗ | RF-31 |
| Asignar pesaje pendiente | ✓ | ✓ | RF-26 |
| Descartar pesaje pendiente | ✓ | ✗ | RF-26 |
| Iniciar y detener el servicio de pesaje | ✓ | ✓ | RF-25 |
| Registrar ubicación de la finca | ✓ | ✗ | RF-14 |
| Invitar y quitar Operarios | ✓ | ✗ | RF-05, RF-37 |
| Resolver conflictos | ✓ | ✓ | RF-34 |
| Corregir rechazos | ✓ | ✓ (según la acción que corrige) | RF-35 |

### 2.2 Necesidades de los usuarios

| ID | Necesidad | Fuente | Requisitos |
|---|---|---|---|
| N-01 | Identificar a cada animal de forma individual y confiable | CAMPO | RF-15 a RF-19 |
| N-02 | Llevar la ficha de cada animal (datos, foto, genealogía) | CAMPO | RF-08, RF-10, RF-12, RF-13 |
| N-03 | Conservar el historial de pesajes de cada animal | CAMPO | RF-10, RF-20 a RF-27 |
| N-04 | Registrar la ubicación de la finca | CAMPO | RF-14 |
| N-05 | Diferenciar lo que puede hacer el dueño y lo que puede hacer un trabajador | CAMPO | RF-05, RF-06, RF-37 |
| N-06 | Trabajar en la finca sin señal | CAMPO | RNF-01 |
| N-07 | Registrar eventos sanitarios con su fecha real | EQUIPO / NORMA (Ley 1659) | RF-28 a RF-31 |
| N-08 | Que varias personas trabajen sobre los mismos datos desde distintos celulares | EQUIPO | RF-32 a RF-36, RF-38 |
| N-09 | No perder un pesaje aunque el animal no esté identificado todavía | EQUIPO (ADR-0003) | RF-23, RF-26 |
| N-10 | Ver el estado del hato de un vistazo | EQUIPO | RF-39 a RF-43 |
| N-11 | Proteger los datos personales del usuario | NORMA (Ley 1581) | RF-04, RNF-13 |

**Escenarios operativos**

| ID | Contexto | Actor | Secuencia | Cubierto por |
|---|---|---|---|---|
| E-01 | Jornada de pesaje en la manga, sin señal, celular con hotspot encendido | Operario, ESP32 | Cada animal pasa por la báscula; el ESP32 lee la chapeta y envía el pesaje; el celular lo guarda como Registro y responde 201. Si se cae la conexión, el ESP32 reintenta con el mismo `id_lectura`. | RF-20 a RF-25, RNF-01 a RNF-03, RNF-05 |
| E-02 | Un animal sin chapeta registrada en la app pasa por la báscula | ESP32, Ganadero | El pesaje queda pendiente (202, `CHIP_NO_ASOCIADO`). Más tarde el Ganadero registra el animal con ese código y la app le ofrece asignarle los pendientes. | RF-08, RF-23, RF-26 |
| E-03 | Una vacuna se aplica en el potrero y se registra en la app al día siguiente | Operario | El Operario registra el evento con la fecha real de aplicación, no la del día en que lo digita. | RF-28, RF-29 |
| E-04 | Dos usuarios editan el mismo animal sin señal | Ganadero, Operario | El primero en sincronizar fija la revisión vigente; la edición del segundo queda como sibling y el animal aparece "en conflicto" hasta que alguien lo resuelve. | RF-12, RF-32 a RF-34 |
| E-05 | Un animal pierde su chapeta y se le pone una nueva | Ganadero | Se escanea la chapeta nueva y se asocia al animal existente, conservando el historial de la anterior. | RF-18, RF-19 |
| E-06 | Un trabajador nuevo se une a la finca | Ganadero, Operario | El Ganadero genera un código de invitación; el Operario, con señal, crea su cuenta y canjea el código. | RF-05 |
| E-07 | Dos celulares sin señal asocian el mismo código a animales distintos | Ganadero, Operario | El segundo en sincronizar es rechazado por unicidad; la fila aparece en la bandeja de rechazos para que el usuario la corrija. | RF-35, RN-01 |

### 2.3 Supuestos y dependencias

| ID | Supuesto o dependencia | Si no se cumple |
|---|---|---|
| SUP-01 | El ESP32 y la báscula los provee el proyecto IoT complementario y respetan `docs/contrato-pesaje.md`. | Los requisitos RF-20 a RF-24 solo pueden demostrarse con un cliente HTTP simulado (curl). |
| SUP-02 | El ESP32 se conecta al hotspot del celular que recibe los pesajes. | No llegan pesajes. |
| SUP-03 | Supabase (Auth, PostgREST, Postgres, Storage) está disponible cuando el celular tiene señal. | Las operaciones de LIM-02 esperan; el resto de la app sigue funcionando. |
| SUP-04 | Cada Usuario usa su propio celular; no se comparte dispositivo. | La autoría de los registros deja de ser confiable. |
| SUP-05 | ⚠ Los dispositivos de verificación son un Samsung Galaxy A15 (referencia) y un Xiaomi 14T. La disponibilidad de NFC en el A15 depende de la variante y está pendiente de verificar. | Si el A15 no tiene NFC, RF-15 se verifica solo en el 14T y se declara. |
| SUP-06 | Los tags del prototipo son NTAG215 de 13,56 MHz. | — |

### 2.4 Restricciones y limitaciones declaradas

**Restricciones** (impuestas desde fuera del diseño)

| ID | Restricción | Origen | Requisitos afectados |
|---|---|---|---|
| RES-01 | App Android nativa: Kotlin, Jetpack Compose, Room; mínimo Android 7.0 (API 24). | EQUIPO (ADR-0001) | RNF-06 |
| RES-02 | Conectividad rural intermitente o ausente en la finca. | CAMPO | RNF-01 |
| RES-03 | Entrega del documento el 29-10-2026 y sustentación el 27-11-2026. | Calendario académico | Todos |
| RES-04 | Identificadores UUID en todas las entidades, para evitar colisiones entre Room y Supabase. | EQUIPO | RF-32, RF-36 |
| RES-05 | No se permite borrar registros de trazabilidad; solo baja lógica. | NORMA (Ley 1659 de 2013) | RF-11, RF-18, RF-31, RNF-04 |
| RES-06 | Autorización previa para el tratamiento de datos personales. | NORMA (Ley 1581 de 2012) | RF-04, RNF-13 |
| RES-07 | Un Usuario pertenece a una sola finca. | EQUIPO (ADR-0002) | RF-01, RF-05 |
| RES-08 | El equipo no desarrolla el ESP32; solo el servicio de la app que lo recibe. | Acuerdo con el proyecto IoT | RF-20 a RF-25 |

**Limitaciones** (concesiones del diseño)

| ID | Limitación | Norma afectada | Impacto | Requisitos afectados | Trabajo futuro |
|---|---|---|---|---|---|
| LIM-01 | La identificación usa tags NFC de 13,56 MHz leídos con el NFC del celular. Las chapetas oficiales usan RFID LF 134,2 kHz. | ISO 11784/11785; SINIGAN | El sistema no lee chapetas oficiales ni puede integrarse con SINIGAN tal como está. | RF-15 a RF-19 | Lector LF externo conectado al celular. |
| LIM-02 | Requieren señal: crear cuenta, primer inicio de sesión en un dispositivo, recuperar contraseña, canjear un código de invitación, sincronizar y subir fotos. | — | Excepción deliberada a offline-first (ADR-0002). | RF-01 a RF-03, RF-05, RF-07, RF-32 a RF-38 | — |
| LIM-03 | Los pesos son simulados por el ESP32; no hay celdas de carga en este prototipo. | — | No se valida la precisión de la medición, solo su recepción y tratamiento. | RF-20 a RF-24 | Integración con el proyecto IoT. |
| LIM-04 | El endpoint `POST /pesaje` no tiene autenticación y usa HTTP sin cifrar. | — | Cualquier equipo conectado a la red del hotspot puede enviar pesajes. | RF-20 | Token compartido con el ESP32. |
| LIM-05 | Los indicadores se calculan sobre la copia local (Room). | — | Reflejan la finca solo hasta la última sincronización; el tablero muestra esa fecha. | RF-39 a RF-43 | — |
| LIM-06 | Un Operario quitado conserva sus datos locales y puede seguir capturando sin señal hasta su próxima sincronización, que será rechazada. | — | Datos capturados después de la remoción no llegan al servidor. | RF-37 | Borrado remoto de la copia local. |
| LIM-07 | Solo el celular que comparte el hotspot recibe pesajes, y los guarda a nombre de su Usuario. | — | Los pesajes de una jornada quedan a nombre de una persona aunque varias trabajen. | RF-20, RF-23 | — |

### 2.5 Reglas de negocio

| ID | Regla | Fuente |
|---|---|---|
| RN-01 | Un código tiene como máximo una Chapeta activa, tanto en el celular como en el servidor (índice único parcial). | EQUIPO |
| RN-02 | El código se normaliza: mayúsculas, sin espacios, `:` ni `-`. No se exige que sea hexadecimal. | EQUIPO |
| RN-03 | La etapa corresponde al sexo: Toro y Novillo son machos; Vaca y Ternera son hembras. | EQUIPO |
| RN-04 | Nada se borra físicamente. Animal, Evento y Registro se dan de baja con `fechaBaja`; la Chapeta se libera con `fechaDesasociacion`. | NORMA (Ley 1659) |
| RN-05 | Todo pesaje válido se conserva: como Registro o como Pesaje pendiente. | EQUIPO (ADR-0003) |
| RN-06 | Un pesaje nunca crea animales ni chapetas. | EQUIPO (ADR-0003) |
| RN-07 | Pesaje válido: 0 < peso ≤ 1500 kg; 0 ≤ `antiguedad_ms` ≤ hora de recepción. | EQUIPO |
| RN-08 | Hora de medición = hora de recepción − `antiguedad_ms`. Nunca se usa el reloj del ESP32. | EQUIPO (ADR-0003) |
| RN-09 | Recibir dos veces el mismo `id_lectura` no crea nada nuevo y responde lo mismo que la primera vez. | EQUIPO (ADR-0003) |
| RN-10 | Un pesaje para un Animal dado de baja queda pendiente con motivo `ANIMAL_DADO_DE_BAJA`. | EQUIPO (ADR-0003) |
| RN-11 | Conciliar es definitivo. Un pendiente descartado se conserva como descartado. | EQUIPO |
| RN-12 | Solo un Animal activo puede recibir pesajes asignados. | EQUIPO |
| RN-13 | Los permisos por rol de la matriz 2.1 se verifican en el celular (funciona sin señal) y en el servidor con RLS (barrera real). | EQUIPO (ADR-0002) |
| RN-14 | Toda escritura local deja `sincronizado = false` y actualiza `fechaModificacion`. | EQUIPO (ADR-0001) |
| RN-15 | Revisiones (solo Animal, Evento, Registro): el servidor asigna `rev`. Si el `rev_base` de una edición es igual a la revisión vigente, se acepta y se asigna `rev + 1`; si es menor, se guarda como sibling. | EQUIPO |
| RN-16 | Las revisiones y los siblings se conservan siempre; no hay depuración. | EQUIPO / NORMA (Ley 1659) |
| RN-17 | Mientras una fila esté en conflicto, se muestra la revisión aceptada primero, marcada "en conflicto", sin bloquear la consulta. Cualquier rol puede resolverlo. Solo una resolución explícita cierra el conflicto: editar la fila sin resolverlo se acepta como revisión nueva y el conflicto sigue abierto. | EQUIPO (ADR-0004) |
| RN-18 | Un código de invitación es de un solo uso y vence a los 7 días. | EQUIPO |
| RN-19 | Evento, Registro, Pesaje pendiente y su conciliación registran el Usuario que los creó o resolvió. | EQUIPO |
| RN-20 | El primer Usuario de una finca es Ganadero. | EQUIPO |
| RN-21 | Un Pesaje pendiente tiene como máximo un Registro asociado, en el celular y en el servidor (índice único sobre `id_lectura`). | EQUIPO |
| RN-22 | Una baja (`fechaBaja`, o `fechaDesasociacion` de una Chapeta) nunca queda como sibling: el servidor la aplica sobre la revisión vigente aunque su `rev_base` sea menor. Si la misma edición cambió otros campos, solo esos pueden quedar como sibling. | EQUIPO (ADR-0004) |
| RN-23 | Reenviar una edición con un `id_version` ya procesado no crea revisión ni sibling, y recibe la misma respuesta que la primera vez. | EQUIPO (ADR-0004) |
| RN-24 | Un cambio rechazado se conserva completo, con su motivo, en la bandeja de rechazos, y no se reenvía mientras siga abierto. Si el servidor tiene la fila, la fila local vuelve a la versión del servidor; si no la tiene, sale de su tabla local junto con las filas que dependen de ella (las de un Animal), que también quedan en la bandeja. | EQUIPO (ADR-0004) |

---

## 3. Requisitos

### 3.1 Requisitos funcionales

#### 3.1.1 Catálogo

| ID | El sistema debe… | Pri. | Estado | Fuente | Obj. | CU |
|---|---|---|---|---|---|---|
| **C1. Cuenta y acceso** | | | | | | |
| RF-01 | Permitir crear una cuenta con nombre, nombre de la finca, correo y contraseña, creando la identidad en Supabase Auth y la finca con su primer Usuario (Ganadero). | M | Implementado | EQUIPO (ADR-0002) | OE3 | CU-01 |
| RF-02 | Permitir iniciar sesión con correo y contraseña contra Supabase Auth. | M | Implementado | EQUIPO (ADR-0002) | OE3 | CU-02 |
| RF-03 | Conservar la sesión de forma segura en el dispositivo, para operar sin señal después del primer inicio de sesión. | M | Implementado | EQUIPO (ADR-0002) | OE3 | CU-02 |
| RF-04 | Solicitar y registrar la autorización de tratamiento de datos personales antes de crear la cuenta. | M | Implementado | NORMA (Ley 1581) | OE3 | CU-01 |
| RF-05 | Permitir al Ganadero generar un código de invitación y a un Operario canjearlo para unirse a la finca. | M | Pendiente | CAMPO | OE3 | CU-04 |
| RF-06 | Restringir las acciones según la matriz de permisos (2.1), en el celular y en el servidor. | M | Implementado | CAMPO | OE3 | Transversal |
| RF-07 | Permitir recuperar la contraseña por correo. | C | Implementado | EQUIPO | OE3 | CU-03 |
| RF-37 | Permitir al Ganadero quitar a un Operario de la finca. | C | Pendiente | EQUIPO | OE3 | CU-05 |
| **C2. Animales** | | | | | | |
| RF-08 | Registrar un animal con nombre, raza, sexo, etapa coherente con el sexo, fecha de nacimiento exacta o estimada, propósito, foto opcional y código de chapeta opcional. | M | Implementado | CAMPO | OE3 | CU-06 |
| RF-09 | Listar los animales activos con su chapeta activa y su último pesaje, filtrables por propósito, con el número de pesajes pendientes. | M | Implementado | EQUIPO | OE3 | CU-07 |
| RF-10 | Mostrar el detalle de un animal con sus datos, chapeta activa e historial de pesajes y eventos, del más reciente al más antiguo. | M | Implementado | CAMPO | OE3 | CU-07 |
| RF-11 | Dar de baja un animal (baja lógica), con confirmación previa. | M | Implementado | NORMA (Ley 1659) | OE3 | CU-09 |
| RF-12 | Editar los datos de un animal. | S | Implementado | CAMPO | OE3 | CU-08 |
| RF-13 | Registrar el padre y la madre de un animal entre los animales de la finca. | S | Implementado | CAMPO | OE3 | CU-06, CU-08 |
| RF-14 | Registrar la ubicación (latitud y longitud) de la finca. | S | Implementado | CAMPO | OE3 | CU-23 |
| **C3. Identificación** | | | | | | |
| RF-15 | Leer el UID de un tag NFC-A con el NFC del celular y normalizarlo. | M | Implementado | CAMPO | OE3 | CU-10 |
| RF-16 | Decidir qué hacer con un código escaneado según el estado de su chapeta y de su animal. | M | Implementado | CAMPO | OE3 | CU-10 |
| RF-17 | Asociar una chapeta al registrar el animal, por escaneo o por texto, rechazando un código que ya tiene otra chapeta activa. | M | Implementado | CAMPO | OE3 | CU-06 |
| RF-18 | Liberar la chapeta activa de un animal, con confirmación, conservando su historial. | M | Implementado | CAMPO | OE3 | CU-12 |
| RF-19 | Asociar una chapeta nueva a un animal existente que no tiene chapeta activa. | M | Pendiente | CAMPO | OE3 | CU-11 |
| **C4. Pesajes** | | | | | | |
| RF-20 | Recibir pesajes por `POST /pesaje` (puerto 8080) desde un servicio en primer plano. | M | Implementado | EQUIPO | OE3 | CU-13 |
| RF-21 | Validar campos, tipos y rangos del pesaje y responder 400 con el campo y el motivo. | M | Implementado | EQUIPO | OE3 | CU-13 |
| RF-22 | Tratar de forma idempotente los reintentos con el mismo `id_lectura`. | M | Implementado | EQUIPO (ADR-0003) | OE3 | CU-13 |
| RF-23 | Guardar cada pesaje válido como Registro del animal (201) o como Pesaje pendiente con su motivo (202). | M | Implementado | EQUIPO (ADR-0003) | OE3 | CU-13 |
| RF-24 | Calcular la hora de medición como hora de recepción menos `antiguedad_ms`. | M | Implementado | EQUIPO (ADR-0003) | OE3 | CU-13 |
| RF-25 | Permitir iniciar y detener el servicio de pesaje y ver si está activo. | M | Implementado | EQUIPO | OE3 | CU-14 |
| RF-26 | Permitir conciliar los pesajes pendientes: asignarlos a un animal activo o descartarlos. | M | Implementado | EQUIPO (ADR-0003) | OE3 | CU-15 |
| RF-27 | Permitir registrar un pesaje manual a un animal activo. | S | Implementado | CAMPO | OE3 | CU-16 |
| **C5. Eventos** | | | | | | |
| RF-28 | Registrar un evento de tipo vacunación o parto, con la fecha y hora del evento (pasada o futura) y un detalle. Un evento con fecha futura queda programado hasta que esa fecha llega. | M | Implementado | EQUIPO / NORMA (Ley 1659) | OE3 | CU-17 |
| RF-29 | Registrar los campos de cada tipo: vacunación (biológico, dosis y vía de aplicación) y parto (cría resultante, que queda con el animal del evento como madre o padre). | C | Implementado | EQUIPO | OE3 | CU-17 |
| RF-30 | Permitir editar eventos y pesajes, guardando cada versión como revisión. | S | Pendiente | EQUIPO | OE3 | CU-18 |
| RF-31 | Dar de baja eventos y pesajes (baja lógica). | C | Pendiente | NORMA (Ley 1659) | OE3 | CU-19 |
| **C6. Sincronización** | | | | | | |
| RF-32 | Subir los cambios locales al servicio de sincronización cuando hay señal. | M | Parcial | EQUIPO | OE2, OE3 | CU-20 |
| RF-33 | Detectar ediciones concurrentes con `rev_base` y guardarlas como siblings. | S | Implementado | EQUIPO | OE2, OE3 | CU-20 |
| RF-34 | Mostrar las filas en conflicto y permitir resolverlas. | S | Implementado | EQUIPO | OE2, OE3 | CU-21 |
| RF-35 | Mostrar las filas rechazadas por el servidor, con su motivo, y permitir corregirlas. | M | Parcial | EQUIPO | OE2, OE3 | CU-22 |
| RF-36 | Descargar los cambios de otros dispositivos desde el último cursor recibido. | M | Implementado | EQUIPO | OE2, OE3 | CU-20 |
| RF-38 | Subir las fotos a Supabase Storage y descargar las de otros dispositivos. | S | Implementado | EQUIPO | OE3 | CU-20 |
| **C7. Indicadores (BI)** | | | | | | |
| RF-39 | Mostrar el inventario activo por propósito, sexo y etapa. | S | Implementado | EQUIPO | OE3 | CU-24 |
| RF-40 | Mostrar la evolución de peso por animal y la ganancia diaria de peso promedio. | S | Implementado | EQUIPO | OE3 | CU-24 |
| RF-41 | Mostrar los pesajes pendientes sin conciliar y su antigüedad. | S | Implementado | EQUIPO | OE3 | CU-24 |
| RF-42 | Mostrar los eventos por tipo y por mes. | C | Implementado | EQUIPO | OE3 | CU-24 |
| RF-43 | Mostrar la completitud del historial: animales sin chapeta activa o sin pesaje en los últimos N días. | S | Implementado | EQUIPO | OG, OE3 | CU-24 |

#### 3.1.2 Requisito transversal

**RF-06 — Aplicar permisos por rol**

| Campo | Contenido |
|---|---|
| Fuente | CAMPO |
| Prioridad / Estado | M / Implementado. En el celular, `DarDeBajaAnimal`, `LiberarChapeta` y `DescartarPesajePendiente` rechazan al Operario y sus pantallas no le muestran el control; en el servidor, las políticas RLS. Invitar y quitar Operarios llegan con RF-05 y RF-37. |
| Enunciado | Toda acción de la matriz 2.1 marcada ✗ para un rol se impide en dos niveles: en el celular (el control no se muestra y el caso de uso la rechaza, aun sin señal) y en el servidor (políticas RLS por finca y rol). |
| Reglas | RN-13 |
| Criterios de aceptación | 1. **Dado** un Operario, **cuando** abre el detalle de un animal, **entonces** no ve "Dar de baja" ni "Liberar chapeta".<br>2. **Dado** un Operario, **cuando** abre un pesaje pendiente, **entonces** puede asignarlo pero no descartarlo.<br>3. **Dado** un Operario que envía una escritura prohibida al servidor, **cuando** se sincroniza, **entonces** el servidor la rechaza y la fila aparece en la bandeja de rechazos (RF-35).<br>4. **Dado** un usuario de otra finca, **cuando** consulta el servidor, **entonces** no recibe filas de esta finca. |
| Verificación | P (casos de uso con rol) y D (prueba de RLS con dos cuentas) |

#### 3.1.3 Fichas de casos de uso

##### C1. Cuenta y acceso

<a id="cu-01"></a>**CU-01 — Crear cuenta**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-01, RF-04 |
| Objetivo / Fuente | OE3 / EQUIPO (ADR-0002), NORMA (Ley 1581) |
| Actor | Ganadero |
| Prioridad / Estado | M / Implementado. La app registra la identidad en Supabase Auth con la finca y la versión de la autorización como metadatos, y un trigger del servidor crea la finca, el primer Usuario y la aceptación. El texto de autorización es provisional (`0.1-provisional`). |
| Precondiciones | La app no tiene cuenta en este dispositivo. Hay señal. |
| Disparador | El usuario abre la app por primera vez. |
| Flujo principal | 1. La app muestra Crear cuenta.<br>2. El usuario ingresa nombre, nombre de la finca, correo, contraseña y su confirmación.<br>3. La app muestra el texto de autorización de la Ley 1581 y el usuario lo acepta.<br>4. El sistema crea la identidad en Supabase Auth.<br>5. El sistema crea la finca y el Usuario con rol Ganadero, en una sola transacción, y registra la fecha de aceptación.<br>6. La app abre la lista de animales con el nombre de la finca. |
| Flujos alternos y excepciones | 2a. Contraseña de menos de 6 caracteres, o confirmación distinta: el botón Crear cuenta queda deshabilitado.<br>2b. El usuario tiene un código de invitación: continúa en CU-04.<br>3a. El usuario no acepta: no se crea nada.<br>4a. Sin señal: la app lo indica y no crea nada, ni local ni remoto.<br>4b. El correo ya existe: la app lo indica y ofrece iniciar sesión. |
| Offline / sincronización | Requiere señal (LIM-02). |
| Postcondiciones | Identidad en Supabase Auth; PerfilFinca, Usuario y aceptación (con fecha) creados en el servidor; PerfilFinca y Usuario copiados al celular. |
| Entidades | PerfilFinca, Usuario |
| Reglas | RN-20, RES-06, RES-07 |
| Criterios de aceptación | 1. **Dado** un dispositivo sin cuenta y con señal, **cuando** el usuario completa datos válidos y acepta la autorización, **entonces** queda en la lista de animales con el nombre de su finca.<br>2. **Dado** que no acepta la autorización, **entonces** no se crea la cuenta.<br>3. **Dado** que no hay señal, **entonces** la app lo indica y no queda ninguna fila creada.<br>4. **Dado** un correo ya registrado, **entonces** la app lo indica y no crea una segunda finca. |
| Trazas | Issues #40, #87 · `CrearCuentaScreen` · `CrearCuentaViewModelTest`, `CrearCuentaTest`, `RoomCuentaRepositoryTest`, `06_crear_finca` |

<a id="cu-02"></a>**CU-02 — Iniciar sesión**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-02, RF-03 |
| Objetivo / Fuente | OE3 / EQUIPO (ADR-0002) |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Implementado. Valida contra Supabase Auth y guarda la sesión cifrada con una clave AES del Android Keystore. |
| Precondiciones | El usuario tiene cuenta. Para el primer inicio de sesión en un dispositivo, hay señal. |
| Disparador | El usuario abre la app en un dispositivo sin sesión, o su sesión fue cerrada. |
| Flujo principal | 1. El usuario ingresa correo y contraseña.<br>2. Supabase Auth valida las credenciales.<br>3. El sistema guarda la sesión en el almacenamiento seguro del dispositivo (no en Room).<br>4. El sistema descarga la finca, el Usuario y los datos de la finca (RF-36).<br>5. La app abre la lista de animales. |
| Flujos alternos y excepciones | 2a. Credenciales inválidas: la app lo indica.<br>2b. Sin señal en el primer inicio de sesión: la app lo indica.<br>A. Con sesión guardada, la app abre la lista directamente, aun sin señal.<br>B. La sesión vence sin señal: la app sigue operando; al recuperar señal renueva la sesión.<br>C. El servidor rechaza la renovación (usuario quitado): la app cierra la sesión (ver LIM-06). |
| Offline / sincronización | El primer inicio de sesión requiere señal (LIM-02); los siguientes no. |
| Postcondiciones | Sesión guardada de forma segura; datos de la finca disponibles en Room. |
| Entidades | Usuario, PerfilFinca |
| Reglas | RN-13 |
| Criterios de aceptación | 1. **Dado** un usuario con cuenta y con señal, **cuando** ingresa credenciales válidas, **entonces** ve los animales de su finca.<br>2. **Dado** credenciales inválidas, **entonces** no entra.<br>3. **Dado** un usuario que ya inició sesión, **cuando** abre la app en modo avión, **entonces** entra y puede registrar. |
| Trazas | Issues #23, #40 · `LoginScreen`, `SesionCifrada` · `LoginViewModelTest`, `InicioViewModelTest` |

<a id="cu-03"></a>**CU-03 — Recuperar contraseña**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-07 |
| Objetivo / Fuente | OE3 / EQUIPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | C / Implementado. El diálogo pide el correo, Supabase Auth envía el mensaje y la app confirma el envío. |
| Precondiciones | Hay señal. |
| Flujo principal | 1. En Iniciar sesión, el usuario toca "¿Olvidaste tu contraseña?" e ingresa su correo.<br>2. Supabase Auth envía el correo de recuperación.<br>3. La app confirma el envío. |
| Flujos alternos y excepciones | 1a. Sin señal: la app lo indica. |
| Offline / sincronización | Requiere señal (LIM-02). |
| Criterios de aceptación | 1. **Dado** un correo registrado y con señal, **cuando** solicita la recuperación, **entonces** recibe el correo.<br>2. **Dado** un correo no registrado, **entonces** la app muestra el mismo mensaje de confirmación, sin revelar si la cuenta existe. |
| Trazas | Issue #91 · `LoginScreen` · `LoginViewModelTest` |

<a id="cu-04"></a>**CU-04 — Invitar y unir a un Operario**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-05 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero (invita), Operario (se une) |
| Prioridad / Estado | M / Pendiente |
| Precondiciones | El Ganadero tiene sesión y señal. El Operario tiene señal. |
| Disparador | El Ganadero quiere sumar un trabajador. |
| Flujo principal | 1. El Ganadero solicita un código de invitación.<br>2. El servidor genera el código y la app lo muestra con su fecha de vencimiento.<br>3. El Ganadero comparte el código por fuera de la app.<br>4. El Operario, en Crear cuenta, elige "Tengo un código" e ingresa su nombre, correo, contraseña y el código, y acepta la autorización de datos.<br>5. El servidor valida el código, crea la identidad y el Usuario con rol Operario asociado a esa finca, y marca el código como usado.<br>6. La app del Operario descarga los datos de la finca y abre la lista de animales. |
| Flujos alternos y excepciones | 5a. Código vencido, usado o inexistente: la app lo indica y no crea la cuenta.<br>5b. Sin señal: la app lo indica. |
| Offline / sincronización | Requiere señal (LIM-02). |
| Postcondiciones | Usuario Operario asociado a la finca; código marcado como usado. |
| Entidades | Usuario, código de invitación (servidor) |
| Reglas | RN-18, RES-07 |
| Criterios de aceptación | 1. **Dado** un código vigente, **cuando** el Operario lo canjea, **entonces** ve los animales de la finca con permisos de Operario.<br>2. **Dado** un código ya usado o con más de 7 días, **entonces** se rechaza.<br>3. **Dado** un Operario, **entonces** no puede generar códigos. |
| Trazas | — |

<a id="cu-05"></a>**CU-05 — Quitar a un Operario**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-37 |
| Objetivo / Fuente | OE3 / EQUIPO (`CONTEXT.md`) |
| Actor | Ganadero |
| Prioridad / Estado | C / Pendiente |
| Precondiciones | Hay señal. La finca tiene al menos un Operario. |
| Flujo principal | 1. El Ganadero abre la lista de usuarios de la finca y elige un Operario.<br>2. Confirma que lo quita.<br>3. El servidor revoca su acceso a la finca. Sus registros anteriores se conservan con su autoría. |
| Flujos alternos y excepciones | 3a. El celular del Operario está sin señal: sigue operando hasta su próxima sincronización, que es rechazada (LIM-06). |
| Postcondiciones | El Operario ya no puede leer ni escribir datos de la finca en el servidor. |
| Reglas | RN-04, RN-19 |
| Criterios de aceptación | 1. **Dado** un Operario quitado, **cuando** su celular sincroniza, **entonces** el servidor rechaza sus escrituras y su sesión se cierra.<br>2. **Dado** que se quitó a un Operario, **entonces** los eventos y pesajes que registró siguen a su nombre. |
| Trazas | — |

##### C2. Animales

<a id="cu-06"></a>**CU-06 — Registrar animal**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-08, RF-13, RF-17 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Implementado. |
| Precondiciones | Sesión iniciada. |
| Disparador | "Nuevo animal" en la lista; un código escaneado sin chapeta activa (CU-10); "Registrar animal con este código" desde un pesaje pendiente (CU-15). |
| Flujo principal | 1. La app muestra el formulario.<br>2. El usuario ingresa nombre y raza, elige sexo y luego etapa (solo las del sexo elegido), fecha de nacimiento (marcando si es estimada) y propósito.<br>3. Opcionalmente toma o elige una foto, elige padre y madre, e ingresa el código de chapeta.<br>4. El usuario guarda.<br>5. El sistema guarda el animal y, si hay código, su chapeta activa, en una sola transacción.<br>6. La app vuelve a la pantalla anterior. |
| Flujos alternos y excepciones | 1a. Llegó desde un escaneo: el código viene precargado y es de solo lectura. Si el código tuvo chapeta antes, la app avisa a qué animal estuvo asociado.<br>4a. Faltan campos obligatorios: el botón Guardar queda deshabilitado.<br>5a. El código ya tiene otra chapeta activa: la app lo indica y no guarda nada.<br>5b. El código tiene pesajes pendientes sin resolver: la app ofrece asignárselos al animal recién registrado; el usuario acepta o rechaza. |
| Offline / sincronización | Funciona sin señal. Las filas quedan con `sincronizado = false`. |
| Postcondiciones | Animal activo; chapeta activa si hubo código; pendientes asignados si el usuario aceptó. |
| Entidades | Animal, Chapeta, Pesaje pendiente, Registro |
| Reglas | RN-01, RN-02, RN-03, RN-14. Para RF-13, el padre es macho y la madre hembra; pueden estar dados de baja, porque un progenitor puede haber salido de la finca antes de registrar la cría. |
| Criterios de aceptación | 1. **Dado** el formulario completo, **cuando** guarda, **entonces** el animal aparece en la lista.<br>2. **Dado** sexo Hembra, **entonces** solo puede elegir Vaca o Ternera.<br>3. **Dado** un código con chapeta activa en otro animal, **entonces** no se guarda ni el animal ni la chapeta.<br>4. **Dado** un código con dos pesajes pendientes, **cuando** acepta asignarlos, **entonces** el detalle del animal muestra esos dos pesajes con su hora de medición original. |
| Trazas | Issues #20, #31, #38 · `AnimalFormScreen` · `RegistrarAnimalViewModelTest`, `RoomAnimalRepositoryTest` |

<a id="cu-07"></a>**CU-07 — Consultar animales**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-09, RF-10 |
| Objetivo / Fuente | OE3 / EQUIPO (RF-09), CAMPO (RF-10) |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Implementado |
| Precondiciones | Sesión iniciada. |
| Flujo principal | 1. La lista muestra los animales activos con su chapeta activa, su último pesaje y el número de pesajes pendientes.<br>2. El usuario filtra por propósito (opcional).<br>3. El usuario abre un animal.<br>4. El detalle muestra sus datos, su chapeta activa y dos pestañas, pesajes y eventos, del más reciente al más antiguo. |
| Flujos alternos y excepciones | 1a. No hay animales: la lista lo indica. |
| Offline / sincronización | Funciona sin señal; lee solo de Room. |
| Reglas | RN-04 (los dados de baja no aparecen) |
| Criterios de aceptación | 1. **Dado** un animal dado de baja, **entonces** no aparece en la lista.<br>2. **Dado** el filtro Leche, **entonces** solo aparecen animales con propósito Leche.<br>3. **Dado** un animal con tres pesajes, **entonces** el detalle los muestra del más reciente al más antiguo. |
| Trazas | Issue #37 · `AnimalListScreen`, `AnimalDetailScreen` · `ListaAnimalesViewModelTest`, `DetalleAnimalViewModelTest` |

<a id="cu-08"></a>**CU-08 — Editar animal**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-12, RF-13 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | S / Implementado |
| Precondiciones | El animal está activo. |
| Flujo principal | 1. En el detalle, el usuario elige Editar.<br>2. Modifica los datos (mismas validaciones que CU-06, incluida la genealogía).<br>3. Guarda.<br>4. El sistema guarda la nueva versión con `rev_base` igual a la revisión que tenía el celular. |
| Flujos alternos y excepciones | 3a. No hubo cambios: no se crea versión. |
| Offline / sincronización | Funciona sin señal. Al sincronizar puede generar un conflicto (CU-20, CU-21). |
| Postcondiciones | Nueva versión local con `sincronizado = false`. |
| Entidades | Animal, revisiones |
| Reglas | RN-03, RN-14, RN-15, RN-16 |
| Criterios de aceptación | 1. **Dado** un animal, **cuando** se cambia su raza y se guarda, **entonces** el detalle muestra la raza nueva.<br>2. **Dado** un cambio de sexo, **entonces** la etapa debe volver a elegirse. |
| Trazas | Issues #92, #111 · `EditarAnimalViewModel` · `ActualizarAnimalTest`, `EditarAnimalViewModelTest` |

<a id="cu-09"></a>**CU-09 — Dar de baja animal**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-11 |
| Objetivo / Fuente | OE3 / NORMA (Ley 1659) |
| Actor | Ganadero |
| Prioridad / Estado | M / Implementado (sin verificación de rol, ver RF-06). |
| Precondiciones | El animal está activo. |
| Flujo principal | 1. En el detalle, el Ganadero elige Dar de baja.<br>2. La app pide confirmación y explica que el historial se conserva.<br>3. El sistema registra `fechaBaja`.<br>4. El animal deja de aparecer en la lista. |
| Flujos alternos y excepciones | 2a. El usuario cancela: no cambia nada. |
| Offline / sincronización | Funciona sin señal. |
| Postcondiciones | Animal con `fechaBaja`. Su chapeta sigue activa; escanearla ofrece liberarla (CU-10). Los pesajes que lleguen con su código quedan pendientes (RN-10). |
| Reglas | RN-04, RN-10, RN-12 |
| Criterios de aceptación | 1. **Dado** un animal dado de baja, **entonces** su historial sigue en la base de datos.<br>2. **Dado** un pesaje con el código de un animal dado de baja, **entonces** queda pendiente con motivo `ANIMAL_DADO_DE_BAJA`. |
| Trazas | `AnimalDetailScreen` · `DetalleAnimalViewModelTest` |

<a id="cu-23"></a>**CU-23 — Registrar ubicación de la finca**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-14 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero |
| Prioridad / Estado | S / Implementado. Solo el Ganadero ve el botón; la app pide una posición nueva al GPS, avisa si falta el permiso y valida las coordenadas. |
| Flujo principal | 1. El Ganadero abre los datos de la finca y elige Registrar ubicación.<br>2. La app pide permiso de ubicación y toma la posición del GPS.<br>3. El Ganadero confirma y el sistema guarda latitud y longitud. |
| Flujos alternos y excepciones | 2a. Permiso denegado: la app permite ingresar las coordenadas a mano ⚠. |
| Offline / sincronización | El GPS funciona sin señal de datos. |
| Criterios de aceptación | 1. **Dado** el permiso concedido, **entonces** la finca queda con latitud y longitud.<br>2. **Dado** el permiso denegado, **entonces** la app no se cierra y ofrece la alternativa. |
| Trazas | Issue #93 · `RegistrarUbicacionFincaScreen` · `RegistrarUbicacionFincaViewModelTest`, `MappersPerfilFincaTest`, `MigracionesTest` |

##### C3. Identificación

<a id="cu-10"></a>**CU-10 — Escanear chapeta**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-15, RF-16 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Implementado |
| Precondiciones | El celular tiene NFC encendido. |
| Disparador | El usuario abre la pestaña Escanear y acerca un tag. |
| Flujo principal | 1. La app lee el UID del tag NFC-A y lo normaliza.<br>2. El sistema busca la chapeta activa de ese código:<br>• activa de un animal activo → abre su detalle;<br>• activa de un animal dado de baja → ofrece liberarla (CU-12) y luego registrar;<br>• sin chapeta activa, pero con historial → abre Registrar animal avisando a qué animal estuvo asociado;<br>• nunca visto → abre Registrar animal con el código precargado. |
| Flujos alternos y excepciones | 1a. El celular no tiene NFC: la app lo indica (el código se puede digitar en Registrar animal).<br>1b. NFC apagado: la app ofrece abrir los ajustes de NFC.<br>2a. ⚠ Al implementar RF-19, en los dos últimos casos se ofrece además "Asociar a un animal existente" (CU-11). |
| Offline / sincronización | Funciona sin señal. |
| Reglas | RN-01, RN-02 |
| Criterios de aceptación | 1. **Dado** un tag asociado a un animal activo, **cuando** se escanea, **entonces** se abre su detalle.<br>2. **Dado** el UID `04:a3:1b:2c`, **entonces** se busca como `04A31B2C`.<br>3. **Dado** un tag nunca visto, **entonces** se abre Registrar animal con el código en solo lectura. |
| Trazas | Issues #31, #36 · `EscanearScreen` · `EscanearViewModelTest` |

<a id="cu-11"></a>**CU-11 — Asociar chapeta a un animal existente**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-19 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Pendiente. Hoy solo se crea una chapeta al registrar un animal nuevo. |
| Precondiciones | El animal está activo y no tiene chapeta activa. |
| Disparador | "Asociar chapeta" en el detalle del animal, o "Asociar a un animal existente" tras escanear un código libre. |
| Flujo principal | 1. El usuario escanea el tag o digita el código (o lo trae del escaneo).<br>2. El sistema verifica que el código no tenga otra chapeta activa.<br>3. El sistema crea la chapeta activa para ese animal.<br>4. Si el código tiene pesajes pendientes, la app ofrece asignarlos. |
| Flujos alternos y excepciones | 2a. El código ya tiene una chapeta activa: la app lo indica y no crea nada.<br>P. El animal ya tiene chapeta activa: primero hay que liberarla (CU-12, solo Ganadero). |
| Offline / sincronización | Funciona sin señal. Un duplicado entre dispositivos se detecta al sincronizar (E-07, CU-22). |
| Postcondiciones | Nueva chapeta activa. Las chapetas anteriores del animal se conservan con su fecha de desasociación. |
| Reglas | RN-01, RN-02, RN-04 |
| Criterios de aceptación | 1. **Dado** un animal cuya chapeta fue liberada, **cuando** se le asocia un código nuevo, **entonces** el detalle muestra el código nuevo y el historial conserva el anterior.<br>2. **Dado** un código activo en otro animal, **entonces** no se asocia. |
| Trazas | — |

<a id="cu-12"></a>**CU-12 — Liberar chapeta**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-18 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero |
| Prioridad / Estado | M / Implementado (sin verificación de rol, ver RF-06). |
| Precondiciones | El animal tiene chapeta activa. |
| Disparador | "Liberar chapeta" en el detalle, o la oferta de Escanear para un animal dado de baja. |
| Flujo principal | 1. La app pide confirmación y explica que los pesajes anteriores se conservan.<br>2. El sistema registra `fechaDesasociacion`.<br>3. El código queda disponible para otro animal. |
| Flujos alternos y excepciones | 1a. El usuario cancela: no cambia nada. |
| Offline / sincronización | Funciona sin señal. |
| Reglas | RN-01, RN-04 |
| Criterios de aceptación | 1. **Dado** una chapeta liberada, **cuando** se escanea su código, **entonces** la app ofrece registrar avisando a qué animal estuvo asociada.<br>2. **Dado** una chapeta liberada, **entonces** los pesajes del animal siguen en su historial. |
| Trazas | `AnimalDetailScreen`, `EscanearScreen` · `DetalleAnimalViewModelTest`, `EscanearViewModelTest` |

##### C4. Pesajes

<a id="cu-13"></a>**CU-13 — Recibir pesaje del ESP32**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-20, RF-21, RF-22, RF-23, RF-24 |
| Objetivo / Fuente | OE3 / EQUIPO (ADR-0003, `docs/contrato-pesaje.md`) |
| Actor | ESP32 (sistema externo) |
| Prioridad / Estado | M / Implementado |
| Precondiciones | Servicio de pesaje activo (CU-14). El ESP32 está conectado al hotspot del celular. |
| Disparador | El ESP32 envía `POST http://<ip>:8080/pesaje` con `id_lectura`, `id_chip`, `peso` y, opcionalmente, `antiguedad_ms`. |
| Flujo principal | 1. El sistema valida que el cuerpo sea JSON con los campos y tipos correctos.<br>2. Valida los rangos de `peso` y `antiguedad_ms`.<br>3. Busca el `id_lectura` entre lo ya guardado.<br>4. Verifica que haya una cuenta en el dispositivo.<br>5. Normaliza el código y busca su chapeta activa y el animal.<br>6. Si el animal está activo, guarda el Registro con la hora de medición y responde 201 con `id_registro`. |
| Flujos alternos y excepciones | 1a / 2a. Cuerpo inválido o fuera de rango: 400 con el campo y el motivo; no guarda nada.<br>3a. `id_lectura` ya guardado: responde lo mismo que la primera vez y no guarda nada nuevo.<br>4a. Sin cuenta: 503; no guarda nada (el ESP32 reintenta).<br>6a. Código sin chapeta activa: guarda Pesaje pendiente y responde 202 con `CHIP_NO_ASOCIADO`.<br>6b. Animal dado de baja: guarda Pesaje pendiente y responde 202 con `ANIMAL_DADO_DE_BAJA`.<br>E1. Ruta o método equivocados: 404.<br>E2. Fallo interno: 500 (el ESP32 reintenta). |
| Offline / sincronización | Funciona sin internet; usa solo la red local del hotspot. |
| Postcondiciones | Un Registro o un Pesaje pendiente a nombre del Usuario del celular, con `sincronizado = false`. |
| Entidades | Registro, Pesaje pendiente, Chapeta, Animal |
| Reglas | RN-02, RN-05 a RN-10, RN-19 |
| Criterios de aceptación | 1. **Dado** un código con chapeta activa de un animal activo, **entonces** se responde 201 y el pesaje aparece en su detalle.<br>2. **Dado** el mismo `id_lectura` enviado dos veces, **entonces** existe un solo pesaje y ambas respuestas son iguales.<br>3. **Dado** `peso` = 0 o 1600, **entonces** se responde 400.<br>4. **Dado** `antiguedad_ms` = 60000, **entonces** la hora guardada es un minuto anterior a la recepción.<br>5. **Dado** un código desconocido, **entonces** se responde 202 y no se crea ningún animal ni chapeta. |
| Trazas | `PesajeHttpServer`, caso de uso `RegistrarPesaje` · Verificación: D (prueba E2E ESP32 → hotspot → Room con 201). Sin prueba automatizada. |

<a id="cu-14"></a>**CU-14 — Controlar el servicio de pesaje**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-25 |
| Objetivo / Fuente | OE3 / EQUIPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | M / Implementado |
| Flujo principal | 1. El usuario abre la pestaña Pesos.<br>2. La app muestra si el servicio está activo.<br>3. El usuario lo inicia o lo detiene.<br>4. Con el servicio activo, se muestra una notificación persistente con el puerto. |
| Flujos alternos y excepciones | 3a. Android 13 o superior sin permiso de notificaciones: la app lo solicita. |
| Offline / sincronización | Funciona sin señal. |
| Criterios de aceptación | 1. **Dado** el servicio iniciado, **entonces** la notificación está visible y la pantalla indica "activo".<br>2. **Dado** el servicio detenido, **entonces** un `POST /pesaje` no recibe respuesta. |
| Trazas | Issue #39 · `PesosScreen` · sin prueba automatizada |

<a id="cu-15"></a>**CU-15 — Conciliar pesajes pendientes**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-26 |
| Objetivo / Fuente | OE3 / EQUIPO (ADR-0003) |
| Actor | Ganadero (asignar y descartar), Operario (solo asignar) |
| Prioridad / Estado | M / Implementado (sin verificación de rol, ver RF-06). |
| Precondiciones | Hay pesajes pendientes sin resolver. |
| Disparador | El usuario toca el contador de pendientes en la lista de animales. |
| Flujo principal | 1. La app lista los pendientes sin resolver, del más reciente al más antiguo, con el estado actual de su código.<br>2. Para uno de ellos, el usuario elige Asignar; la app sugiere el animal que hoy tiene esa chapeta, pero permite elegir cualquier animal activo.<br>3. El usuario confirma; el sistema crea el Registro con la hora y el autor originales y marca el pendiente como asignado.<br>4. Si hay más pendientes del mismo código, la app ofrece asignarlos al mismo animal. |
| Flujos alternos y excepciones | 2a. El usuario elige Descartar y confirma: el pendiente queda descartado, sin crear Registro.<br>2b. El código no tiene chapeta activa: la app ofrece registrar un animal con ese código (CU-06).<br>3a. El animal fue dado de baja o el pendiente ya fue resuelto mientras tanto: la app lo indica y no guarda nada. |
| Offline / sincronización | Funciona sin señal. Una doble conciliación entre dispositivos se detecta al sincronizar (CU-22). |
| Postcondiciones | Pendiente resuelto (asignado o descartado) con Usuario y fecha de resolución; Registro creado si fue asignado. |
| Entidades | Pesaje pendiente, Registro, Animal |
| Reglas | RN-11, RN-12, RN-19, RN-21 |
| Criterios de aceptación | 1. **Dado** un pendiente asignado, **entonces** el pesaje aparece en el detalle del animal con su hora de medición original.<br>2. **Dado** un pendiente descartado, **entonces** desaparece de la lista y se conserva en la base de datos como descartado.<br>3. **Dado** que se asignan varios pendientes y uno falla, **entonces** no se asigna ninguno. |
| Trazas | Issues #52, #53, #61 · `PesosScreen` · `PesajesPendientesViewModelTest` |

<a id="cu-16"></a>**CU-16 — Registrar pesaje manual**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-27 |
| Objetivo / Fuente | OE3 / CAMPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | S / Implementado. |
| Precondiciones | El animal está activo. |
| Flujo principal | 1. En el detalle, el usuario elige Registrar pesaje.<br>2. Ingresa el peso y la fecha y hora de la medición (por defecto, el momento actual).<br>3. El sistema guarda el Registro sin `id_lectura`, a nombre del usuario. |
| Flujos alternos y excepciones | 2a. Peso fuera de 0 < peso ≤ 1500 kg: la app lo indica.<br>2b. ⚠ Fecha futura: la app no la acepta. |
| Offline / sincronización | Funciona sin señal. La idempotencia por `id_lectura` (RN-09) no aplica a pesajes manuales. |
| Reglas | RN-07, RN-12, RN-19 |
| Criterios de aceptación | 1. **Dado** un peso válido, **entonces** el pesaje aparece en el historial del animal con la fecha ingresada.<br>2. **Dado** un peso de 0, **entonces** no se guarda. |
| Trazas | Issue #95 · `PesajeFormScreen` · `NuevoPesajeViewModelTest` |

##### C5. Eventos

<a id="cu-17"></a>**CU-17 — Registrar evento**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-28, RF-29 |
| Objetivo / Fuente | OE3 / EQUIPO, NORMA (Ley 1659) |
| Actor | Ganadero, Operario |
| Prioridad / Estado | RF-28: M / Implementado. RF-29: C / Implementado. |
| Precondiciones | El animal está activo. |
| Flujo principal | 1. En la pestaña Eventos o en el detalle de un animal, el usuario elige Registrar evento.<br>2. Elige el tipo (vacunación o parto).<br>3. Ingresa el título, la fecha y hora del evento (por defecto, ahora; puede ser pasada o futura) y el animal, que ya viene elegido si entró desde su detalle.<br>4. Si es vacunación, ingresa el biológico (obligatorio), la dosis y la vía; si es parto, elige la cría o registra una nueva.<br>5. El sistema guarda el evento a nombre del usuario. |
| Flujos alternos y excepciones | 3a. Fecha futura: el evento queda programado hasta que esa fecha llega, y no cuenta en los eventos por mes de los indicadores.<br>4a. Parto con cría nueva: la app abre Registrar animal (CU-06) con la madre precargada.<br>4b. La cría elegida ya tiene otra madre o padre: la app no lo sobrescribe y no registra el parto; hay que corregirlo editando a la cría. |
| Offline / sincronización | Funciona sin señal. |
| Postcondiciones | Evento (con los campos de su tipo) con `sincronizado = false`. En un parto con cría, la cría queda con el animal del evento como madre (si es hembra) o como padre. |
| Entidades | Evento, Animal |
| Reglas | RN-14, RN-19 |
| Criterios de aceptación | 1. **Dado** una vacuna aplicada ayer y registrada hoy, **cuando** se ingresa la fecha de ayer, **entonces** el evento queda con la fecha de ayer.<br>2. **Dado** un evento guardado, **entonces** aparece en la pestaña Eventos del animal con el nombre del usuario que lo registró.<br>3. **Dado** una fecha de mañana, **cuando** se guarda el evento, **entonces** aparece como programado y no cuenta en los eventos por mes.<br>4. **Dado** un parto con una cría elegida, **entonces** la cría queda con la madre del evento. |
| Trazas | Issues #39, #44, #128 (reemplaza a #22) · `EventoFormScreen` · `NuevoEventoViewModelTest`, `RegistrarEventoTest`, `EventosViewModelTest` |

<a id="cu-18"></a>**CU-18 — Editar evento o pesaje**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-30 |
| Objetivo / Fuente | OE3 / EQUIPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | S / Pendiente |
| Precondiciones | El evento o pesaje no está dado de baja. |
| Flujo principal | 1. En el historial del animal, el usuario abre un evento o pesaje y elige Editar.<br>2. Modifica los campos (mismas validaciones del registro).<br>3. Guarda; el sistema crea una nueva versión con `rev_base` igual a la revisión que tenía el celular. |
| Flujos alternos y excepciones | 2a. Pesaje del ESP32: se conserva su `id_lectura`. |
| Offline / sincronización | Funciona sin señal. Puede generar un conflicto al sincronizar. |
| Reglas | RN-07, RN-15, RN-16 |
| Criterios de aceptación | 1. **Dado** un pesaje editado, **entonces** el historial muestra el valor nuevo y la versión anterior se conserva como revisión. |
| Trazas | — |

<a id="cu-19"></a>**CU-19 — Dar de baja evento o pesaje**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-31 |
| Objetivo / Fuente | OE3 / NORMA (Ley 1659) |
| Actor | Ganadero |
| Prioridad / Estado | C / Pendiente. El campo `fechaBaja` ya existe en el esquema. |
| Flujo principal | 1. En el historial, el usuario elige Dar de baja sobre un evento o pesaje.<br>2. Confirma.<br>3. El sistema registra `fechaBaja`; el elemento deja de aparecer en el historial. |
| Offline / sincronización | Funciona sin señal. |
| Reglas | RN-04 |
| Criterios de aceptación | 1. **Dado** un pesaje dado de baja, **entonces** no aparece en el historial ni en los indicadores, y sigue en la base de datos. |
| Trazas | — |

##### C6. Sincronización

<a id="cu-20"></a>**CU-20 — Sincronizar**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-32, RF-33, RF-36, RF-38 |
| Objetivo / Fuente | OE2, OE3 / EQUIPO (ADR-0004, ADR-0005) |
| Actor | Sistema (en nombre del usuario con sesión) |
| Prioridad / Estado | RF-32: M / Parcial: la sincronización corre al abrir la app, al iniciar sesión, dos minutos después de la última escritura local y con "Sincronizar ahora", como un único trabajo de WorkManager que espera la señal y reintenta con espera creciente (#83); `MotorSincronizacionTest` pasa en el A15; falta probar los disparadores en un dispositivo. RF-36: M / Implementado: la descarga (paso 7) corre después de la subida, con el mismo botón; `MotorSincronizacionDescargaTest` pasa en el A15. RF-33: S / Implementado. RF-38: S / Implementado: las fotos suben y bajan al final, después de la descarga de datos, y una foto que no pasa se reintenta en segundo plano (#86). |
| Precondiciones | Sesión válida y señal. |
| Disparador | Se abre la app, el usuario elige "Sincronizar ahora" o se guarda un cambio local. Cada disparo encola un único trabajo, que corre cuando hay señal. |
| Flujo principal | 1. El celular toma un lote pequeño de filas con `sincronizado = false`, de padres a hijos; las editables llevan su `rev_base` e `id_version`.<br>2. El celular llama la función `subir_cambios` por PostgREST con el JWT del usuario. La función corre como el usuario, de modo que aplican las políticas RLS.<br>3. Para cada fila editable, la función compara `rev_base` con la revisión vigente: si es igual, acepta y asigna `rev + 1`; si es menor, la guarda como sibling. Una baja se aplica siempre sobre la vigente (RN-22) y un `id_version` ya procesado recibe la respuesta anterior (RN-23).<br>4. La función responde por fila: aceptada (con su `rev`), sibling (con la revisión vigente) o rechazada (con motivo).<br>5. El celular marca las aceptadas con `sincronizado = true`; en un sibling reemplaza su fila con la revisión vigente y guarda su edición como sibling local, y la fila queda "en conflicto"; las rechazadas pasan a la bandeja (CU-22, RN-24). Repite con el siguiente lote.<br>6. El celular llama `descargar_cambios` con el último cursor (con margen de solapamiento) y recibe los cambios por páginas, con los siblings sin resolver. Aplica cada página de forma idempotente por id y `rev` y guarda el cursor después de cada una.<br>7. El celular registra la fecha de la última sincronización: los datos quedaron al día.<br>8. Aparte, el celular sube a Storage las fotos pendientes, convertidas a WebP (RNF-10), y descarga las que cambiaron en otros celulares de la finca. Una foto que falla queda para la próxima vez y no cambia el resultado ni la fecha; una que Storage no aceptará nunca pasa a la bandeja (CU-22). |
| Flujos alternos y excepciones | 1a. Se pierde la señal a mitad: los lotes ya confirmados quedan sincronizados; las filas sin confirmar siguen con `sincronizado = false` y se reenvían. Reenviarlas no duplica nada (UUID de la fila, `id_version` de la edición). Una descarga cortada se retoma desde el último cursor guardado.<br>1b. Señal débil: lotes pequeños, tiempos de espera cortos y reintentos con espera creciente; las fotos van al final y por separado, así que nunca retrasan los datos. "Sincronizar ahora" avisa al terminar los datos; las fotos siguen en segundo plano.<br>2a. Sesión rechazada: la app pide iniciar sesión.<br>4a. Violación de unicidad o de permisos: la fila se rechaza (CU-22). |
| Offline / sincronización | Requiere señal (LIM-02). |
| Postcondiciones | Servidor y celular con los mismos datos aceptados; conflictos y rechazos visibles. |
| Entidades | Todas las sincronizadas; revisiones; cursor |
| Reglas | RN-01, RN-13, RN-14, RN-15, RN-16, RN-21, RN-22, RN-23, RN-24 |
| Criterios de aceptación | 1. **Dado** 20 animales y 50 pesajes creados sin señal en un celular, **cuando** sincroniza, **entonces** el otro celular de la finca los recibe completos y sin alteración.<br>2. **Dado** dos ediciones del mismo animal desde la misma revisión, **entonces** la primera queda vigente y la segunda queda como sibling.<br>3. **Dado** una sincronización interrumpida y repetida, **entonces** no hay filas duplicadas en el servidor.<br>4. **Dado** un usuario de otra finca, **entonces** no recibe nada de esta. |
| Trazas | — |

<a id="cu-21"></a>**CU-21 — Resolver conflicto**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-34 |
| Objetivo / Fuente | OE2, OE3 / EQUIPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | S / Implementado. Un conflicto resuelto sigue abierto si el servidor no acepta la resolución. |
| Precondiciones | Hay filas en conflicto. |
| Flujo principal | 1. La app marca como "en conflicto" la fila y muestra la revisión vigente.<br>2. El usuario abre el conflicto; la app muestra la revisión vigente y los siblings, campo por campo.<br>3. El usuario elige la versión que queda ⚠ (o combina campo por campo).<br>4. El sistema guarda la elección como una edición nueva sobre la revisión vigente, que nombra los siblings que resuelve; esos siblings quedan resueltos y se conservan. Un sibling que llegue después queda sin resolver. |
| Flujos alternos y excepciones | 3a. El usuario no resuelve: la fila sigue consultable con la marca "en conflicto".<br>3b. Alguien edita la fila sin resolver el conflicto: la edición se acepta como revisión nueva y el conflicto sigue abierto (RN-17). |
| Reglas | RN-15, RN-16, RN-17 |
| Criterios de aceptación | 1. **Dado** un conflicto resuelto, **entonces** la marca desaparece en todos los celulares tras sincronizar, y el sibling sigue guardado.<br>2. **Dado** un conflicto sin resolver, **entonces** la fila se puede consultar. |
| Trazas | Issue #84 · `ResolverConflictoScreen` · `ResolverConflictoTest`, `ResolverConflictoViewModelTest`, `RoomConflictoRepositoryTest` |

<a id="cu-22"></a>**CU-22 — Corregir rechazos**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-35 |
| Objetivo / Fuente | OE2, OE3 / EQUIPO |
| Actor | Ganadero, Operario (según la acción que corrige) |
| Prioridad / Estado | M / Parcial: la lista avisa de los rechazos abiertos y la bandeja muestra cada uno con su motivo y la acción sugerida, y deja ver el animal o cerrar el rechazo, que se conserva (#85). "Asociar otro código" abre el detalle del animal, donde falta "Asociar chapeta" (RF-19, #94). |
| Precondiciones | El servidor rechazó una o más filas. |
| Flujo principal | 1. La app muestra un aviso y la bandeja de rechazados, con la entidad, el motivo y la acción sugerida.<br>2. Según el motivo:<br>• chapeta duplicada → el usuario asocia otro código al animal, o cierra el rechazo si el código sí es del otro animal;<br>• pendiente ya conciliado en otro celular → el usuario acepta la conciliación del servidor;<br>• permiso denegado → el usuario cierra el rechazo sin reenviar el cambio.<br>3. La corrección se guarda como una escritura nueva y se sincroniza; el rechazo queda cerrado. |
| Flujos alternos y excepciones | 2a. El usuario no corrige: la fila sigue en la bandeja y no se pierde. |
| Reglas | RN-01, RN-13, RN-21, RN-24. Un rechazo cerrado se conserva; no se borra. |
| Criterios de aceptación | 1. **Dado** el escenario E-07, **entonces** el segundo celular ve el rechazo con el nombre del animal que ya tiene ese código.<br>2. **Dado** una fila rechazada sin corregir, **entonces** sigue visible después de cerrar la app. |
| Trazas | — |

##### C7. Indicadores

<a id="cu-24"></a>**CU-24 — Consultar indicadores**

| Campo | Contenido |
|---|---|
| RF cubiertos | RF-39, RF-40, RF-41, RF-42, RF-43 |
| Objetivo / Fuente | OG, OE3 / EQUIPO |
| Actor | Ganadero, Operario |
| Prioridad / Estado | S (RF-42: C) / Implementado. |
| Precondiciones | Sesión iniciada. |
| Flujo principal | 1. El usuario abre Indicadores.<br>2. La app muestra, calculados sobre Room:<br>• inventario activo por propósito, sexo y etapa;<br>• evolución de peso de un animal elegido y ganancia diaria de peso promedio;<br>• pesajes pendientes sin conciliar y el más antiguo;<br>• eventos por tipo y por mes;<br>• animales sin chapeta activa y sin pesaje en los últimos N días (⚠ N = 30 por defecto).<br>3. La app muestra la fecha de la última sincronización. |
| Flujos alternos y excepciones | 2a. Sin datos suficientes para un indicador (por ejemplo, un solo pesaje para la ganancia diaria): la app lo indica en lugar de mostrar un valor. |
| Offline / sincronización | Funciona sin señal (LIM-05). |
| Reglas | RN-04 (no cuenta lo dado de baja) |
| Criterios de aceptación | 1. **Dado** 3 vacas de leche y 2 novillos de carne activos, **entonces** el inventario muestra esos conteos.<br>2. **Dado** un animal con 400 kg el día 1 y 430 kg el día 31, **entonces** su ganancia diaria es 1,0 kg/día.<br>3. **Dado** un animal dado de baja, **entonces** no aparece en ningún indicador. |
| Trazas | Issues #97, #98 · `IndicadoresScreen` · `IndicadoresViewModelTest` |

### 3.2 Requisitos de interfaz externa

#### 3.2.1 Interfaz de usuario

App en Jetpack Compose con Material 3 y navegación inferior de cuatro pestañas: Animales, Escanear, Eventos y Pesos.

| Pantalla | CU | Estado |
|---|---|---|
| Crear cuenta | CU-01, CU-04 | Implementada; falta canjear un código de invitación (CU-04) |
| Iniciar sesión | CU-02, CU-03 | Implementada, con recuperación de contraseña |
| Lista de animales | CU-07 | Implementada |
| Detalle de animal | CU-07, CU-09, CU-12 | Implementada |
| Registrar animal | CU-06 | Implementada |
| Registrar evento | CU-17 | Implementada |
| Escanear | CU-10 | Implementada |
| Pesos (servicio de pesaje y pesajes pendientes) | CU-14, CU-15 | Implementada |
| Eventos | CU-17 | Implementada |
| Registrar pesaje manual | CU-16 | Implementada |
| Editar animal | CU-08 | Implementada |
| Editar evento y pesaje; asociar chapeta | CU-11, CU-18, CU-19 | Pendientes |
| Datos de la finca (ubicación) | CU-23 | Implementada |
| Usuarios e invitación | CU-04, CU-05 | Pendientes |
| Conflictos | CU-21 | Implementada |
| Rechazos | CU-22 | Parcial |
| Estado de sincronización (cambios sin subir y última sincronización, en Indicadores) | CU-20 | Implementada |
| Indicadores | CU-24 | Implementada |

Criterios transversales de la interfaz: RNF-16.

#### 3.2.2 Interfaz de hardware

| Dispositivo | Uso | Detalle | Estado |
|---|---|---|---|
| NFC del celular | Leer el UID de las chapetas | Modo lector NFC-A, sin comprobación NDEF; solo lectura (LIM-01). `android.hardware.nfc` declarado como no obligatorio. | Implementado |
| Cámara | Foto del animal | Cámara o galería, a través de `FileProvider`. | Implementado |
| GPS | Ubicación de la finca (CU-23) | Permiso de ubicación declarado en el manifiesto; la app avisa si falta. | Implementado |
| ESP32 (vía Wi-Fi) | Pesajes | Se conecta al hotspot del celular; ver 3.2.4. | Implementado (lado app) |

#### 3.2.3 Interfaz de software

| Sistema | Uso | Requisitos de la interfaz | Estado |
|---|---|---|---|
| Supabase Auth | Crear cuenta, iniciar sesión, renovar sesión, recuperar contraseña | La app lo llama directamente. Los tokens se guardan en el almacenamiento seguro del dispositivo (RNF-11). | Implementado |
| Supabase PostgREST y Postgres | Base de datos central y sincronización | **Subida:** la función `subir_cambios` recibe un lote de filas (las editables con `rev_base` e `id_version`) y responde por fila: aceptada con su `rev`, sibling con la revisión vigente, o rechazada con motivo. **Descarga:** la función `descargar_cambios` recibe el último cursor y un límite, y devuelve una página de filas posteriores (con margen de solapamiento), los siblings sin resolver y el nuevo cursor. PostgREST exige `Authorization: Bearer <JWT del usuario>` y las funciones corren como ese usuario (`SECURITY INVOKER`). Políticas RLS por finca y rol en todas las tablas; índice único parcial para RN-01 e índice único para RN-21; secuencia global para el cursor; tablas de revisiones y siblings para Animal, Evento y Registro. Contrato en ADR-0004 y ADR-0005. | Pendiente |
| Supabase Storage | Fotos de animales | Archivos WebP con acceso restringido por finca mediante políticas. | Implementado |

#### 3.2.4 Interfaz de comunicaciones

| Enlace | Protocolo | Detalle |
|---|---|---|
| ESP32 → celular | HTTP sin cifrar, red local del hotspot, puerto 8080 | `POST /pesaje`. Contrato completo en `docs/contrato-pesaje.md` (resumen abajo). Sin autenticación (LIM-04). |
| Celular → Supabase | HTTPS | Solo cuando hay señal. |

Resumen del contrato de pesaje:

| Respuesta | Significado | ¿El ESP32 reintenta? |
|---|---|---|
| 201 `{"status":"ok","id_registro"}` | Registrado al animal | No |
| 202 `{"status":"pendiente_asociacion","id_pesaje_pendiente","motivo"}` | Guardado como pendiente (`CHIP_NO_ASOCIADO` o `ANIMAL_DADO_DE_BAJA`) | No |
| 400 | Cuerpo inválido | No |
| 404 | Ruta o método equivocados | No |
| 503 | El celular no tiene cuenta | Sí |
| 500 o sin respuesta | Fallo o conexión perdida | Sí, con el mismo `id_lectura` |

### 3.3 Requisitos no funcionales

#### 3.3.1 Rendimiento y eficiencia

<a id="rnf-05"></a>**RNF-05 — Tiempo de registro de un pesaje**

| Campo | Contenido |
|---|---|
| Categoría (ISO/IEC 25010) | Eficiencia de desempeño — comportamiento temporal |
| Enunciado medible | En el Samsung Galaxy A15, desde que la app recibe un `POST /pesaje` válido hasta que el pesaje es visible en el detalle abierto del animal: **≤ 5 s** para cada uno de 30 pesajes consecutivos. |
| Fuente | EQUIPO (KPI de OE3) |
| Verificación | D: 30 envíos con hora de envío registrada y observación de la pantalla. |
| RF afectados | RF-20 a RF-24 |
| Prioridad / Estado | M / Sin medir |
| Nota | El KPI de la propuesta mide "desde la detección del sensor". El tramo del sensor al envío pertenece al ESP32 (RES-08); la app solo puede medir desde la recepción. La tesis debe declarar esta diferencia. |

<a id="rnf-09"></a>**RNF-09 — Descarga incremental**

| Campo | Contenido |
|---|---|
| Categoría | Eficiencia de desempeño — utilización de recursos |
| Enunciado medible | La descarga transfiere solo las filas con cursor mayor que el último recibido, más el margen de solapamiento. Una sincronización sin cambios remotos no descarga filas fuera de ese margen. |
| Fuente | EQUIPO (RES-02: conectividad limitada) |
| Verificación | A/D: dos sincronizaciones seguidas sin cambios; la segunda devuelve solo filas del margen. |
| RF afectados | RF-36 |
| Prioridad / Estado | M / Implementado: `unaSegundaDescargaSinCambiosSoloPideElMargen` pasa en el A15. |

<a id="rnf-10"></a>**RNF-10 — Tamaño de las fotos**

| Campo | Contenido |
|---|---|
| Categoría | Eficiencia de desempeño — utilización de recursos |
| Enunciado medible | Antes de subirlas, las fotos se convierten a WebP con el lado mayor ≤ 1280 px y calidad 75. |
| Fuente | EQUIPO |
| Verificación | I: dimensiones y formato de 10 fotos subidas. |
| RF afectados | RF-08, RF-38 |
| Prioridad / Estado | S / Implementado: la conversión tiene pruebas en el celular (#86); falta la inspección de 10 fotos subidas al proyecto real. |

#### 3.3.2 Fiabilidad y disponibilidad sin conexión

<a id="rnf-01"></a>**RNF-01 — Operación sin señal**

| Campo | Contenido |
|---|---|
| Categoría | Fiabilidad — disponibilidad |
| Enunciado medible | El **100 %** de las funciones de registro y consulta, salvo las de LIM-02, operan con el celular en modo avión. |
| Fuente | CAMPO; KPI del objetivo general |
| Verificación | D: lista de verificación por RF ejecutada en modo avión en el Galaxy A15, con registro. |
| RF afectados | Todos, salvo los de LIM-02 |
| Prioridad / Estado | M / Diseño implementado (ADR-0001); sin registro de prueba |

<a id="rnf-02"></a>**RNF-02 — Ningún pesaje válido se pierde ni se duplica**

| Campo | Contenido |
|---|---|
| Categoría | Fiabilidad — tolerancia a fallos |
| Enunciado medible | El **100 %** de los pesajes válidos enviados queda guardado exactamente una vez (como Registro o Pesaje pendiente), aunque el ESP32 reintente. Las escrituras de cada caso de uso son todo o nada. |
| Fuente | EQUIPO (ADR-0003) |
| Verificación | P: `RegistrarPesajeTest` y `PesajeHttpServerIntegracionTest` (en el A15). D: script que envía N pesajes con reintentos duplicados y compara conteos. |
| RF afectados | RF-20 a RF-24, RF-26 |
| Prioridad / Estado | M / Implementado: `RegistrarPesajeTest` y `PesajeHttpServerIntegracionTest`. |

<a id="rnf-03"></a>**RNF-03 — Servicio activo con la pantalla apagada**

| Campo | Contenido |
|---|---|
| Categoría | Fiabilidad — disponibilidad |
| Enunciado medible | El servicio de pesaje sigue recibiendo pesajes con la pantalla apagada durante **≥ 15 min**. |
| Fuente | EQUIPO (especificación inicial, sección 8) |
| Verificación | D: registro existente ⚠ [ubicación de la evidencia]. |
| RF afectados | RF-20, RF-25 |
| Prioridad / Estado | M / Verificado |

<a id="rnf-04"></a>**RNF-04 — Sin borrado físico e integridad del esquema**

| Campo | Contenido |
|---|---|
| Categoría | Fiabilidad — integridad |
| Enunciado medible | Ninguna operación borra filas de trazabilidad. Desde la versión 10 del esquema, cada cambio lleva una migración explícita y su esquema exportado; si falta una migración, la app falla en lugar de borrar datos. |
| Fuente | NORMA (Ley 1659); EQUIPO |
| Verificación | I: casos de uso, DAOs, `Migraciones.kt` y `app/schemas/`. En el servidor: ninguna política RLS permite `DELETE`. |
| RF afectados | RF-11, RF-18, RF-31 |
| Prioridad / Estado | M / Implementado en el celular; pendiente en el servidor |

<a id="rnf-08"></a>**RNF-08 — Integridad de la sincronización**

| Campo | Contenido |
|---|---|
| Categoría | Fiabilidad — integridad |
| Enunciado medible | **≥ 95 %** de las filas sincronizadas llegan al servidor y al otro dispositivo sin pérdida ni alteración. Las filas rechazadas quedan en la bandeja (RF-35), no se pierden. |
| Fuente | EQUIPO (KPI del objetivo general) |
| Verificación | D: ≥ 100 filas creadas sin señal en un dispositivo; tras sincronizar, comparación de conteos y de contenido por fila en el servidor y en el segundo dispositivo. |
| RF afectados | RF-32, RF-35, RF-36 |
| Prioridad / Estado | M / Sin medir: la sincronización existe; falta la medición con ≥ 100 filas y dos dispositivos. |

#### 3.3.3 Seguridad y privacidad

<a id="rnf-11"></a>**RNF-11 — Autenticación y sesión**

| Campo | Contenido |
|---|---|
| Categoría | Seguridad — autenticidad, confidencialidad |
| Enunciado medible | La identidad la gestiona Supabase Auth, con contraseñas de al menos 6 caracteres. Los tokens se guardan en almacenamiento cifrado respaldado por el Android Keystore; nunca en Room ni en texto plano. |
| Fuente | EQUIPO (ADR-0002) |
| Verificación | I: código de almacenamiento de la sesión. D: inspección de la base de datos local. |
| RF afectados | RF-01 a RF-03, RF-07 |
| Prioridad / Estado | M / Implementado: la sesión se guarda cifrada con una clave AES-GCM del Android Keystore, fuera de Room. |

<a id="rnf-12"></a>**RNF-12 — Autorización por finca y rol**

| Campo | Contenido |
|---|---|
| Categoría | Seguridad — confidencialidad, integridad |
| Enunciado medible | Todas las tablas y el bucket de fotos tienen políticas RLS por finca y rol. Las funciones de sincronización corren con el JWT del usuario (`SECURITY INVOKER`); la clave de servicio nunca se usa para operaciones de usuario. |
| Fuente | EQUIPO (ADR-0002) |
| Verificación | D: con dos cuentas de fincas distintas y una de Operario, intentar leer y escribir fuera de los permisos; todos los intentos se rechazan. |
| RF afectados | RF-06, RF-32, RF-36, RF-38 |
| Prioridad / Estado | M / Implementado: políticas RLS por finca y rol, con las pruebas pgTAP `01_aislamiento_por_finca`, `02_permisos_por_rol` y `07_fotos`. |

<a id="rnf-13"></a>**RNF-13 — Tratamiento de datos personales**

| Campo | Contenido |
|---|---|
| Categoría | Seguridad — cumplimiento |
| Enunciado medible | No se crea ninguna cuenta sin la aceptación previa de la autorización de tratamiento de datos. Se registran la fecha de aceptación y la versión del texto aceptado. |
| Fuente | NORMA (Ley 1581 de 2012) |
| Verificación | I + D |
| RF afectados | RF-01, RF-04, RF-05 |
| Prioridad / Estado | M / Parcial: la autorización se pide antes de crear la cuenta y se registra su fecha y versión (#87); el texto es provisional (`0.1-provisional`), ⚠ [ubicación]. |
| Nota | `allowBackup` está en `false`: Android no copia la base local a la nube de Google. |

<a id="rnf-14"></a>**RNF-14 — Auditoría**

| Campo | Contenido |
|---|---|
| Categoría | Seguridad — responsabilidad |
| Enunciado medible | Toda escritura conserva su autor y su fecha de modificación. Las conciliaciones guardan quién y cuándo. Las revisiones y siblings se conservan sin depuración. |
| Fuente | NORMA (Ley 1659); EQUIPO |
| Verificación | I |
| RF afectados | RF-23, RF-26, RF-28, RF-30, RF-33 |
| Prioridad / Estado | M / Implementado: el autor se registra en Evento, Registro y Pesaje pendiente, y el servidor conserva las revisiones y los siblings sin depuración (tablas `revision` y `sibling`). |

#### 3.3.4 Usabilidad

<a id="rnf-15"></a>**RNF-15 — Eficiencia frente al registro manual**

| Campo | Contenido |
|---|---|
| Categoría | Usabilidad — eficiencia, satisfacción |
| Enunciado medible | **≥ 30 %** de reducción del tiempo promedio de registro por bovino frente al método manual, y **≥ 80 %** de aceptación operativa de los usuarios participantes. |
| Fuente | EQUIPO (KPI de OE4). ⚠ Ajustar si la tesis reemplaza la tabla de KPI. |
| Verificación | D/A en la finca piloto: tiempos cronometrados (manual vs. app) y cuestionario TAM. |
| RF afectados | RF-08, RF-15, RF-20, RF-28 |
| Prioridad / Estado | M / Pendiente (Fase 6) |

<a id="rnf-16"></a>**RNF-16 — Uso en campo**

| Campo | Contenido |
|---|---|
| Categoría | Usabilidad — prevención de errores, operabilidad |
| Enunciado medible | Toda acción que no se puede deshacer (dar de baja, liberar, descartar, quitar usuario) pide confirmación explicando su efecto. Los campos de entrada tienen una altura mínima de 54 dp. |
| Fuente | EQUIPO |
| Verificación | I |
| RF afectados | RF-11, RF-18, RF-26, RF-31, RF-37 |
| Prioridad / Estado | S / Implementado en las acciones existentes |

#### 3.3.5 Compatibilidad

<a id="rnf-06"></a>**RNF-06 — Plataforma y dispositivos**

| Campo | Contenido |
|---|---|
| Categoría | Compatibilidad / portabilidad |
| Enunciado medible | La app funciona en Android 7.0 (API 24) o superior. En un celular sin NFC se instala y opera, y la chapeta se ingresa digitando el código. Se verifica en el Samsung Galaxy A15 y el Xiaomi 14T. |
| Fuente | EQUIPO (RES-01) |
| Verificación | I (`minSdk = 24`, NFC no obligatorio en el manifiesto) + D en los dos dispositivos |
| RF afectados | RF-15, RF-17 |
| Prioridad / Estado | M / Implementado; verificación en dispositivos pendiente (SUP-05) |

#### 3.3.6 Mantenibilidad y verificabilidad

<a id="rnf-07"></a>**RNF-07 — Pruebas del prototipo**

| Campo | Contenido |
|---|---|
| Categoría | Mantenibilidad — capacidad de prueba |
| Enunciado medible | Las reglas de negocio viven en un módulo de Kotlin puro (`:domain`), sin dependencias de Android. Cada caso de uso del dominio tiene al menos una prueba unitaria; existe al menos una prueba de integración servidor HTTP → Room; todas las pruebas pasan en `main`. |
| Fuente | EQUIPO (ADR-0001; KPI de OE3: pruebas unitarias y de integración) |
| Verificación | I + ejecución de `./gradlew test` |
| RF afectados | Todos |
| Prioridad / Estado | M / Implementado: 274 pruebas JVM, 108 de instrumentación y 150 de pgTAP, con una prueba por caso de uso y `PesajeHttpServerIntegracionTest`. En el A15 pasan 107 de las 108: `ExampleInstrumentedTest` solo comprueba el nombre del paquete y falló porque la corrida usó otro `applicationId`. |

---

## 4. Matriz de trazabilidad

**Leyenda de verificación:** P = prueba automatizada (nombre del archivo en `app/src/test`) · D = demostración manual con registro · — = sin evidencia todavía. La columna "Conf." indica la confirmación del ganadero (⚠ = pendiente de registrar).

### 4.1 Requisitos funcionales

| RF | CU | Fuente | Conf. | Obj. | Pri. | Estado | Verificación | Issue |
|---|---|---|---|---|---|---|---|---|
| RF-01 | CU-01 | EQUIPO | — | OE3 | M | Implementado | P `CrearCuentaViewModelTest`, `CrearCuentaTest`, `RoomCuentaRepositoryTest`, `06_crear_finca` | #40, #87 |
| RF-02 | CU-02 | EQUIPO | — | OE3 | M | Implementado | P `LoginViewModelTest`, `InicioViewModelTest` | #23, #40 |
| RF-03 | CU-02 | EQUIPO | — | OE3 | M | Implementado | P `InicioViewModelTest`; I `SesionCifrada` | #23 |
| RF-04 | CU-01 | NORMA | — | OE3 | M | Implementado | P `CrearCuentaViewModelTest`, `CrearCuentaTest`, `06_crear_finca` | #87 |
| RF-05 | CU-04 | CAMPO | ⚠ | OE3 | M | Pendiente | — | #88 |
| RF-06 | Transv. | CAMPO | ⚠ | OE3 | M | Implementado | P `DarDeBajaAnimalTest`, `LiberarChapetaTest`, `DescartarPesajePendienteTest`, `02_permisos_por_rol` | #79, #89 |
| RF-07 | CU-03 | EQUIPO | — | OE3 | C | Implementado | P `LoginViewModelTest` | #91 |
| RF-08 | CU-06 | CAMPO | ⚠ | OE3 | M | Implementado | P `RegistrarAnimalViewModelTest` | #38 |
| RF-09 | CU-07 | EQUIPO | — | OE3 | M | Implementado | P `ListaAnimalesViewModelTest` | #37 |
| RF-10 | CU-07 | CAMPO | ⚠ | OE3 | M | Implementado | P `DetalleAnimalViewModelTest` | #37 |
| RF-11 | CU-09 | NORMA | — | OE3 | M | Implementado | P `DetalleAnimalViewModelTest` | — |
| RF-12 | CU-08 | CAMPO | ⚠ | OE3 | S | Implementado | P `ActualizarAnimalTest`, `EditarAnimalViewModelTest` | #92 |
| RF-13 | CU-06, CU-08 | CAMPO | ⚠ | OE3 | S | Implementado | P `RegistrarAnimalViewModelTest`, `DetalleAnimalViewModelTest`, `RoomAnimalRepositoryTest` | #20 |
| RF-14 | CU-23 | CAMPO | ⚠ | OE3 | S | Implementado | P `RegistrarUbicacionFincaViewModelTest`, `MappersPerfilFincaTest` | #93 |
| RF-15 | CU-10 | CAMPO | ⚠ | OE3 | M | Implementado | D (lectura de un NTAG215; registro pendiente) | #31 |
| RF-16 | CU-10 | CAMPO | ⚠ | OE3 | M | Implementado | P `EscanearViewModelTest` | #31, #36 |
| RF-17 | CU-06 | CAMPO | ⚠ | OE3 | M | Implementado | P `RegistrarAnimalViewModelTest` | #38 |
| RF-18 | CU-12 | CAMPO | ⚠ | OE3 | M | Implementado | P `DetalleAnimalViewModelTest`, `EscanearViewModelTest` | — |
| RF-19 | CU-11 | CAMPO | ⚠ | OE3 | M | Pendiente | — | #94 |
| RF-20 | CU-13 | EQUIPO | — | OE3 | M | Implementado | D (repetir con el contrato vigente) | — |
| RF-21 | CU-13 | EQUIPO | — | OE3 | M | Implementado | D (repetir con el contrato vigente) | — |
| RF-22 | CU-13 | EQUIPO | — | OE3 | M | Implementado | D (repetir con el contrato vigente) | #52 |
| RF-23 | CU-13 | EQUIPO | — | OE3 | M | Implementado | D (repetir con el contrato vigente) | #52 |
| RF-24 | CU-13 | EQUIPO | — | OE3 | M | Implementado | D (repetir con el contrato vigente) | — |
| RF-25 | CU-14 | EQUIPO | — | OE3 | M | Implementado | D | #39 |
| RF-26 | CU-15 | EQUIPO | — | OE3 | M | Implementado | P `PesajesPendientesViewModelTest` | #53, #61 |
| RF-27 | CU-16 | CAMPO | ⚠ | OE3 | S | Implementado | P `NuevoPesajeViewModelTest` | #95 |
| RF-28 | CU-17 | EQUIPO / NORMA | — | OE3 | M | Implementado | P `NuevoEventoViewModelTest`, `EventosViewModelTest`, `ObservarIndicadoresHatoTest` | #39, #44, #128 |
| RF-29 | CU-17 | EQUIPO | — | OE3 | C | Implementado | P `RegistrarEventoTest`, `MappersEventoTest`, `RoomEventoRepositoryTest`, `04_subir_cambios` | #128 (reemplaza a #22) |
| RF-30 | CU-18 | EQUIPO | — | OE3 | S | Pendiente | — | #96 |
| RF-31 | CU-19 | NORMA | — | OE3 | C | Pendiente | — | #96 |
| RF-32 | CU-20 | EQUIPO | — | OE2, OE3 | M | Parcial | P `04_subir_cambios`, `MotorSincronizacionTest`, `DisparosDeSincronizacionTest` | #81, #83 |
| RF-33 | CU-20 | EQUIPO | — | OE2, OE3 | S | Implementado | P `04_subir_cambios`, `MotorSincronizacionTest` | #80, #81 |
| RF-34 | CU-21 | EQUIPO | — | OE2, OE3 | S | Implementado | P `ResolverConflictoTest`, `ResolverConflictoViewModelTest`, `RoomConflictoRepositoryTest` | #84 |
| RF-35 | CU-22 | EQUIPO | — | OE2, OE3 | M | Parcial | — | #85, #94 |
| RF-36 | CU-20 | EQUIPO | — | OE2, OE3 | M | Implementado | P `05_descargar_cambios`, `MotorSincronizacionDescargaTest` (en el A15, 28-10-2026) | #82 |
| RF-37 | CU-05 | EQUIPO | — | OE3 | C | Pendiente | — | #90 |
| RF-38 | CU-20 | EQUIPO | — | OE3 | S | Implementado | P `MotorSincronizacionFotosTest`, `FotosEnArchivosTest` (en el A15, 28-10-2026), `07_fotos` | #86 |
| RF-39 | CU-24 | EQUIPO | — | OE3 | S | Implementado | P `IndicadoresViewModelTest` | #97 |
| RF-40 | CU-24 | EQUIPO | — | OE3 | S | Implementado | P `IndicadoresViewModelTest` | #43, #98 |
| RF-41 | CU-24 | EQUIPO | — | OE3 | S | Implementado | P `IndicadoresViewModelTest` | #97 |
| RF-42 | CU-24 | EQUIPO | — | OE3 | C | Implementado | P `IndicadoresViewModelTest` | #98 |
| RF-43 | CU-24 | EQUIPO | — | OG, OE3 | S | Implementado | P `IndicadoresViewModelTest` | #97 |

### 4.2 Requisitos no funcionales

| RNF | Categoría | Fuente | Conf. | Obj. | Pri. | Estado | Verificación |
|---|---|---|---|---|---|---|---|
| RNF-01 | Fiabilidad | CAMPO | ⚠ | OG | M | Diseño implementado | D (modo avión; registro pendiente) |
| RNF-02 | Fiabilidad | EQUIPO | — | OE3 | M | Implementado | P `RegistrarPesajeTest`, `PesajeHttpServerIntegracionTest` |
| RNF-03 | Fiabilidad | EQUIPO | — | OE3 | M | Verificado | D ⚠ [ubicación] |
| RNF-04 | Fiabilidad | NORMA | — | OE3 | M | Implementado (celular) | I |
| RNF-05 | Rendimiento | EQUIPO | — | OE3 | M | Sin medir | — |
| RNF-06 | Compatibilidad | EQUIPO | — | OE3 | M | Implementado | I; D pendiente |
| RNF-07 | Mantenibilidad | EQUIPO | — | OE3 | M | Implementado | P (274 JVM, 108 de instrumentación y 150 pgTAP) |
| RNF-08 | Fiabilidad | EQUIPO | — | OG | M | Sin medir | — |
| RNF-09 | Rendimiento | EQUIPO | — | OE2 | M | Implementado | P `05_descargar_cambios`, `MotorSincronizacionDescargaTest` (en el A15, 28-10-2026) |
| RNF-10 | Rendimiento | EQUIPO | — | OE3 | S | Implementado | — |
| RNF-11 | Seguridad | EQUIPO | — | OE3 | M | Implementado | I `SesionCifrada` |
| RNF-12 | Seguridad | EQUIPO | — | OE3 | M | Implementado | P `01_aislamiento_por_finca`, `02_permisos_por_rol`, `07_fotos` |
| RNF-13 | Privacidad | NORMA | — | OE3 | M | Parcial | P `CrearCuentaTest`, `06_crear_finca` |
| RNF-14 | Seguridad | NORMA | — | OE3 | M | Implementado | I; P `04_subir_cambios` |
| RNF-15 | Usabilidad | EQUIPO | — | OE4 | M | Pendiente | — |
| RNF-16 | Usabilidad | EQUIPO | — | OE3 | S | Implementado | I |

