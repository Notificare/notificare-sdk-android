// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.notificare.services) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.huawei.agconnect) apply false
}

subprojects {
    if (this.name != "sample") {

        apply(plugin = "com.android.library")
        apply(plugin = "kotlin-android")
        apply(plugin = "kotlin-kapt")
        apply(plugin = "kotlin-parcelize")
        apply(plugin = "maven-publish")

        group = rootProject.libs.versions.maven.artifactGroup
        version = rootProject.libs.versions.maven.artifactVersion

        val properties = loadProperties("local.properties")
        val awsS3AccessKeyId = System.getenv("AWS_ACCESS_KEY_ID") ?: properties.getProperty("aws.s3.access_key_id")
        val awsS3SecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY") ?: properties.getProperty("aws.s3.secret_access_key")

        val artifactChannel =
            if (Regex("/^([0-9]+)\\.([0-9]+)\\.([0-9]+)\$/").matches(version.toString())) "releases" else "prereleases"

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
