# Reporte de pruebas

Resultado de las pruebas automatizadas de MiGanado y estado de las demostraciones registradas. Es la evidencia de la columna "Verificación" del SRS (sección 4) y del requisito RNF-07. La escala de severidad está en `severidad.md`.

| Dato | Valor |
|---|---|
| Fecha de las corridas | 2026-10-29 |
| Código probado | `main` en el commit `1b68371` (PR #134). Las pruebas JVM y de instrumentación corrieron en una copia limpia de ese commit; pgTAP corrió con `supabase/` de la rama `docs`, idéntico al de `main` |
| Celular | Samsung Galaxy A15 (SM-A155M), Android 16, con NFC |
| Máquina de las pruebas JVM y de base de datos | Linux (Fedora), Gradle 9.6.0; Postgres local con Podman |

## 1. Suites automatizadas

| Suite | Dónde corre | Pruebas | Pasan | Fallan | Reporte |
|---|---|---|---|---|---|
| JVM, módulo `:domain` | Máquina de desarrollo | 105 | 105 | 0 | `reportes/jvm-domain/` |
| JVM, módulo `:app` | Máquina de desarrollo | 190 | 190 | 0 | `reportes/jvm-app/` |
| Instrumentación | Galaxy A15 | 108 | 107 | 1 (DEF-01) | `reportes/instrumentacion/` |
| pgTAP | Postgres local (Podman) | 150 | 150 | 0 | `reportes/pgtap.txt` |
| **Total** | | **553** | **552** | **1** | |

Comandos, desde la raíz del repositorio:

```sh
cd AppBovina && ./gradlew cleanTest test                       # JVM
cd AppBovina && ANDROID_SERIAL=<celular> ./gradlew :app:connectedDebugAndroidTest   # instrumentación
npx supabase@2.122.0 db reset && npx supabase@2.122.0 test db  # pgTAP, con DOCKER_HOST apuntando a Podman
```

La instrumentación corrió sobre una copia de la app con `applicationIdSuffix = ".prueba"`, para que el comando no instalara encima ni desinstalara la app con los datos reales del celular. Ese sufijo no está en el repositorio.

### 1.1 JVM, `:domain` (105)

`RegistrarPesajeTest` 13, `RegistrarEventoTest` 12, `ObservarBandejaRechazosTest` 11, `RegistrarAnimalTest` 8, `AsignarPesajesPendientesTest` 6, `CrearCuentaTest` 6, `DescartarPesajePendienteTest` 6, `ObservarIndicadoresHatoTest` 6, `ActualizarAnimalTest` 5, `RegistrarPesajeManualTest` 5, `ResolverConflictoTest` 5, `ObservarEvolucionPesoTest` 4, `ResolverDestinoEscaneoTest` 4, `AnioMesTest` 3, `DarDeBajaAnimalTest` 3, `EventoTest` 3, `LiberarChapetaTest` 3, `ObservarPesajesPendientesTest` 2.

### 1.2 JVM, `:app` (190)

`PesajesPendientesViewModelTest` 14, `RegistrarAnimalViewModelTest` 14, `LoginViewModelTest` 13, `RegistrarUbicacionFincaViewModelTest` 13, `IndicadoresViewModelTest` 12, `EditarAnimalViewModelTest` 11, `ListaAnimalesViewModelTest` 11, `NuevoEventoViewModelTest` 10, `DetalleAnimalViewModelTest` 9, `CrearCuentaViewModelTest` 8, `EscanearViewModelTest` 8, `NuevoPesajeViewModelTest` 8, `GraficoEventosTest` 8, `GraficoPesoTest` 8, `MappersEventoTest` 6, `ResolverConflictoViewModelTest` 6, `EventosViewModelTest` 5, `MappersPerfilFincaTest` 5, `GraficoInventarioTest` 5, `InicioViewModelTest` 4, `DisparosDeSincronizacionTest` 3, `EdadTest` 3, `FechaHoraTest` 3, `BandejaRechazosViewModelTest` 2, `ExampleUnitTest` 1.

### 1.3 Instrumentación en el A15 (108)

`MotorSincronizacionTest` 17, `MotorSincronizacionFotosTest` 12, `RoomConflictoRepositoryTest` 12, `MotorSincronizacionDescargaTest` 11, `MigracionesTest` 10, `RoomPesajePendienteRepositoryTest` 7, `FotosEnArchivosTest` 6, `RoomRechazoRepositoryTest` 6, `EscriturasLocalesTest` 5, `RevisionesLocalesTest` 4, `RoomChapetaRepositoryTest` 4, `RoomCuentaRepositoryTest` 4, `PesajeHttpServerIntegracionTest` 4, `RoomEventoRepositoryTest` 2, `BandejaRechazosE07Test` 1, `ExampleInstrumentedTest` 1 (falla), `RoomAnimalRepositoryTest` 1, `RoomRegistroRepositoryTest` 1.

### 1.4 pgTAP (150)

| Archivo | Pruebas | Qué comprueba |
|---|---|---|
| `01_aislamiento_por_finca` | 13 | Un usuario no ve ni escribe datos de otra finca (RLS). |
| `02_permisos_por_rol` | 28 | Lo que cada rol puede hacer. |
| `03_reglas_de_las_tablas` | 23 | Restricciones de las tablas y unicidad de la chapeta activa. |
| `04_subir_cambios` | 34 | Aceptada, sibling y rechazada en `subir_cambios`; idempotencia por `id_version`. |
| `05_descargar_cambios` | 18 | Descarga por cursor y páginas. |
| `06_crear_finca` | 19 | Alta de la cuenta, la finca y la aceptación de datos. |
| `07_fotos` | 15 | Políticas de las fotos en Storage. |

## 2. Defectos por severidad

| Severidad | Abiertos | Corregidos | Aceptados |
|---|---|---|---|
| S1 Crítica | 0 | 0 | 0 |
| S2 Alta | 0 | 0 | 0 |
| S3 Media | 0 | 0 | 0 |
| S4 Baja | 1 | 0 | 0 |

| ID | Sev. | Dónde apareció | Descripción | Frecuencia | Requisito | Estado |
|---|---|---|---|---|---|---|
| DEF-01 | S4 | `ExampleInstrumentedTest.useAppContext` | La prueba compara el nombre del paquete con `com.devcris80.prototipo` fijo. Falla en cualquier corrida cuyo `applicationId` sea otro, como la copia de prueba `.prueba` que protege los datos del celular. Ninguna función de la app falla. | Siempre, con otro `applicationId` | RNF-07 | Abierto: se arregla leyendo el paquete esperado de `BuildConfig.APPLICATION_ID`, o borrando la plantilla. |

Las demostraciones ejecutadas (sección 3) no produjeron defectos. Los que salgan de las que faltan se agregan aquí con el mismo formato.

**Observación sin clasificar.** Entre DM-11 y la segunda corrida de DM-12 el servicio de pesaje dejó de escuchar en el puerto 8080 y no volvió a arrancar solo; hubo que iniciarlo desde la pantalla de Pesos. El celular había pasado por la vista de aplicaciones recientes. No hay log de la causa ni se reprodujo, así que no se registra como defecto. Si se confirma que la app puede perder el servicio sin que el usuario lo detenga, sería S2 por RNF-03 (el servicio es de inicio manual y el servicio en primer plano no se reinicia si el sistema lo mata en segundo plano). Conviene reproducirlo.

## 3. Demostraciones registradas

Cada ficha trae los pasos, el resultado esperado, el criterio de aprobación y el lugar para el video. Los scripts de medición están en `scripts/`.

| Ficha | Demostración | Requisitos | Cómo se ejecuta | Estado |
|---|---|---|---|---|
| [DM-01](fichas/DM-01.md) | Lectura del UID de una chapeta NFC | RF-15 | Manual, con chapetas | Pendiente |
| [DM-02](fichas/DM-02.md) | Destino de un código escaneado | RF-16, RF-17 | Manual, código digitado | Pendiente |
| [DM-03](fichas/DM-03.md) | Iniciar y detener el servicio de pesaje | RF-20, RF-25 | `adb` y `curl` | Aprobada, con video |
| [DM-04](fichas/DM-04.md) | Jornada de pesaje sin internet (DE-01) | RF-20 a RF-24, RNF-01 | `scripts/dm04-jornada-de-pesaje.sh` | Pasos 1 a 5 aprobados, con video; falta repetir sin internet |
| [DM-05](fichas/DM-05.md) | Subida de cambios al reconectar (DS-01) | RF-32, RF-33, RF-35 | Manual, con Supabase | Pendiente |
| [DM-06](fichas/DM-06.md) | Descarga en un segundo celular (DS-02) | RF-36, RF-38, RNF-09 | `scripts/dm06-descarga-en-segundo-celular.sh`, dos celulares | Aprobada: bases iguales fila por fila (307 registros), con video |
| [DM-07](fichas/DM-07.md) | Edición concurrente del mismo animal (DE-02) | RF-12, RF-33, RF-34 | Manual, dos celulares | Pendiente |
| [DM-08](fichas/DM-08.md) | Mismo código en dos celulares sin señal (DE-03) | RF-17, RF-35 | Manual, dos celulares | Pendiente |
| [DM-09](fichas/DM-09.md) | Funciones de registro y consulta en modo avión | RNF-01 | Manual con `adb` | Pendiente |
| [DM-10](fichas/DM-10.md) | Integridad de la sincronización con al menos 100 filas | RNF-08 | Script y dos celulares | Pendiente |
| [DM-11](fichas/DM-11.md) | Cada pesaje válido se guarda exactamente una vez | RNF-02 | `scripts/dm11-una-sola-vez.sh` | Aprobada (60 de 60 envíos únicos en 2 corridas con chapeta y 10 de 10 sin ella), con video |
| [DM-12](fichas/DM-12.md) | Tiempo hasta que un pesaje es visible | RNF-05 | `scripts/dm12-tiempo-de-pesaje.sh` | Aprobada: 30 de 30, máximo 2,6 s, con video |
| [DM-13](fichas/DM-13.md) | Servicio de pesaje con la pantalla apagada | RNF-03 | `scripts/dm13-pantalla-apagada.sh` | Aprobada: 15 min, 30 de 30 |

Resumen de lo ejecutado: **DM-03, DM-06, DM-11, DM-12 y DM-13 aprobadas**; **DM-04 aprobada en los pasos funcionales**, pendiente de repetir sin internet. Todas corrieron en el A15 el 2026-10-29, con la sesión que ya tenía el celular y con el build de depuración del 2026-10-27 (esquema Room 17), anterior a `main`: el código del servidor de pesajes es el mismo, pero esa app no tenía los campos nuevos del evento ni los gráficos. Después se actualizó el A15 a `main` (esquema 18, sin perder datos) para las demostraciones con dos celulares. El segundo celular es un Oppo A5 5G (CPH2735, Android 16, con NFC), no el Xiaomi 14T que nombra el SRS; cada celular usa un usuario distinto de la misma finca. Los videos están junto a las fichas (`fichas/DM-03.mp4`, `DM-04.mp4`, `DM-06.mp4`, `DM-11.mp4` y `DM-12.mp4`). Faltan DM-01 y DM-02 (necesitan una chapeta NFC), DM-05, DM-07, DM-08, DM-09 y DM-10.

## 4. Lo que estas pruebas no cubren

- Ninguna prueba automatizada toca el lector NFC, el servicio en primer plano con la pantalla apagada ni la sincronización entre dos celulares reales; eso lo cubren las demostraciones.
- La instrumentación usa un servidor falso (`ServidorFalso`); la sincronización contra el proyecto Supabase real la cubren DM-05, DM-06 y DM-10.
- pgTAP corrió en una base local con las migraciones de `supabase/migrations/`. La corrida contra el proyecto en la nube (`test db --linked`) no se hizo.
- El A15 declara NFC (`android.hardware.nfc`), lo que cierra la duda de SUP-05 para ese celular; la lectura real de una chapeta queda para DM-01.
