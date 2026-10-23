# Prompt de rediseño visual — MiGanado

Contexto: Quiero aplicar un rediseño VISUAL a las pantallas que YA están implementadas. Las referencias de diseño están en docs/pantallasdiseño (cada carpeta tiene screen.png y code.html; el sistema de diseño está en verdant_acre/DESIGN.md). Los PNG son la referencia visual; el HTML sirve para sacar medidas, radios y colores exactos. No uses el HTML como código: hay que reescribirlo en Compose. Vamos pantalla por pantalla

## ALCANCE ESTRICTO
- Solo capa de presentación (composables, theme, recursos). NO tocar: entidades, DAOs, repositorios, ViewModels (lógica), rutas de navegación, NFC, sincronización, permisos ni ninguna función existente.
- Si una pantalla del diseño NO existe en la app, debemos crear la parte visual por el momento.
- Las pantallas del diseño son un mockup con datos de ejemplo. NUNCA copies sus datos (Margarita, #042, Potrero, 520 kg, etc.). Usa los datos reales de la app.
- Si la app tiene un campo, botón o función que el diseño no muestra, se CONSERVA y se restiliza con el mismo lenguaje visual. Que no esté en el diseño no es motivo para quitarlo.
- Si el diseño muestra algo que la app no tiene (dato, función o estado), NO lo inventes ni lo dejes con texto fijo: omítelo y anótalo en el reporte final.

## PASO 0 — Inventario (no edites nada todavía)
1. Lista las pantallas (composables Screen/Content) que existen hoy y el archivo de cada una.
2. Haz una tabla: pantalla de la app ↔ carpeta de diseño correspondiente. Mapa esperado: Registro→1.registro, Login→2.iniciosesion, Lista de animales→3.pantallaprincipal, Registrar animal→4.registraranimal (si existe), Detalle→5.detalleanimal, Escanear→6.escanearchip (si existe), Eventos→7.eventos (solo la lista de eventos ya registrados), Pesos→9.pesos.
3. Para cada pantalla, indica cuáles elementos del diseño no tienen respaldo en la app (según las reglas de arriba).
4. Muéstrame esa tabla y espera mi OK antes de continuar. Solo pregunta si algo es realmente ambiguo.

## PASO 1 — Rama y baseline
- Crea la rama `rediseno-ui` y el tag `pre-rediseno` sobre el estado actual.
- Verifica que `./gradlew assembleDebug` compila antes de cambiar nada.

## PASO 2 — Theme y componentes compartidos (un commit)
- Tema Material3 con los tokens de verdant_acre/DESIGN.md (colores light, tipografía, shapes). Un solo tema, sin colores hardcodeados en las pantallas.
- Tipografía: Plus Jakarta Sans en TODAS las pantallas (aunque algunos HTML usen Nunito Sans o Commissioner, el sistema de diseño manda). Descarga los .ttf y empaquétalos en res/font (la app es offline-first: no usar Google Fonts descargables).
- Componentes reutilizables con @Preview: botón primario (píldora, degradado #2E5E22→#71B433, 52dp), botón secundario, campo de texto (píldora, 52dp), chips de filtro, badge/status pill, tarjeta, TopBar y barra de navegación inferior (Animales, Escanear, Eventos, Pesos; pestaña activa en píldora con ícono y etiqueta, igual en todas las pantallas).
- Íconos: Material Icons de Compose (o vector assets locales). Nada de URLs externas: el logo y cualquier imagen del diseño se exportan a res/drawable. Las fotos de animales usan el fotoUri real con el cargador de imágenes que ya usa la app y un placeholder local cuando no hay foto.

## PASO 3 — Una pantalla por commit
Para cada pantalla mapeada: replica el layout, espaciados, radios, jerarquía tipográfica y colores del PNG. Mantén la estructura actual. Compila (`./gradlew assembleDebug`) y corre los tests existentes después de cada pantalla. Si un test o compilación falla, arréglalo antes de seguir.

### Reglas por pantalla
- PRIMORDIAL: mantener la funcionalidad de las pantallas que actualmente ya tenemos
- Conserva TODOS los campos que el registro actual ya pide (por ejemplo nombre de finca/rol) aunque el diseño no los muestre.
- Lista de animales: omitir clima, foto de persona (usa iniciales o nada), "Sincronizado" si no se calcula de un estado real, y las métricas L/día, "Preñada", "Parto: Nov", Potrero/Lote y estados como "En ordeño", "Padrón", "Levante/Reemplazo". En cada tarjeta mostrar solo datos reales (nombre, raza, sexo, chapeta/código, último peso, y GMD solo si ya se calcula). Los contadores de las categorías (Lecheras/Novillos/Crías) solo si salen de datos reales existentes.
- Detalle de animal: solo campos reales. Omitir "Condición corporal", producción de leche, Potrero/Lote y metas ("En meta esperada"). Conservar cualquier acción actual (editar, dar de baja, historial de pesos/eventos).
- Eventos: restilizar únicamente la lista de eventos ya registrados. Omitir "Programar Nuevo Evento", "Reasignar", estados Pendiente/Programado/Confirmado, el aviso ICA/fitosanitario, "FPP", conteos de pendientes y cualquier agenda.
- Pesos: restilizar la captura (ESP32/manual) y la lista de pesajes que ya existen. Omitir "Óptimo para venta", "Días restantes proyectados", "Superó curva…", "Condición corporal", "Producción L/día", Lote/Potrero, metas y GMD si hoy no se calcula. Conservar el texto que la app ya usa sobre el origen del peso (simulado/real); no escribir marcas de básculas.
- Escanear: restilizar sin tocar la lógica NFC ni los estados que ya maneja. Omitir el botón "Escanear chip ahora" si la lectura ya es automática al abrir la pantalla, y el texto "5 cm" / "arete". Los banners de estado (internet/NFC) solo si reflejan estado real.
- Registrar animal: conserva los campos actuales; omitir Lote/Potrero, Peso inicial y el botón de QR si hoy no existen. El UID/chapeta debe seguir comportándose igual que hoy.

## PASO 4 — Reporte final
Entrégame: (a) tabla pantalla → archivos modificados → commit, (b) lista de elementos del diseño que omitiste por no tener respaldo, (c) cualquier diferencia visual que no pudiste replicar y por qué, (d) confirmación de que no se modificó ninguna entidad, DAO, repositorio, ViewModel ni ruta. Los cambios en capa de datos son un fallo: si crees que alguno hace falta, repórtalo en vez de hacerlo.
