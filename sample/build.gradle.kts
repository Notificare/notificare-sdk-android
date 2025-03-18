import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.notificare.services)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val properties = loadProperties("local.properties")

android {
    namespace = "re.notifica.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        applicationId = "re.notifica.sample.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 12
        versionName = "3.0.0"

        manifestPlaceholders["googleMapsApiKey"] = properties.getProperty("google.maps.key")

        resValue("string", "sample_user_id", properties.getProperty("userId"))
        resValue("string", "sample_user_name", properties.getProperty("userName"))
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = properties.getProperty("keystore.debug.store.password")
            keyAlias = properties.getProperty("keystore.debug.key.alias")
            keyPassword = properties.getProperty("keystore.debug.key.password")
        }
        create("release") {
            storeFile = file("release.keystore")
            storePassword = properties.getProperty("keystore.release.store.password")
            keyAlias = properties.getProperty("keystore.release.key.alias")
            keyPassword = properties.getProperty("keystore.release.key.password")
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".dev"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "api"
    productFlavors {
        create("apiTest") {
            dimension = "api"
            applicationId = "re.notifica.sample.app.test"
        }
        create("apiProduction") {
            dimension = "api"
        }
    }

    applicationVariants.configureEach {
        when (name) {
            "apiTestDebug" -> {
                @Suppress("ktlint:standard:argument-list-wrapping")
                resValue("string", "notificare_app_links_hostname", "\"618d0f4edc09fbed1864e8d0.applinks-test.notifica.re\"")
                resValue("string", "notificare_dynamic_link_hostname", "\"sample-app-dev.test.ntc.re\"")
            }
            "apiTestRelease" -> {
                @Suppress("ktlint:standard:argument-list-wrapping")
                resValue("string", "notificare_app_links_hostname", "\"654d017fc468efc19379921e.applinks-test.notifica.re\"")
                resValue("string", "notificare_dynamic_link_hostname", "\"sample-app.test.ntc.re\"")
            }
            "apiProductionDebug" -> {
                resValue("string", "notificare_app_links_hostname", "\"61644511218adebf72c5449b.applinks.notifica.re\"")
                resValue("string", "notificare_dynamic_link_hostname", "\"sample-app-dev.ntc.re\"")
            }
            "apiProductionRelease" -> {
                resValue("string", "notificare_app_links_hostname", "\"6511625f445cc1c81d47fd6f.applinks.notifica.re\"")
                resValue("string", "notificare_dynamic_link_hostname", "\"sample-app.ntc.re\"")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

ktlint {
    debug.set(true)
    verbose.set(true)
    android.set(true)
    baseline.set(file("ktlint-baseline.xml"))
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

dependencies {
    implementation(libs.kotlinx.coroutines)

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewModel)
    implementation(libs.androidx.work.runtime)
    implementation(libs.google.material)
    implementation(libs.timber)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    // Moshi
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.codegen)

    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation(project(":notificare"))
    implementation(project(":notificare-assets"))
    implementation(project(":notificare-geo"))
    implementation(project(":notificare-geo-beacons"))
    implementation(project(":notificare-in-app-messaging"))
    implementation(project(":notificare-inbox"))
    implementation(project(":notificare-loyalty"))
    implementation(project(":notificare-push"))
    implementation(project(":notificare-push-ui"))
    implementation(project(":notificare-scannables"))
}
