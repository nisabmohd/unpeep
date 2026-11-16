# Contributing

Thanks for helping. Unpeep stays small, private and honest, so a few rules:

- **Device lists** (the most useful help): see `community-signatures/README.md`.
- **Translations:** `app/src/main/res/values-<lang>/strings.xml`. Short plain words, sentence case, no exclamation
  marks, no fear words ("spy", "danger"). Never translate a result into "safe". Rules: `docs/COPY.md`.
- **Checklist text** lives in `strings.xml` too. Explain what to look for and how, in one line.
- **Code:** read `CLAUDE.md`. Use the components in `docs/COMPONENTS.md` and the tokens in `ui/theme`;
  `ProjectRulesTest` checks this. No new dependency without an issue first.
- Never add anything that logs in to a device, tries passwords, opens a stream, or sends data off the local network.

Run before a pull request:
```
./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest :app:lintPlayDebug
```
By contributing you agree your work is licensed under GPL v3.
