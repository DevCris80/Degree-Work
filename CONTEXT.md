# Trazabilidad Bovina

Sistema de trazabilidad de ganado bovino, offline-first, compuesto por una app Android nativa y un dispositivo ESP32 externo que reporta pesajes vía HTTP.

## Language

**Animal**:
Un bovino individual registrado en el sistema, identificado por un `id_animal` (UUID).

**Chapeta**:
El identificador físico colocado en un `Animal`, identificado por su `codigo` (el UID del tag NFC, en mayúsculas y sin separadores; normalmente hexadecimal, pero no se exige). Está **activa** mientras no tiene fecha de desasociación; un mismo `codigo` puede tener varias Chapetas en el tiempo, pero solo una activa a la vez. Se **libera** (se desasocia) explícitamente desde el Detalle de un `Animal`; liberarla nunca ocurre en silencio.
_Avoid_: chip, etiqueta, id_chip ("chip" sobrevive solo en el contrato con el ESP32: el campo `id_chip` y el motivo `CHIP_NO_ASOCIADO`)

**Evento**:
Un suceso clínico o de manejo asociado a un `Animal` (vacunación, tratamiento, parto, inseminación), con fecha y detalle.
_Avoid_: Registro (reservado exclusivamente para pesajes)

**Pesaje**:
La medición (código y peso) que reporta el ESP32 cada vez que un bovino pasa por la báscula. Todo Pesaje válido se conserva: termina como `Registro` o como `Pesaje pendiente`.
_Avoid_: lectura (reservada para la lectura NFC de una `Chapeta` en Escanear), medición

**Registro**:
Un `Pesaje` ya asignado a un `Animal` que no está dado de baja: solo, porque su código tenía una `Chapeta` activa de ese `Animal`, o por una persona al conciliar un `Pesaje pendiente`. También lo crea una persona que digita un `Pesaje manual`. El sustantivo queda reservado para pesajes; el verbo *registrar* sí se usa para dar de alta un `Animal` ("registrar un animal").
_Avoid_: Evento, medición; "Registro" como nombre de la creación de un `Animal` o de una cuenta

**Pesaje manual**:
Un `Registro` que una persona digita en el celular, sin pasar por el ESP32: no tiene `id_lectura`, queda a nombre del `Usuario` del dispositivo y lleva la fecha y hora del pesaje que se ingresó, que no puede ser futura.
_Avoid_: medición manual

**Pesaje pendiente**:
Un `Pesaje` válido que no pudo asignarse a un `Animal`, porque su código no tiene `Chapeta` activa o porque el `Animal` está dado de baja. Está sin resolver hasta que alguien lo concilia; resuelto, se conserva igual, con su motivo.
_Avoid_: lectura huérfana, cuarentena (en ganadería es un término sanitario)

**Conciliar**:
Resolver un `Pesaje pendiente`: una persona lo **asigna** a un `Animal`, lo que crea el `Registro`, o lo **descarta**. El `Animal` lo elige la persona y no tiene que ser el de la `Chapeta` con ese código. Es definitivo.

**Descartar**:
Conciliar un `Pesaje pendiente` sin asignarlo, porque no corresponde a ningún `Animal`. Lo decide una persona, y el pendiente se conserva como descartado.
_Avoid_: borrar, eliminar; "descartar" para un pesaje que el sistema rechaza o pierde

**sincronizado**:
Campo booleano presente en toda entidad: `false` significa que la fila tiene cambios locales que todavía no se han subido a Supabase. Toda creación o modificación lo devuelve a `false` y actualiza `fechaModificacion`, la fecha del último cambio de la fila. `fechaModificacion` informa cuándo cambió la fila, pero nunca decide un `Conflicto`. Vuelve a `true` cuando la subida aplica la respuesta del servidor para esa fila, si la fila no cambió mientras subía.

**Revisión**:
Cada versión de un `Animal`, `Evento` o `Registro` que el servidor aceptó, numerada en orden. Toda edición parte de una Revisión (la última que conocía el celular que la hizo); si esa sigue siendo la vigente al sincronizar, la edición se acepta como la Revisión siguiente. Las Revisiones se conservan siempre.
_Avoid_: versión (a secas), historial

**Sibling**:
Una edición que llegó al servidor partiendo de una `Revisión` que ya no era la vigente. No se descarta ni se fusiona: se guarda junto a la Revisión vigente y deja la fila en `Conflicto`. Cuenta la fila entera, aunque los campos editados no se pisen. Se conserva aun después de resolverse.
_Avoid_: copia, duplicado, versión perdedora

**Conflicto**:
El estado de una fila que tiene uno o más `Sibling` sin resolver. La fila se sigue consultando con su `Revisión` vigente, marcada "en conflicto". Solo lo cierra una persona al **resolverlo**, eligiendo qué queda; editar la fila sin resolverlo no lo cierra. Cualquier rol puede resolverlo.

**Rechazo**:
Un cambio local que el servidor no aceptó porque viola una regla de unicidad o de permisos (por ejemplo, un código con otra `Chapeta` activa, o un `Pesaje pendiente` ya conciliado en otro celular). No se pierde: queda a la vista hasta que una persona lo corrige.
_Avoid_: descartar (reservado para `Pesaje pendiente`), error, `Conflicto` (un Conflicto es una edición concurrente válida; un Rechazo viola una regla)

**Dar de baja**:
Marcar un `Animal`, `Evento` o `Registro` como retirado: deja de aparecer en las listas, pero se conserva. Una baja nunca queda como `Sibling`: se aplica aunque la fila haya cambiado en otro celular.
_Avoid_: borrar, eliminar

**Usuario**:
Una persona que opera la app, identificada por `idUsuario` y asociada a un `PerfilFinca` mediante `idPerfilFinca`; tiene un `rol` que distingue su función. Un mismo `PerfilFinca` puede tener varios Usuarios, cada uno con la app en su propio dispositivo; un Usuario pertenece a una sola finca. Su `rol` es **Ganadero** u **Operario**.
_Avoid_: Ganadero como nombre de la entidad de datos (es un `rol`, no la entidad)

**Ganadero**:
El `rol` del `Usuario` responsable de la finca: puede hacer todo, incluido dar de baja un `Animal`, liberar una `Chapeta`, descartar un `Pesaje pendiente` e invitar o quitar Usuarios.
_Avoid_: Administrador, dueño

**Operario**:
El `rol` del `Usuario` que trabaja con el ganado: puede ver animales, escanear, registrar `Animal` y `Evento` y asignar un `Pesaje pendiente`, pero no dar de baja, liberar una `Chapeta`, descartar un `Pesaje pendiente` ni gestionar Usuarios.

**PerfilFinca**:
La finca a la que pertenecen un `Usuario` y sus `Animal`, identificada por `idPerfilFinca`, con `nombreFinca`, `latitud` y `longitud`. No incluye el nombre del usuario — ese dato vive en `Usuario`.
_Avoid_: guardar identidad de usuario en esta entidad

**Crear cuenta**:
El acto por el que una persona se convierte en `Usuario` de la app.
_Avoid_: Registro, registrarse
