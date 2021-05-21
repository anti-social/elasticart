import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

kotlin {
    jvm()
    linuxX64()
    js().nodejs()

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
                implementation(coroutines("core"))

                implementation(serialization("json"))

                implementation(ktorClient("core"))
                implementation(ktorClient("encoding"))
                implementation(ktorClient("json"))
                implementation(ktorClient("serialization"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation(ktorClient("mock"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))

                implementation(ktorClient("cio"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val nativeMain by creating {
            dependencies {
                implementation(ktorClient("curl"))
            }
        }
        val nativeTest by creating {}

        val jsMain by getting {
            dependencies {
                implementation(ktorClient("js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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
