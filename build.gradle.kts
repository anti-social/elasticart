buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.ajoberstar.grgit") version "4.1.0"
    signing
    `maven-publish`
}

val tag = grgit.describe(mapOf("match" to listOf("v*"), "tags" to true))
    ?: "v0.0.0-SNAPSHOT"

group = "dev.evo.elasticart"
version = tag.trimStart('v')

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("kotlin-multiplatform")
        plugin("kotlinx-serialization")
        plugin("maven-publish")
        plugin("signing")
    }

    repositories {
        mavenCentral()
    }

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    signing {
        sign(publishing.publications)
    }

    publishing {
        repositories {
            test(project)
        }

        publications.withType<MavenPublication> {
            artifact(javadocJar.get())

            configurePom()
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            username.set(sonatypeUser())
            password.set(sonatypePassword())
        }
    }
}
