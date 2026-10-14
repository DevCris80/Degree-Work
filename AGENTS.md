# AGENTS.md

## Agent skills

### Issue tracker

Issues live as GitHub issues in DevCris80/Degree-Work, managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Git workflow

Never commit to `main`: one branch and one pull request per issue. Read `docs/agents/git-workflow.md` before committing.

### Domain docs

Single-context layout — `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Architecture (AppBovina)

Three layers, `ui → domain ← data`. The why is in `docs/adr/0001-capas-con-dominio-puro-y-datos-offline-first.md`.

- **`:domain`** (Gradle module, pure Kotlin) holds the models, the repository interfaces and the use cases. Its only dependency is `kotlinx-coroutines-core`.
- **`data`** (package in `:app`) is the only code that touches Room. `data/local` holds the database, the `*Entity` classes (`entity/`) and the DAOs (`dao/`); `data/repository` holds the mappers and the `Room*Repository` implementations. Every read and write from outside `data` goes through a repository interface; that includes `data/SeedData`, which writes the test Animal through `RegistrarAnimal`.
- **Schema changes** bump `AppDatabase.version`, add an explicit `Migration` and commit the schema Room exports to `AppBovina/app/schemas/`. There is no destructive fallback from version 10 on: a missing Migration crashes the app instead of wiping data.
- **`ui`** has one ViewModel per screen, created with `appViewModel { container -> ... }`. It exposes a `StateFlow` of screen state; the screen receives state and events, and `AppNavHost` only navigates.
- **Use cases** exist where there is a business rule. A read with no rule goes from the ViewModel straight to the repository.
- **`AppContainer`** creates the database and wires repositories and use cases by hand. Register new ones there. It is the only class outside `data` that imports from `data`.
