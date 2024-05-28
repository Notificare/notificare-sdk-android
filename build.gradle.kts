// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.notificare.services) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

subprojects {
    if (this.name != "sample") {

        apply(plugin = "com.android.library")
        apply(plugin = "kotlin-android")
        apply(plugin = "com.google.devtools.ksp")
        apply(plugin = "kotlin-parcelize")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")

        ktlint {
            debug.set(true)
            verbose.set(true)
            android.set(true)
            baseline.set(file("ktlint-baseline.xml"))
        }

        val ktlintTaskProvider = tasks.named("ktlintCheck")
        tasks.getByName("preBuild").finalizedBy(ktlintTaskProvider)

        detekt {
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
            baseline = file("detekt-baseline.xml")
        }

        val detektTaskProvider = tasks.named("detekt")
        tasks.getByName("preBuild").finalizedBy(detektTaskProvider)

        group = rootProject.libs.versions.maven.artifactGroup.get()
        version = rootProject.libs.versions.maven.artifactVersion.get()

        val properties = loadProperties("local.properties")
        val awsS3AccessKeyId = System.getenv("AWS_ACCESS_KEY_ID")
            ?: properties.getProperty("aws.s3.access_key_id")

        val awsS3SecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY")
            ?: properties.getProperty("aws.s3.secret_access_key")

        val artifactChannel =
            if (Regex("^([0-9]+)\\.([0-9]+)\\.([0-9]+)\$").matches(version.toString())) "releases" else "prereleases"

        afterEvaluate {
            configure<PublishingExtension> {
                publications {
                    register("release", MavenPublication::class) {
                        from(components["release"])
                    }
                }
                repositories {
                    maven {
                        name = "S3"
                        url = uri("s3://maven.notifica.re/$artifactChannel")
                        credentials(AwsCredentials::class) {
                            accessKey = awsS3AccessKeyId
                            secretKey = awsS3SecretAccessKey
                        }
                    }
                }
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
