pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.notifica.re/releases")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "notificare-sdk-android"

include(":notificare")
include(":notificare-assets")
include(":notificare-geo")
include(":notificare-geo-beacons")
include(":notificare-in-app-messaging")
include(":notificare-inbox")
include(":notificare-loyalty")
include(":notificare-push")
include(":notificare-push-ui")
include(":notificare-scannables")
include(":notificare-user-inbox")
include(":sample")
include(":sample-user-inbox")
