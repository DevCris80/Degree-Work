# Trazabilidad Bovina

Sistema de trazabilidad de ganado bovino, offline-first, compuesto por una app Android nativa y un dispositivo ESP32 externo que reporta pesajes vía HTTP.

## Language

**Animal**:
Un bovino individual registrado en el sistema, identificado por un `id_animal` (UUID).

**Chapeta**:
El identificador físico colocado en un `Animal`, identificado por su `codigo` (el UID del tag NFC, en mayúsculas y sin separadores; normalmente hexadecimal, pero no se exige). Está **activa** mientras no tiene fecha de desasociación; un mismo `codigo` puede tener varias Chapetas en el tiempo, pero solo una activa a la vez. Se **libera** (se desasocia) explícitamente desde el Detalle de un `Animal`; liberarla nunca ocurre en silencio.
_Avoid_: chip, etiqueta, id_chip (este último sobrevive solo como nombre de campo en el mensaje que envía el ESP32)

**Evento**:
Un suceso clínico o de manejo asociado a un `Animal` (vacuna, tratamiento, nacimiento, inseminación), con fecha y detalle.
_Avoid_: Registro (reservado exclusivamente para pesajes)

**Pesaje**:
La medición (código y peso) que reporta el ESP32 cada vez que un bovino pasa por la báscula. Todo Pesaje válido se conserva: termina como `Registro` o como `Pesaje pendiente`.
_Avoid_: lectura (reservada para la lectura NFC de una `Chapeta` en Escanear), medición

**Registro**:
Un `Pesaje` ya asignado a un `Animal`: su código tenía una `Chapeta` activa y el `Animal` no estaba dado de baja. El sustantivo queda reservado para pesajes; el verbo *registrar* sí se usa para dar de alta un `Animal` ("registrar un animal").
_Avoid_: Evento, medición; "Registro" como nombre de la creación de un `Animal` o de una cuenta

**Pesaje pendiente**:
Un `Pesaje` válido que no pudo asignarse a un `Animal`, porque su código no tiene `Chapeta` activa o porque el `Animal` está dado de baja. Se conserva con su motivo hasta que alguien lo concilie.
_Avoid_: lectura huérfana, cuarentena (en ganadería es un término sanitario)

**sincronizado**:
Campo booleano presente en toda entidad: `false` significa que la fila tiene cambios locales que todavía no se han subido a Supabase. Toda creación o modificación lo devuelve a `false` y actualiza `fechaModificacion`, la fecha del último cambio de la fila. La sincronización en sí todavía no está implementada.

**Usuario**:
Una persona que opera la app, identificada por `idUsuario` y asociada a un `PerfilFinca` mediante `idPerfilFinca`; tiene un `rol` que distingue su función. Un mismo `PerfilFinca` puede tener varios Usuarios, cada uno con la app en su propio dispositivo; un Usuario pertenece a una sola finca. Su `rol` es **Ganadero** u **Operario**.
_Avoid_: Ganadero como nombre de la entidad de datos (es un `rol`, no la entidad)

**Ganadero**:
El `rol` del `Usuario` responsable de la finca: puede hacer todo, incluido dar de baja un `Animal`, liberar una `Chapeta` e invitar o quitar Usuarios.
_Avoid_: Administrador, dueño

**Operario**:
El `rol` del `Usuario` que trabaja con el ganado: puede ver animales, escanear y registrar `Animal` y `Evento`, pero no dar de baja, liberar una `Chapeta` ni gestionar Usuarios.

**PerfilFinca**:
La finca a la que pertenecen un `Usuario` y sus `Animal`, identificada por `idPerfilFinca`, con `nombreFinca`, `latitud` y `longitud`. No incluye el nombre del usuario — ese dato vive en `Usuario`.
_Avoid_: guardar identidad de usuario en esta entidad

**Crear cuenta**:
El acto por el que una persona se convierte en `Usuario` de la app.
_Avoid_: Registro, registrarse
