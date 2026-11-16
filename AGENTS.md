# Agents

Read `CLAUDE.md` first: it holds every rule for this repo (what Unpeep is, hard rules, design rules, stack,
behaviour). This file exists so tools other than Claude Code find it.

- Next work: `TODO.md`. The plan: `docs/PLAN.md`. Screens: `docs/SCREENS.md` + `design/screens/*.png`.
- Build: `./gradlew :app:assemblePlayDebug`. Tests: `./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest`.
- `ProjectRulesTest` fails on raw colours, sizes, text sizes or motion outside `ui/theme`, on dialogs, and on any
  socket outside `scan/net`. Fix the code, never the test.
- Never add a dependency without asking. Never make the app talk to anything but the local network
  (and GitHub for the `github` flavor's update check).
