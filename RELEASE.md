# Release process

1. Update the `maven_artifact_version` in `variables.gradle`.
2. Update the `NOTIFICARE_VERSION` in `notificare/src/main/java/re/notifica/internal/Version.kt`.
3. Update the `CHANGELOG.md`.
4. Push the changes to the repo.
5. Run `./gradlew clean`.
6. Run `./gradlew publishReleasePublicationToS3Repository`.
7. Create a GitHub release with the contents of the `CHANGELOG.md`.
