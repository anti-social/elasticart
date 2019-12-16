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
    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60")
}

idea {
    module {
        isDownloadJavadoc = false
        isDownloadSources = false
    }
}
