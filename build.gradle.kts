buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    kotlin("multiplatform") apply false
    id("kotlinx-serialization") version Versions.kotlin apply false
    id("org.ajoberstar.grgit") version "4.1.0"
}

val tag = grgit.describe(mapOf("match" to listOf("v*"), "tags" to true))
    ?: "v0.0.0-SNAPSHOT"

subprojects {
    apply {
        plugin("kotlin-multiplatform")
        plugin("kotlinx-serialization")
        plugin("maven-publish")
    }

    repositories {
        mavenCentral()
        jcenter()
    }
    group = "dev.evo"
    version = tag.trimStart('v')
}
