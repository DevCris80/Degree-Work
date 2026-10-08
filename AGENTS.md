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
- **`data`** (package in `:app`) is the only code that touches Room. `data/local` holds the database, the `*Entity` classes (`entity/`) and the DAOs (`dao/`); `data/repository` holds the mappers and the `Room*Repository` implementations. Every read and write from outside `data` goes through a repository interface.
- **`ui`** has one ViewModel per screen, created with `appViewModel { container -> ... }`. It exposes a `StateFlow` of screen state; the screen receives state and events, and `AppNavHost` only navigates.
- **Use cases** exist where there is a business rule. A read with no rule goes from the ViewModel straight to the repository.
- **`AppContainer`** wires repositories and use cases by hand. Register new ones there.

Migration in progress (#33): code in `ui` or `service` that still reaches `database` or a DAO, and the old `domain` package inside `:app`, is legacy being replaced screen by screen. New code follows the rules above.
