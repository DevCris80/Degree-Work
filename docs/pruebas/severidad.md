# Escala de severidad de defectos

Cada defecto que encuentra una prueba (automatizada o demostración) se clasifica con esta escala. La severidad mide el daño que el defecto le hace al usuario y a los datos de la finca; no mide cuánto cuesta arreglarlo. El reporte (`reporte-pruebas.md`) cuenta los defectos por severidad.

## Niveles

| Nivel | Nombre | Criterio | Ejemplos de este proyecto |
|---|---|---|---|
| S1 | Crítica | Se pierde, se duplica o se corrompe un dato de trazabilidad; un usuario ve o escribe datos de otra finca; la app se cierra al abrir o falla de forma que no deja trabajar. | Un pesaje válido que el ESP32 envió y que no queda guardado (RNF-02). Una migración de Room que borra los datos. Una política RLS que deja leer otra finca. |
| S2 | Alta | Un requisito de prioridad M no se cumple y no hay otra forma de lograr el mismo resultado. | El servicio de pesaje se detiene con la pantalla apagada (RNF-03). La subida de cambios nunca termina al recuperar la señal (RF-32). |
| S3 | Media | Un requisito no se cumple pero hay una alternativa razonable, o falla una función de prioridad S, o el resultado es incorrecto sin perder datos. | Una pantalla muestra un conteo desactualizado hasta que se vuelve a abrir. Una foto no sube a la primera y se reintenta sola. |
| S4 | Baja | Defecto de presentación o de texto que no cambia lo que el usuario logra hacer. | Un texto cortado, un margen incorrecto, un mensaje con una errata. |

## Cómo se decide

1. Se empieza por S1 y se baja hasta el primer nivel cuyo criterio se cumple.
2. Si dudan dos niveles, gana el más alto hasta que el equipo lo discuta.
3. Un defecto que solo ocurre en un caso muy raro conserva su severidad; la frecuencia se anota aparte en el campo "Frecuencia" del defecto.
4. Un defecto en una prueba que no es de la app (por ejemplo, el nombre del paquete que comprueba `ExampleInstrumentedTest`) se clasifica por lo que daña: si no daña ninguna función, es S4.

## Qué se registra de cada defecto

| Campo | Contenido |
|---|---|
| ID | `DEF-nn`, en orden de aparición. |
| Severidad | S1 a S4. |
| Dónde apareció | Prueba automatizada (nombre) o ficha de demostración (DM-nn). |
| Descripción | Qué se esperaba y qué pasó. |
| Frecuencia | Siempre, a veces o una sola vez. |
| Requisito afectado | RF o RNF del SRS. |
| Estado | Abierto, corregido (con el issue o PR) o aceptado (con la razón). |

## Cómo se relaciona con el estado del SRS

Un requisito con un defecto abierto S1 o S2 no pasa a "Verificado" en la matriz de trazabilidad (SRS, sección 4). Con un defecto abierto S3 o S4 puede pasar, siempre que el defecto quede en el reporte.
