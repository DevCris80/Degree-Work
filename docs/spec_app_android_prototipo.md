# Especificación técnica — App Android (Prototipo inicial)
## Sistema de Trazabilidad Bovina

---

## 1. Contexto

App Android nativa para un sistema de trazabilidad bovina con arquitectura **offline-first**. Esta primera iteración cubre exclusivamente: (a) persistencia local, (b) registro manual de animales, y (c) recepción de datos de peso enviados por un ESP32 externo vía HTTP. **No incluye** lectura NFC real ni sincronización con Supabase — ambas quedan para iteraciones posteriores (ver sección 8, Fuera de alcance).

---

## 2. Stack obligatorio

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Persistencia local:** Room (sobre SQLite)
- **Servidor HTTP embebido:** NanoHTTPD (o librería equivalente ligera), corriendo dentro de un **Foreground Service** con notificación persistente
- **Concurrencia:** Corrutinas de Kotlin
- **IDs:** UUID (`java.util.UUID`), **no autoincremento** — es un requisito de diseño, no una preferencia, porque estos IDs deben poder coexistir sin colisión con una futura sincronización a una base de datos remota

---

## 3. Modelo de datos (Room)

Todas las entidades usan `id: String` (UUID) como clave primaria, y un campo `sincronizado: Boolean = false` para preparar la futura sincronización (no se implementa aún, pero el campo debe existir desde ahora para no requerir migración de esquema después).

### Entidad `Animal`
| Campo | Tipo | Notas |
|---|---|---|
| id_animal | String (UUID) | PK |
| nombre | String | |
| raza | String | |
| sexo | String | |
| edad | Int | |
| proposito | String | |
| sincronizado | Boolean | default false |

### Entidad `Evento`
| Campo | Tipo | Notas |
|---|---|---|
| id_evento | String (UUID) | PK |
| id_animal | String | FK → Animal |
| tipo_evento | String | ej. "vacuna", "tratamiento", "nacimiento", "inseminacion" |
| fecha | Long | epoch millis |
| detalle | String | |
| sincronizado | Boolean | default false |

### Entidad `Registro` (pesajes)
| Campo | Tipo | Notas |
|---|---|---|
| id_registro | String (UUID) | PK |
| id_animal | String | FK → Animal (resuelto vía lookup, ver sección 5) |
| peso | Float | |
| timestamp | Long | epoch millis, generado al recibir el dato |
| sincronizado | Boolean | default false |

### Entidad `Chapeta`
| Campo | Tipo | Notas |
|---|---|---|
| id_chip | String | PK — UID del chip físico (en esta fase, valor de prueba manual) |
| id_animal | String | FK → Animal |
| fecha_asociacion | Long | epoch millis |
| fecha_desasociacion | Long? | nullable |
| sincronizado | Boolean | default false |

Usar Room con DAOs estándar (`@Insert`, `@Query` para lookups) y `Flow` para exponer datos reactivos a la UI.

---

## 4. Pantallas requeridas (Jetpack Compose)

1. **Lista de animales** — muestra todos los `Animal` registrados (nombre, raza). Tap para ver detalle.
2. **Registro de animal nuevo** — formulario con los campos de `Animal`. Al guardar, genera UUID y persiste en Room.
3. **Detalle de animal** — muestra datos del animal, lista de `Registro` (pesajes) asociados y lista de `Evento` asociados, ambos ordenados por fecha descendente.
4. **Registro de evento** — formulario simple para crear un `Evento` asociado a un animal existente.
5. **Pantalla de estado del servicio HTTP** — indicador visual simple (activo/inactivo) de si el foreground service está corriendo, con botón para iniciar/detener. Útil para depuración durante el desarrollo del prototipo.

No se requiere autenticación, ni theming avanzado, ni animaciones — priorizar funcionalidad sobre pulido visual en esta iteración.

---

## 5. Foreground Service + Servidor HTTP

### Requisito de estabilidad (crítico)
El servicio debe seguir escuchando peticiones **con la pantalla del celular apagada**. Implementar como `Foreground Service` con notificación persistente visible (obligatorio en Android 8+), y declarar el tipo de servicio correspondiente en el manifest (`android:foregroundServiceType="dataSync"` o el que aplique).

### Contrato del endpoint

> **Reemplazado.** El contrato vigente está en [`docs/contrato-pesaje.md`](contrato-pesaje.md) (`POST /pesaje`, ver ADR 0003). Lo que sigue describe el prototipo inicial y se conserva solo como registro.

- **Método:** `POST`
- **Ruta:** `/registro-peso`
- **Body (JSON):**
```json
{
  "id_chip": "string",
  "peso": 123.45
}
```
- **Comportamiento al recibir la petición:**
  1. Buscar en la tabla `Chapeta` un registro donde `id_chip` coincida.
  2. Si existe match → obtener `id_animal` asociado.
  3. Crear un nuevo `Registro` con: `id_registro` (UUID nuevo), `id_animal` (resuelto), `peso` (del body), `timestamp` (momento de recepción, no depender de timestamp del ESP32).
  4. Persistir en Room.
  5. Responder `200 OK` con un JSON simple de confirmación, ej. `{"status": "ok", "id_registro": "..."}`.
  6. **Si no hay match de `id_chip` en `Chapeta`:** responder `404` con un mensaje de error claro (`{"status": "error", "message": "chip no asociado"}`), y no crear ningún `Registro`. No inventar ni crear animales automáticamente bajo ninguna circunstancia — esto es una decisión de diseño explícita, no un descuido.

### Permisos necesarios en el Manifest
`INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` (Android 14+), `POST_NOTIFICATIONS` (Android 13+, requiere solicitud en runtime).

---

## 6. Dato de prueba precargado (obligatorio para poder probar el flujo end-to-end)

Al iniciar la app por primera vez (o mediante un botón de "Seed data" visible solo en esta fase de desarrollo), insertar en Room:

- Un `Animal` de prueba con datos ficticios completos.
- Un `Chapeta` de prueba con `id_chip = "TEST001"` (o el valor que se vaya a hardcodear en el firmware del ESP32) asociado al `id_animal` de ese animal de prueba.

Esto permite que, al recibir el primer POST del ESP32 (que enviará ese mismo `id_chip` fijo), el lookup encuentre coincidencia real y el flujo se pueda probar de punta a punta sin depender de NFC físico.

---

## 7. Fuera de alcance en esta iteración (no implementar todavía)

- **Lectura NFC real de chips físicos** — no hay tags disponibles aún para probar. La tabla `Chapeta` y su lógica de asociación deben existir y funcionar (vía precarga manual, sección 6), pero no construir la pantalla ni el código de escaneo NFC en esta fase.
- **Sincronización con Supabase** — el campo `sincronizado` debe existir en el esquema, pero no implementar ninguna llamada de red hacia Supabase todavía. No es necesario ni un botón de "sincronizar" funcional en esta fase.
- **Autenticación de usuarios / roles** (Ganadero, Operario, Administrador) — no implementar en este prototipo.
- **Reportes exportables** — no aplica a esta fase.

---

## 8. Criterio de aceptación de esta iteración

El prototipo se considera funcional cuando:
1. Se puede registrar un animal manualmente desde la app y verlo en la lista.
2. El foreground service permanece activo con la pantalla apagada durante al menos 15 minutos (verificar manualmente).
3. Un POST HTTP externo (ej. desde Postman o `curl`, simulando al ESP32) con un `id_chip` precargado (sección 6) crea correctamente un `Registro` asociado al `id_animal` correcto, visible en el detalle del animal.
4. Un POST con un `id_chip` no reconocido responde `404` sin crear ningún registro corrupto.
