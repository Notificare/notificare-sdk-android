android {
    namespace = "re.notifica.geo.beacons"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xexplicit-api=strict",
            "-opt-in=re.notifica.InternalNotificareApi",
        )
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)

    // Notificare
    implementation(project(":notificare"))
    implementation(project(":notificare-geo"))
    implementation(project(":notificare-utilities"))

    implementation(libs.androidx.core)

    // AltBeacon
    implementation(libs.altbeacon)
}
