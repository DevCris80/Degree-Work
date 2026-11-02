---
status: accepted
---

# Una base de datos por cuenta guardada en el celular

Un celular solo podía tener los datos de una finca: iniciar sesión con otra cuenta, crear otra finca o unirse a una con un código fallaba con "desinstala la app para empezar limpio". Queremos que un mismo celular use cualquier cuenta sin tocar el servidor: una cuenta sigue siendo una sola finca (ADR-0002), y quien trabaja en dos fincas usa dos cuentas en el mismo celular. Decidimos que cada cuenta tenga su propia base Room, aislada de las demás, y que la app abra la del Usuario que acaba de iniciar sesión.

Un registro pequeño, fuera de esas bases, lista las `Cuenta guardada` del celular para la pantalla de inicio de sesión. Al iniciar sesión se autentica contra Supabase Auth, se abre (o se crea) la base de ese `idUsuario` y se reconstruye `AppContainer` y el servidor que recibe los pesajes del ESP32 sobre ella. Cambiar de cuenta es cerrar sesión e iniciar otra, con señal y contraseña. Los celulares que ya tienen una finca adoptan su base actual como la primera cuenta guardada, sin migrar datos.

## Considered Options

- **Una sola base con todas las fincas, cada tabla filtrada por `idPerfilFinca`**: descartado. Casi todos los DAO y los `getOnce()` del repositorio de sesión asumen una finca; habría que tocar cada consulta, escribir una migración de Room y confiar en que ninguna olvida el filtro, y un olvido mostraría datos de otra finca. Con una base por cuenta el aislamiento es estructural.
- **Borrar los datos al cambiar de cuenta**: descartado. Obliga a sincronizar todo antes de cambiar, que no es posible sin señal en el campo, y a descargar la finca entera cada vez que se vuelve a ella.
- **Cuentas recordadas, con las sesiones de varias cuentas guardadas para cambiar sin señal ni contraseña**: descartado por ahora. En un celular compartido, quien lo tenga entraría a todas las fincas guardadas sin contraseña.

## Consequences

- Solo el Usuario del dispositivo sincroniza. Los cambios sin subir de una `Cuenta guardada` esperan hasta que esa cuenta vuelva a iniciar sesión; por eso cerrar sesión avisa de ellos y quitar una cuenta se bloquea mientras tenga cambios, fotos o rechazos por subir.
- Dos Usuarios de la misma finca pueden compartir celular; cada uno tiene su copia de la finca, que sincroniza por separado.
- Los pesajes del ESP32 se guardan a nombre del Usuario del dispositivo, es decir, de la cuenta activa. El ESP32 no manda finca, así que cambiar de cuenta con la báscula conectada manda lo que llegue a la finca que quede abierta.
- Un `Operario quitado`, o una cuenta cuya finca ya no existe en el servidor, pierde su base del celular de inmediato. Ya no hay datos suyos que conservar: lo que capturó no se sube y la finca ya no es suya.
- Cada cuenta ocupa su propio espacio, incluso cuando dos comparten finca.
- Mientras más de una base pueda abrirse, hay que cuidar que ninguna parte del código conserve una referencia a la base de otra cuenta al cambiar. La sincronización lo hace comprobando, antes de cada lote y de cada foto, que su cuenta siga siendo la abierta; una llamada ya en vuelo al cambiar de cuenta no se puede detener.
- Borrar una base espera unos segundos a que la pantalla de la cuenta que sale deje de leerla. Si la app muere en ese lapso, el archivo queda sin dueño y el siguiente arranque lo borra.
- La sesión que la app pierde (el servidor rechazó renovarla) no borra la cuenta: pudo vencer, y los cambios sin subir se perderían. Solo la borra el servidor al negarle la finca.
