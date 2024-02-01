pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.notifica.re/releases")
        maven(url = "https://developer.huawei.com/repo")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.huawei.agconnect") {
                useModule("com.huawei.agconnect:agcp:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://developer.huawei.com/repo")
    }
}

rootProject.name = "notificare-sdk-android"

include(":notificare")
include(":notificare-assets")
include(":notificare-geo")
include(":notificare-geo-beacons")
include(":notificare-geo-gms")
include(":notificare-geo-hms")
include(":notificare-in-app-messaging")
include(":notificare-inbox")
include(":notificare-loyalty")
include(":notificare-monetize")
include(":notificare-monetize-gms")
include(":notificare-push")
include(":notificare-push-gms")
include(":notificare-push-hms")
include(":notificare-push-ui")
include(":notificare-push-ui-gms")
include(":notificare-push-ui-hms")
include(":notificare-scannables")
include(":notificare-scannables-gms")
include(":notificare-scannables-hms")
include(":notificare-user-inbox")
include(":sample")
