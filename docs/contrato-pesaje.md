# Contrato del pesaje (ESP32 → app)

El ESP32 conectado a la báscula le reporta cada pesaje a la app con una petición HTTP. Este documento es la referencia para el firmware. El porqué de las decisiones está en `docs/adr/0003-todo-pesaje-valido-se-guarda.md`.

El servidor vive en la app (`PesajeHttpServer`, dentro de un servicio en primer plano) y escucha en el puerto `8080` del teléfono. Es HTTP sin cifrar, pensado para la red local entre el ESP32 y el teléfono.

## Petición

`POST http://<ip-del-telefono>:8080/pesaje`, con el encabezado `Content-Type: application/json` y cuerpo JSON:

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `id_lectura` | string | Sí | UUID que genera el ESP32 para cada pesaje. Un reintento del mismo pesaje lleva el **mismo** valor. |
| `id_chip` | string | Sí | Código de la Chapeta leída, tal como sale del tag. La app lo normaliza (mayúsculas, sin espacios, `:` ni `-`). |
| `peso` | number | Sí | Peso en kg. Mayor que 0 y menor o igual a 1500. |
| `antiguedad_ms` | number | No | Milisegundos transcurridos entre la medición y este envío; mayor o igual a 0. Si no viene, se asume 0. |

```json
{ "id_lectura": "5f0c2a9e-7d7b-4e0a-9d5e-1c2b3a4d5e6f", "id_chip": "04A31B2C", "peso": 452.5, "antiguedad_ms": 1200 }
```

El ESP32 **no envía su reloj**. La app calcula la hora del pesaje como el momento en que recibe la petición menos `antiguedad_ms`. Si el ESP32 se reinicia entre la medición y el envío y pierde esa cuenta, es mejor que no envíe el campo.

## Respuestas

| Código | Cuerpo | Significado | ¿El ESP32 reintenta? |
|---|---|---|---|
| `201` | `{"status":"ok","id_registro":"<uuid>"}` | Pesaje registrado al animal. | No |
| `202` | `{"status":"pendiente_asociacion","id_pesaje_pendiente":"<uuid>","motivo":"<motivo>"}` | Pesaje guardado como pendiente: no se pudo asignar a un animal. | No |
| `400` | `{"status":"error","message":"<campo>: <motivo>"}` | Cuerpo inválido: falta un campo, es de otro tipo o está fuera de rango. | No |
| `404` | `{"status":"error","message":"ruta no encontrada"}` | Ruta o método equivocados. | No |
| `503` | `{"status":"error","message":"sin sesion activa"}` | El teléfono todavía no tiene una cuenta creada. No se guarda nada. | Sí |
| `500` | `{"status":"error","message":"error interno"}` | Fallo de la app. | Sí |
| Sin respuesta | | Se perdió la conexión. | Sí |

Todo reintento usa el **mismo** `id_lectura`.

El `motivo` de un `202` es uno de estos:

| `motivo` | Significado |
|---|---|
| `CHIP_NO_ASOCIADO` | El código no tiene una Chapeta activa en la app. |
| `ANIMAL_DADO_DE_BAJA` | El código tiene una Chapeta activa, pero su animal está dado de baja. |

Un `202` no es un error para el ESP32: el pesaje quedó guardado y alguien lo asignará después desde la app. Conviene que la báscula lo indique, porque suele significar que a ese animal todavía no se le ha asociado la chapeta en la app.

## Reglas

- **Orden.** La app primero valida el cuerpo (`400`), después busca el `id_lectura` entre lo ya guardado, después comprueba que haya cuenta (`503`) y por último busca la Chapeta.
- **Ningún pesaje válido se descarta.** Todo pesaje que pasa la validación y llega con una cuenta creada queda guardado, como registro del animal (`201`) o como pendiente (`202`).
- **Idempotencia.** Si la app ya guardó un pesaje con ese `id_lectura`, responde lo mismo que la primera vez (`201` con el mismo `id_registro`, o `202` con el mismo `id_pesaje_pendiente` y motivo) y no guarda nada nuevo, aunque el código o el peso del reintento sean otros. Por eso reintentar siempre es seguro. Un reintento con el cuerpo inválido recibe `400`, como cualquier otro.
- **Nunca se crean animales ni chapetas** a partir de un pesaje. Un código desconocido solo produce un pesaje pendiente.
- **Usuario.** Cada pesaje guardado, sea registro o pendiente, queda a nombre del Usuario del teléfono que lo recibió.

## Probarlo sin ESP32

Con el servicio iniciado desde la pestaña Conexión y el teléfono conectado por `adb`:

```sh
adb forward tcp:8080 tcp:8080
curl -i -X POST http://localhost:8080/pesaje \
  -H 'Content-Type: application/json' \
  -d '{"id_lectura":"prueba-1","id_chip":"TEST001","peso":450.5}'
```
