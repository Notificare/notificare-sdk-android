android {
    namespace = "re.notifica.monetize"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get().toString()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
                argument("room.incremental", "true")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    // Android
    implementation(libs.androidx.lifecycle.livedata)

    // Android: Room
    implementation(libs.bundles.androidx.room)
    kapt(libs.androidx.room.compiler)

    // OkHttp
    implementation(libs.okhttp)

    // Moshi
    implementation(libs.bundles.moshi)
    kapt(libs.moshi.codegen)
}
