---
status: accepted
---

# Capas con dominio puro y capa de datos offline-first (no Clean Architecture completa)

AppBovina se organiza en tres capas, `ui → dominio ← data`: la UI (Compose + un ViewModel por pantalla) y la capa de datos dependen del dominio, y el dominio no depende de nadie. El dominio vive en un módulo Gradle `:domain` de Kotlin puro (sin Android ni Room) con los modelos, las interfaces de repositorio y los casos de uso; `data` y `ui` son paquetes de `:app`. Lo elegimos para poder testear las reglas en JVM sin emulador y para que la sincronización con Supabase se enchufe en `data` sin tocar la UI.

Offline-first se sostiene con tres reglas de la capa de datos:

1. **Room es la única fuente de verdad.** La UI lee solo de Room (vía repositorios, con `Flow`); nunca de la red.
2. **Toda escritura pasa por un repositorio**, que guarda local, pone `sincronizado = false` y actualiza `fechaModificacion`. Nadie fuera de `data` toca un DAO.
3. **La sincronización es un proceso aparte dentro de `data`.** Ni la UI ni el dominio saben que existe.

## Considered Options

- **Clean Architecture completa** (caso de uso por cada operación, modelos distintos por capa, módulos `:data` y `:presentation`): descartada por ceremonia sin beneficio a este tamaño. Hay caso de uso solo donde hay una regla de negocio; una lectura sin regla va del ViewModel al repositorio.
- **Paquetes dentro de `:app` con disciplina** en vez del módulo `:domain`: descartada porque los issues los implementan agentes, y una barrera de compilación no se rompe por descuido; una convención sí.
- **Reutilizar las `@Entity` de Room como modelos de dominio** (estilo guía de Google): descartada porque arrastra `sincronizado`, `fechaModificacion` y los futuros campos de sync hasta la UI.

## Consequences

- Cada entidad tiene dos clases (modelo de dominio y `*Entity` de Room) y un mapper entre ellas.
- La inyección de dependencias es manual (`AppContainer` en `BovinaApplication`); pasar a Hilt más adelante es mecánico porque todo usa inyección por constructor.
