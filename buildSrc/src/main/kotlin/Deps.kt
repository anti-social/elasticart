import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

object Deps {
    data class Dep(val group: String, val artifact: String, val version: String? = null) {
        fun toNotation(module: String? = null): String {
            val fullArtifact = if (module.isNullOrEmpty()) {
                artifact
            } else {
                "$artifact-$module"
            }
            return if (version != null) {
                "$group:$fullArtifact:$version"
            } else {
                "$group:$fullArtifact"
            }
        }
        fun impl(depencencyHandler: KotlinDependencyHandler) {
            depencencyHandler.implementation(toNotation())
        }
        fun impl(depencencyHandler: KotlinDependencyHandler, vararg modules: String) {
            modules.forEach { module ->
                depencencyHandler.implementation(toNotation(module))
            }
        }
    }

    val ktorClient = Dep("io.ktor", "ktor-client", Versions.ktor)
    val serialization = Dep("org.jetbrains.kotlinx", "kotlinx-serialization", Versions.serialization)

    val coroutines = Dep("org.jetbrains.kotlinx", "kotlinx-coroutines", Versions.coroutines)
}

fun KotlinDependencyHandler.ktorClient(module: String): String {
    return Deps.ktorClient.toNotation(module)
}

fun KotlinDependencyHandler.serialization(module: String): String {
    return Deps.serialization.toNotation(module)
}

fun KotlinDependencyHandler.coroutines(module: String): String {
    return Deps.coroutines.toNotation(module)
}
