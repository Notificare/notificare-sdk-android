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
    namespace = "re.notifica.sample.user.inbox"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        applicationId = "re.notifica.sample.user.inbox.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 12
        versionName = "3.0.0"

        manifestPlaceholders["googleMapsApiKey"] = properties.getProperty("google.maps.key")
        manifestPlaceholders["auth0Domain"] = properties.getProperty("user.inbox.login.domain")
        manifestPlaceholders["auth0Scheme"] = "auth.re.notifica.sample.user.inbox.app.dev"

        resValue("string", "user_inbox_base_url", properties.getProperty("user.inbox.base.url"))
        resValue("string", "user_inbox_fetch_inbox_url", properties.getProperty("user.inbox.fetch.inbox.url"))
        resValue("string", "user_inbox_register_device_url", properties.getProperty("user.inbox.register.device.url"))
        resValue("string", "user_inbox_login_domain", properties.getProperty("user.inbox.login.domain"))
        resValue("string", "user_inbox_login_client_id", properties.getProperty("user.inbox.login.client.id"))
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    // Retrofit
    implementation(libs.bundles.retrofit)

    // Auth0
    implementation(libs.auth0)

    implementation(project(":notificare"))
    implementation(project(":notificare-push"))
    implementation(project(":notificare-push-ui"))
    implementation(project(":notificare-user-inbox"))
}
