# MIGRATING

Notificare 3.x upgrades our implementation language from Java to Kotlin and brings a highly modular system.
While you can consume the library from Java sources, there are certain pain points that we are addressing to give you the best developer experience possible. If you are using Kotlin in your app, you should have a first class experience.

## Requirements

We have increased the minimum Android version required to run the Notificare library to Android 6.0+ (API level 23+). According to Google's API version distribution stats, this minimum version should support ~94% of the devices worldwide. Additionally, this version addresses the granular permission user controls which is essential in modern privacy-compliant applications.

## Configuration file

Instead of the `notificareconfig.properties` you are used to having in the v2.x library and that contains two sets of app keys — development and production — we have moved to a `notificare-services.json` for each environment, similar to what Firebase offers.

We have also created a Gradle plugin to help you automatically configure the Notificare libraries. Add the following to your `build.gradle` files.

```gradle
//
// root build.gradle
//
buildscript {
    repositories {
        maven { url 'https://maven.notifica.re/releases' }
    }
    dependencies {
        classpath 're.notifica.gradle:notificare-services:1.0.0'
    }
}

allprojects {
    repositories {
        maven { url 'https://maven.notifica.re/releases' }
    }
}

//
// app build.gradle
//
plugins {
    // ...
    id 're.notifica.gradle.notificare-services'
}
```

## Packages

We have made some changes to existing packages, removed a few and added several others. Here's all the dependencies available:

```gradle
dependencies {
    def notificare_version = '3.0.0'
    implementation "re.notifica:notificare:$notificare_version"

    //
    // Optional modules
    //

    implementation "re.notifica:notificare-assets:$notificare_version"
    implementation "re.notifica:notificare-inbox:$notificare_version"
    implementation "re.notifica:notificare-loyalty:$notificare_version"

    implementation "re.notifica:notificare-geo:$notificare_version"
    implementation "re.notifica:notificare-geo-gms:$notificare_version"         // Enable support for Google Mobile Services.
    implementation "re.notifica:notificare-geo-hms:$notificare_version"         // Enable support for Huawei Mobile Services.
    implementation "re.notifica:notificare-geo-beacons:$notificare_version"     // Enable support for beacons detection.

    implementation "re.notifica:notificare-push:$notificare_version"
    implementation "re.notifica:notificare-push-gms:$notificare_version"        // Enable support for Google Mobile Services.
    implementation "re.notifica:notificare-push-hms:$notificare_version"        // Enable support for Huawei Mobile Services.

    implementation "re.notifica:notificare-push-ui:$notificare_version"
    implementation "re.notifica:notificare-push-ui-gms:$notificare_version"     // Enable support for Google Mobile Services.
    implementation "re.notifica:notificare-push-ui-hms:$notificare_version"     // Enable support for Huawei Mobile Services.

    implementation "re.notifica:notificare-scannables:$notificare_version"
    implementation "re.notifica:notificare-scannables-gms:$notificare_version"  // Enable support for Google Mobile Services.
    implementation "re.notifica:notificare-scannables-hms:$notificare_version"  // Enable support for Huawei Mobile Services.
}
```

## Package cherry-picking

In the v2.x iteration, we already took the first steps to a more modular library. In this iteration we took it a whole new level.

We understand that not every app will take advantage of every bit of functionality provided by our platform. To help reduce your app's size, dependency footprint and automatically included permissions, now you are able to cherry-pick which modules you want to include in your app.

In the hypothetical scenario where you have an app that wants to add push notifications and an in-app inbox, only supporting devices running Google's mobile services, you would include the following dependencies.

```gradle
dependencies {
    def notificare_version = '3.0.0'
    implementation "re.notifica:notificare:$notificare_version"

    implementation "re.notifica:notificare-inbox:$notificare_version"

    implementation "re.notifica:notificare-push:$notificare_version"
    implementation "re.notifica:notificare-push-gms:$notificare_version"

    implementation "re.notifica:notificare-push-ui:$notificare_version"
    implementation "re.notifica:notificare-push-ui-gms:$notificare_version"
}
```

## Moving forward

Given the foundational changes and large differences in the Public API in the new libraries, we found the best way to cover every detail is to go through the [documentation](https://docs.notifica.re/sdk/v3/android/implementation) for each of the modules you want to include and adjust accordingly.

As always, if you have anything to add or require further assistance, we are available via our [Support Channel](mailto:support@notifica.re).
