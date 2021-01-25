plugins {
    `kotlin-dsl`
    idea
}

repositories {
    mavenLocal()
    jcenter()
    repositories {
    maven("https://plugins.gradle.org/m2/")
  }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
}

idea {
    module {
        isDownloadJavadoc = false
        isDownloadSources = false
    }
}
