import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation(
                "kotlinx.serialization.ImplicitReflectionSerializer"
            )
            languageSettings.useExperimentalAnnotation(
                "kotlinx.serialization.UnstableDefault"
            )
        }
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("reflect"))
                implementation(serialization("runtime-common"))
                implementation(ktorClient("core"))
                implementation(ktorClient("json"))
                implementation(ktorClient("serialization"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(coroutines("core-common"))
                implementation(ktorClient("mock"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(serialization("runtime"))
                implementation(ktorClient("cio"))
                implementation(ktorClient("serialization-jvm"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(coroutines("core"))
                implementation(ktorClient("mock-jvm"))
            }
        }

        val nativeMain by creating {
            dependencies {
                implementation(serialization("runtime-native"))
                implementation(ktorClient("core-native"))
                implementation(ktorClient("curl"))
                implementation(ktorClient("serialization-native"))
            }
        }
        val nativeTest by creating {
            dependencies {
                implementation(coroutines("core-native"))
                implementation(ktorClient("mock-native"))
            }
        }
        val nativeTargetNames = targets.withType<KotlinNativeTarget>().names
        project.configure(nativeTargetNames.map { getByName("${it}Main") }) {
            dependsOn(nativeMain)
        }
        project.configure(nativeTargetNames.map { getByName("${it}Test") }) {
            dependsOn(nativeTest)
        }
    }
}

publishing {
    repositories {
        bintray(project, "elasticart")
    }
}
