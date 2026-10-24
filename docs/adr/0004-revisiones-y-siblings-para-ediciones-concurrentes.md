---
status: accepted
---

# Revisiones y siblings para las ediciones concurrentes, sin fusión automática

Varios Usuarios de una finca editan los mismos datos desde celulares que pasan horas sin señal, así que dos ediciones de la misma fila hechas a la vez son el caso normal, no la excepción. Decidimos que el servidor numere cada versión aceptada de un `Animal`, `Evento` o `Registro` (`rev`) y que cada edición viaje con la revisión de la que partió (`rev_base`). Si `rev_base` es la vigente, la edición se acepta como `rev + 1`; si no, se guarda como **sibling** de la vigente y la fila queda **en conflicto** hasta que una persona lo resuelve. Nada se fusiona solo y nada se descarta: las revisiones y los siblings se conservan siempre, como exige la Ley 1659.

## Reglas

1. **El sibling es la fila entera.** Dos ediciones desde la misma revisión hacen sibling aunque toquen campos distintos.
2. **Reenviar no duplica.** Cada escritura local genera un `id_version` (UUID); varias ediciones locales antes de sincronizar se funden en una sola, que conserva el `rev_base` original. Si llega un `id_version` que el servidor ya procesó, responde lo mismo que la primera vez.
3. **El celular que pierde no pierde su edición.** La respuesta "sibling" trae la revisión vigente: el celular reemplaza su fila con ella y guarda su edición como sibling local. El celular guarda solo la vigente y los siblings sin resolver; el historial completo vive en el servidor.
4. **La baja nunca es sibling.** `fechaBaja` (y `fechaDesasociacion` de la Chapeta) se escribe una sola vez y el servidor la aplica sobre la revisión vigente aunque `rev_base` sea viejo. Si la misma edición cambió otros campos, solo esos pueden quedar como sibling.
5. **Solo una resolución explícita cierra el conflicto.** La resolución es una edición nueva sobre la vigente que nombra los `id_version` de los siblings que resuelve. Editar la fila sin resolver acepta la revisión siguiente y deja el conflicto abierto; un sibling que llega después de abrir la resolución queda sin resolver.
6. **Sin revisiones:** `Chapeta` y `Pesaje pendiente` solo se insertan y cambian de estado una vez (liberar, conciliar), y lo que choque es un rechazo por unicidad. `PerfilFinca` tiene un único escritor (el Ganadero) y se sube sin revisión. `Usuario` solo lo cambian funciones del servidor.
7. **Un rechazo también deja al celular con la verdad del servidor.** El cambio rechazado pasa, completo y con su motivo, a una tabla local de rechazos, de donde una persona lo corrige; mientras siga abierto no se reenvía. Si el servidor tiene la fila (se rechazó una modificación, como la baja que sube un Operario), la fila local vuelve a la versión del servidor. Si no la tiene (se rechazó una inserción), la fila sale de su tabla, y con ella las que dependen de ella: las Chapetas, Eventos, Registros y crías de un Animal, cada una con su propio rechazo, porque sin su Animal en el servidor tampoco pueden subir y el `CASCADE` de Room las borraría en silencio. `PerfilFinca` nunca sale de su tabla. Sin esto, la fila rechazada choca con los índices únicos locales al descargar la versión del servidor (por ejemplo, dos Chapetas activas con el mismo código).

## Considered Options

- **Gana la `fechaModificacion` más reciente** (la regla que planteaba #46): descartada porque depende del reloj de cada celular y pierde en silencio la edición más vieja.
- **Relojes vectoriales**: descartados. Detectan la concurrencia sin servidor, pero aquí siempre hay un servidor que ordena las escrituras, y un contador por fila basta para saber si una edición partió de la vigente. Cuestan un vector por fila y una explicación que la tesis no necesita.
- **Fusión automática por campo**: descartada. Fusionar raza y nombre de dos personas produce una versión que nadie escribió, y decidir qué campos "no chocan" es una regla de negocio por entidad.

## Consequences

- `Animal`, `Evento` y `Registro` ganan en Room `rev`, `rev_base` e `id_version`, y Room gana una tabla de siblings sin resolver.
- Hace falta una pantalla para ver y resolver conflictos, y la lista y el detalle muestran la marca "en conflicto".
- Una baja hecha en un celular siempre gana a una edición concurrente; lo que la otra persona cambió sobrevive como sibling solo si tocó otros campos.
