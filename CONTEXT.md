# Trazabilidad Bovina

Sistema de trazabilidad de ganado bovino, offline-first, compuesto por una app Android nativa y un dispositivo ESP32 externo que reporta pesajes vía HTTP.

## Language

**Animal**:
Un bovino individual registrado en el sistema, identificado por un `id_animal` (UUID).

**Chapeta**:
El identificador físico (chip/etiqueta) colocado en un `Animal`. Asocia un `id_chip` (UID del chip físico) con un `id_animal`, con fecha de asociación y, opcionalmente, de desasociación. En esta iteración el UID se precarga manualmente; la lectura NFC real queda fuera de alcance.
_Avoid_: Tag, chip, etiqueta

**Evento**:
Un suceso clínico o de manejo asociado a un `Animal` (vacuna, tratamiento, nacimiento, inseminación), con fecha y detalle.
_Avoid_: Registro (reservado exclusivamente para pesajes)

**Registro**:
Un pesaje de un `Animal`, recibido vía HTTP desde el ESP32 y resuelto contra una `Chapeta` para determinar a qué `Animal` pertenece.
_Avoid_: Evento, medición

**sincronizado**:
Campo booleano presente en toda entidad, reservado para una futura sincronización con Supabase. No se implementa en esta iteración — existe únicamente para evitar una migración de esquema posterior.

**Usuario**:
La persona que opera la app, identificada por `idUsuario` y asociada a un `PerfilFinca` mediante `idPerfilFinca`; tiene un `rol` que distingue su función. Definido en el diagrama ER (`docs/diagramas/diagrama-relaciones-trazabilidad-bovina.md`); el código actual todavía no lo separa de `PerfilFinca`.
_Avoid_: Ganadero (como nombre de entidad de datos — sigue siendo el término de cara al usuario en la UI)

**PerfilFinca**:
La finca a la que pertenecen un `Usuario` y sus `Animal`, identificada por `idPerfilFinca`, con `nombreFinca`, `latitud` y `longitud`. No incluye el nombre del usuario — ese dato vive en `Usuario`. Definido en el diagrama ER; el código actual (`PerfilFinca.kt`) todavía guarda `nombreGanadero` aquí, pendiente de separarse.
_Avoid_: guardar identidad de usuario en esta entidad
