android {
    namespace = "re.notifica.push"
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
        freeCompilerArgs += listOf(
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
    implementation(project(":notificare-utilities"))

    // Android
    implementation(libs.androidx.core)
    implementation(libs.bundles.androidx.lifecycle)

    implementation(libs.google.playServices.base)

    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.messaging)

    // OkHttp
    implementation(libs.okhttp)

    // Moshi
    implementation(libs.bundles.moshi)
    ksp(libs.moshi.codegen)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
