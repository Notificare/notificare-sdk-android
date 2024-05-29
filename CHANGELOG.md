# CHANGELOG

## Upcoming release

- Fix padding discrepancies in alert dialog with actions
- Device identifiers become long-lived
- Keep weak references for listeners to prevent memory leaks
- `launch()`, `unlaunch()`, `enableRemoteNotifications()` and `disableRemoteNotifications()` become suspending functions with a callback alternative

#### Breaking changes

- `NotificareDevice.id` attribute no longer contains the push token. Use `Notificare.push().subscriptionId` instead.
- The `NotificareDevice` data model was reduced to only publicly relevant attributes.
- `onDeviceRegistered` only triggers once, when the device is created.
- `launch()`, `unlaunch()`, `enableRemoteNotifications()` and `disableRemoteNotifications()` become suspending functions that complete after all the work is done.
- `NotificareTransport` was moved to the push module.
- Drops support for the monetize module.
- Drops support for Huawei Mobile Services.
- Drops support for v1 passes in-app wallet.

## 3.9.0

- Add support for deferred links

## 3.8.0

- Prevent processing location updates too close to the last known location
- Fix race condition where geo triggers and region sessions were sent multiple times
- Limit the amount of location points and ranged beacons in geo sessions

## 3.7.1

- Fix map loading conditionals leading to no camera updates

## 3.7.0

- Adjusted zoom level when presenting a single map marker
- Fix Google Play Services Location minification issue

#### Important changes since 3.6.1

- Add manifest flag to disable the auto configuration
- Automatically enable remote notifications during launch when possible
- Automatically enable location updates during launch when possible
- Prevent the `device_registered` event from invoking before the `ready` event
- Fix warning when notification intents are handled by the broadcast receiver
- Include proguard rule to work around the issue with Moshi in R8
- Fix crash when presenting an in-app browser when the phone has none installed
- Fix `getParcelableExtra` on API 33 in certain cases

**Important notice:** Re-enabling remote notifications and location services is no longer required.
You can safely remove the following piece of code as the SDK will automatically handle it for you during the launch flow.

```kotlin
override fun onReady(application: NotificareApplication) {
    // This check is no longer necessary.
    if (Notificare.push().hasRemoteNotificationsEnabled) {
        Notificare.push().enableRemoteNotifications()
    }

    // This check is no longer necessary.
    if (Notificare.geo().hasLocationServicesEnabled) {
        Notificare.geo().enableLocationUpdates()
    }
}
```

## 3.7.0-beta.1

- Add manifest flag to disable the auto configuration
- Automatically enable remote notifications during launch when possible
- Automatically enable location updates during launch when possible
- Prevent the `device_registered` event from invoking before the `ready` event
- Fix warning when notification intents are handled by the broadcast receiver
- Include proguard rule to work around the issue with Moshi in R8
- Fix crash when presenting an in-app browser when the phone has none installed
- Fix `getParcelableExtra` on API 33 in certain cases

**Important notice:** Re-enabling remote notifications and location services is no longer required. 
You can safely remove the following piece of code as the SDK will automatically handle it for you during the launch flow.

```kotlin
override fun onReady(application: NotificareApplication) {
    // This check is no longer necessary.
    if (Notificare.push().hasRemoteNotificationsEnabled) {
        Notificare.push().enableRemoteNotifications()
    }

    // This check is no longer necessary.
    if (Notificare.geo().hasLocationServicesEnabled) {
        Notificare.geo().enableLocationUpdates()
    }
}
```

## 3.6.1

- Fix race condition when synchronising monitored regions

## 3.6.0

- Allow checking which regions are being monitored
- Allow checking which regions the device is inside of
- Allow setting the amount of regions to monitor

## 3.5.4

- Prevent queued events without an associated device
- Prevent `logCustom` usage before Notificare becomes ready

## 3.5.3

- Explicit handling of Notificare Links in Deep Link notifications
- Improve supported deep links validation
- Stricter unlaunch flow

## 3.5.2

- Prevent multiple configurations from taking place
- Add broadcast receiver for geo events
- Start monitoring nearest regions immediately after upgrading to background location
- Correctly track device on initial application open event

## 3.5.1

- Improved action categories parsing
- Prevent Glide from invoking the coroutine continuation several times
- Fix cached language when the network request fails
- Update cached device when the language changes
- Use YouTube privacy-enhanced mode

## 3.5.0

- Add user-level inbox module
- Remove non-optional `id` requirement from HMS system notifications
- Compiles with & targets Android 13
- Include `POST_NOTIFICATIONS` permission in the push module
- Add Live Activities compatibility mechanism
- Allow a context evaluation upon un-suppressing in-app messages
- Include the delivery mechanism for notification received events

## 3.4.1

- Fix locale-sensitive time formatting on `NotificareTime` objects

## 3.4.0

#### Important changes since 3.3.0

- In-app messaging module

## 3.4.0-beta.2

- Fix card in-app message click outside to dismiss

## 3.4.0-beta.1

- In-app messaging
- Refactor deprecation notices on the Notification UI

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
