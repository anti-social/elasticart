buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    kotlin("multiplatform") apply false
    id("kotlinx-serialization") version Versions.kotlin apply false
}

subprojects {
    apply {
        plugin("kotlin-multiplatform")
        plugin("kotlinx-serialization")
        plugin("maven-publish")
    }

    repositories {
        mavenCentral()
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
    }
    group = "dev.evo"
    version = "0.0.2"
}
