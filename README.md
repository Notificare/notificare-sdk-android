[<img src="https://raw.githubusercontent.com/notificare/notificare-sdk-android/main/assets/logo.png"/>](https://notificare.com)

# Notificare Android SDK

[![GitHub release](https://img.shields.io/github/v/release/notificare/notificare-sdk-android?include_prereleases)](https://github.com/notificare/notificare-sdk-android/releases)
[![License](https://img.shields.io/github/license/notificare/notificare-sdk-android)](https://github.com/notificare/notificare-sdk-android/blob/main/LICENSE)

The Notificare Android SDK makes it quick and easy to communicate efficiently with many of the Notificare API services and enables you to seamlessly integrate our various features, from Push Notifications to Contextualised Storage.

Get started with our [📚 integration guides](https://docs.notifica.re/sdk/v3/android/setup) and [example projects](#examples), or [📘 browse the SDK reference]() (coming soon).


> :warning: **The v3 SDK is currently in alpha. If you are running a production application, take a look at the v2.x SDK instead.**


Table of contents
=================

* [Features](#features)
* [Releases](#releases)
* [Installation](#installation)
  * [Requirements](#requirements)
  * [Configuration](#configuration)
* [Getting Started](#getting-started)
* [Examples](#examples)


## Features

**Push notifications**: Use the SDK to receive push notifications and automatically track its engagement.

**Push notifications UI**: We provide native screens and elements to display your push notifications and handle its actions with zero effort.

**Inbox**: Apps with a built-in message inbox enjoy higher conversions due to its nature of keeping messages around that can be opened as many times as users want. The SDK gives you all the tools necessary to build your inbox UI.

**Geo-location**
> coming soon

**Loyalty**
> coming soon

**Monetise**
> coming soon

**Assets**
> coming soon

**Scannables**
> coming soon


## Installation

### Requirements

* Android 6 (API level 23) and above
* [AndroidX](https://developer.android.com/jetpack/androidx)

### Configuration

Add the Notificare Services Plugin to your project's build.gradle dependencies.

```gradle
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
```

Apply the plugin and add the following dependencies to your app's `build.gradle`.

```gradle
apply plugin: 're.notifica.gradle.notificare-services'

dependencies {
    def notificare_version = '3.0.0-alpha.1'
    implementation "re.notifica:notificare:$notificare_version"

    // Optional modules
    implementation "re.notifica:notificare-inbox:$notificare_version"

    implementation "re.notifica:notificare-push:$notificare_version"
    implementation "re.notifica:notificare-push-fcm:$notificare_version"
    implementation "re.notifica:notificare-push-hms:$notificare_version"

    implementation "re.notifica:notificare-push-ui:$notificare_version"
    implementation "re.notifica:notificare-push-ui-fcm:$notificare_version"
    implementation "re.notifica:notificare-push-ui-hms:$notificare_version"
}
```

## Getting Started

### Integration
Get started with our [📚 integration guides](https://docs.notifica.re/sdk/v3/android/setup) and [example projects](#examples), or [📘 browse the SDK reference]() (coming soon).


### Examples
- The [Demo app example project](https://github.com/Notificare/notificare-demo-android) demonstrates how to integrate and use our various modules in a single app.
- The [example project](https://github.com/Notificare/notificare-sdk-android/tree/main/sample) demonstrates other integrations in a simplified fashion, to quickly understand how a given feature should be implemented.
