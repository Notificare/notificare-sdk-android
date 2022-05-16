# CHANGELOG

## Upcoming release

- Fix notification content when opening partial inbox items

## 3.1.1

- Improve bitmap loading
- Prevent crashing when generating notifications with invalid attachments
- Include JSON serialisation methods for unknown notifications

## 3.1.0

- Include `Accept-Language` and custom `User-Agent` headers
- Allow notification push services to be subclassed
- Add notification attributes to unknown notifications
- Improve `allowedUI` to accurately reflect push capabilities
- Prevent push tokens from being registered immediately after an install

## 3.0.1

- Update Gradle build tools
- Use compile-time constant for the SDK version 
- Remove unnecessary `BuildConfig` files
- Update dependencies

## 3.0.0

Please check our [migration guide](./MIGRATION.md) before adopting the v3.x generation.
