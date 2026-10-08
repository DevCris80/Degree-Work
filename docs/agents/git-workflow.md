# Git workflow

How to branch, commit and open pull requests in this repo. Apply these rules whenever you commit — when a skill implements an issue, or when the user asks for commits.

## Rules

- **Never commit to `main`.** All work reaches `main` through a pull request.
- **One branch per issue**, created from an up-to-date `main` and named `issue-<number>-<short-slug>` (e.g. `issue-34-andamiaje-capas`). There are no long-lived or per-milestone branches.
- **Do not start an issue that has an open blocker.** Check its `Blocked by` section / issue dependencies first.
- **Commit messages** are in Spanish, prefixed with the area they touch: `AppBovina: <qué cambió>`.
- **No AI attribution in commits.** Do not add `Co-Authored-By` trailers, session links or any other line crediting an AI assistant. The only author of a commit is the person who owns the issue.
- **One pull request per issue**, targeting `main`. Its body contains `Closes #<number>` and its milestone is the issue's milestone:
  `gh pr create --base main --milestone "<milestone>" --title "..." --body "Closes #<number> ..."`
- **Do not merge the pull request.** The person who owns the issue merges it; review by the other collaborator is not required.

## Milestones

A milestone groups the issues of one stage of the project; each issue belongs to exactly one. Milestones only group work — they have no branch of their own. When creating an issue, set its milestone (`gh issue create --milestone "<milestone>"`).
