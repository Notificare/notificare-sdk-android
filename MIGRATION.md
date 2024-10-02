# MIGRATING

In Notificare 4.x, one of the most significant changes is the immutability of device identifiers. Previously, the `id` property of a `NotificareDevice` would change based on the device's push capability. Now, new devices will be assigned a UUID that remains constant. For existing devices, their current identifier will persist without changes.

## Breaking changes

### Device identifier immutability

As noted, device identifiers are now immutable in Notificare 4.x. Most applications won't need to directly access the device identifier or the push token, as Notificare handles these internally. However, if you do need to access the push token, it is available through the `notificare-push` module.

```kotlin
// Get the current subscription.
val subscription = Notificare.push().subscription

// Observe changes to the subscription.
Notificare.push().observableSubscription.observe(lifecycleOwner) { subscription ->

}
```

### Device registration behavior

In previous versions, the `onDeviceRegistered` event triggered when enabling or disabling remote notifications. With the new immutability of device identifiers, this event will now only trigger once — when the device is initially created.

If you need to track changes when a device switches between a temporary and a push-enabled state (or vice versa), you can listen to the `observableSubscription` property, as demonstrated in the example above.

### Asynchronous functions

Previously, the functions `launch()`, `unlaunch()`, `enableRemoteNotifications()`, and `disableRemoteNotifications()` would complete before their underlying processes were finished, requiring you to listen for events like `onReady()` to know when to continue. Now, these functions are asynchronous, ensuring the entire process is completed during their execution, which allows developers to write simpler control flows.

```diff
-class ExampleViewModel : ViewModel(), Notificare.Listener {
+class ExampleViewModel : ViewModel() {
-    init {
-        Notificare.addListener(this)
-    }
-
-    override fun onCleared() {
-        super.onCleared()
-        Notificare.removeListener(this)
-    }
-
-    override fun onReady(application: NotificareApplication) {
-        // Notificare is ready. Continue...
-    }

    fun doSomething() {
-        Notificare.launch()
+        viewModelScope.launch {
+            try {
+                Notificare.launch()
+
+                // Notificare is ready. Continue...
+            } catch (e: Exception) {
+
+            }
+        }
    }
}
```

### Drop support for Huawei Mobile Services

Support for Huawei Mobile Services (HMS) has been discontinued due to limited market adoption and the absence of ongoing development from Huawei. As a result, the `notificare-*-hms` peer dependencies have been removed, and the `notificare-*-gms` modules have been merged into their core modules, streamlining the integration process.

```diff
dependencies {
-    def notificare_version = '3.0.0'
+    def notificare_version = '4.0.0'
    
    implementation "re.notifica:notificare:$notificare_version"

    // Optional modules
    implementation "re.notifica:notificare-inbox:$notificare_version"
    implementation "re.notifica:notificare-push:$notificare_version"
-   implementation "re.notifica:notificare-push-gms:$notificare_version"
-   implementation "re.notifica:notificare-push-hms:$notificare_version"
    implementation "re.notifica:notificare-push-ui:$notificare_version"
-   implementation "re.notifica:notificare-push-ui-gms:$notificare_version"
-   implementation "re.notifica:notificare-push-ui-hms:$notificare_version"
}
```

## Migration guides for other versions

Looking to migrate from an older version of Notificare? Refer to the following guides for more details:

- [Migrating to 3.0.0](./MIGRATION-3.0.md)

---

As always, if you have anything to add or require further assistance, we are available via our [Support Channel](mailto:support@notifica.re).
