# Pruebas

Evidencia de verificación de MiGanado para el SRS (sección 4) y la tesis.

| Qué | Dónde |
|---|---|
| Resultado de las suites automatizadas, defectos por severidad y estado de las demostraciones | `reporte-pruebas.md` |
| Escala de severidad de defectos | `severidad.md` |
| Fichas de las demostraciones DM-01 a DM-13: pasos, resultado esperado, criterio y registro de la ejecución | `fichas/` |
| Scripts que miden DM-04 y DM-10 a DM-13, y sus resultados | `scripts/` y `scripts/resultados/` |
| Videos de las demostraciones | `fichas/DM-nn.mp4` |
| Reportes crudos (XML de JUnit y salida de pgTAP) | `reportes/` |

## Correr los scripts

Los scripts de `scripts/` hablan con la app por wifi, igual que el ESP32, y usan `adb` para leer el estado del celular y copiar su base de datos. Necesitan una app de depuración, porque la base se copia con `run-as`. Se pueden correr contra la app que ya está en el celular (`PAQUETE=com.devcris80.prototipo`, como se hizo el 2026-10-29) o contra una copia aparte, para no tocar los datos de la app real. Esa copia lleva el sufijo `.prueba` en su `applicationId`, que no se sube al repositorio:

```kotlin
// AppBovina/app/build.gradle.kts, dentro de buildTypes.
debug { applicationIdSuffix = ".prueba" }
```

Variables que aceptan: `PAQUETE` (por defecto `com.devcris80.prototipo.prueba`; con la app real, `com.devcris80.prototipo`), `IP` del celular, `SERIAL` del celular para `adb` y `ADB` con la ruta de `adb`. Si `adb devices` lista el mismo celular dos veces por wifi, `SERIAL` evita el error «more than one device».

Cada corrida deja su carpeta en `scripts/resultados/` con el resumen, el detalle de los envíos y la copia de la base. Las copias de las bases no se versionan.
