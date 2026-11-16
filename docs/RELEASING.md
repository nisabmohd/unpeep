# Releasing

1. Update `versionCode`/`versionName` in `app/build.gradle.kts` and add a `## x.y.z` section to `CHANGELOG.md`.
2. Run tests and check size: `./gradlew :app:testPlayReleaseUnitTest :app:assemblePlayRelease` (< 5 MB).
3. Regenerate the baseline profile if screens changed.
4. Tag `vX.Y.Z` and push: `.github/workflows/release.yml` builds signed `github` and `foss` APKs, writes
   `SHA256SUMS.txt`, and creates the GitHub release from the changelog.
   Secrets: `UNPEEP_KEYSTORE_BASE64`, `UNPEEP_KEYSTORE_PASSWORD`, `UNPEEP_KEY_ALIAS`, `UNPEEP_KEY_PASSWORD`.
5. Play: `./gradlew :app:bundlePlayRelease`, upload; listing text in `release/listing.md`, data safety in
   `release/data-safety.md`, content rating in `release/content-rating.md`.
6. F-Droid builds `foss` from the tag using `fastlane/metadata`.
Local release files go in `build/dist/v<version>/`.
