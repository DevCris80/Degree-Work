---
status: accepted
---

# Todo pesaje válido se guarda, y el endpoint es idempotente

La spec original (`docs/spec_app_android_prototipo.md`, sección 5) respondía 404 y no guardaba el pesaje cuando el código no tenía una `Chapeta` asociada. Decidimos lo contrario: todo `Pesaje` válido se conserva, como `Registro` si se puede asignar a un `Animal` o como `Pesaje pendiente` (respuesta 202) si no. Además, cada pesaje lleva un identificador generado por el ESP32 (`id_lectura`), y recibirlo dos veces no crea nada nuevo. La razón es que una pesada no se puede repetir a voluntad —el animal ya salió de la báscula— y que el ESP32 reintenta cuando no recibe respuesta, así que sin estas dos reglas se pierden pesajes o se duplican.

## Considered Options

- **Mantener el 404 y no guardar el pesaje**: descartada porque el ganadero pierde el dato justo en el caso más común en campo, un animal cuya chapeta todavía no se ha registrado.
- **Crear el `Animal` o la `Chapeta` automáticamente** cuando llega un código desconocido: descartada, igual que en la spec original. Un pesaje nunca crea animales ni chapetas; solo queda pendiente hasta que una persona lo concilie.
- **Confiar en la hora que envíe el ESP32** para los pesajes diferidos: descartada porque su reloj no es confiable. El ESP32 envía cuánto tiempo pasó desde la medición (`antiguedad_ms`) y la app calcula la hora restándolo al momento de recepción.

## Consequences

- El contrato con el firmware cambia: ruta `POST /pesaje`, respuestas 201 y 202 en lugar de 200 y 404, y una tabla explícita de cuándo reintentar. La ruta anterior, `/registro-peso`, se reemplaza de una vez, sin periodo de convivencia.
- Un pesaje para un `Animal` dado de baja deja de crear un `Registro` y pasa a `Pesaje pendiente`.
- `Pesaje pendiente` es una entidad más que guardar y sincronizar.
- Hace falta una forma de conciliar los pendientes; mientras no exista, se acumulan sin que el ganadero los vea.
