# CHANGELOG

## 3.3.1

- Expose Notificare base methods and properties statically to the JVM
- Add intent actions & extras as JVM fields

## 3.3.0

- Add opt-in intent when opening a beacon foreground service notification
- Fix GMS/HMS notification race condition for late configured apps
- Monetise module for Google Play
- Prevent unnecessary copies of `LiveData` from being created
- Update HMS libraries, fixing Google Play compliance warnings
- Monitor and range non-triggering beacons
- Prevent internal _main beacon region_ from triggering events
- Fix R8/ProGuard minification issues
- Add Java-friendly wrappers

## 3.2.0

- Fix notification content when opening partial inbox items
- Use GMS/HMS `message.sentTime` when creating inbox items
- Log events methods correctly throw when failures are not recoverable
- Improve session control mechanism
- Fix session length
- Fix GMS/HMS token refresh race condition for late configured apps
- Add `InAppBrowser` notification type
- Aliased `WebView` action into `InAppBrowser`, aligning with the notification type
- Ensure listeners are called on the main thread
- Allow non-ASCII header values

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
