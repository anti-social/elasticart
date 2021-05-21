import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.MavenPublication

fun RepositoryHandler.test(project: Project): MavenArtifactRepository = maven {
    name = "test"
    url = project.uri("file://${project.rootProject.buildDir}/localMaven")
}

fun Project.sonatypeUser(): String? {
    return findProperty("sonatypeUser")?.toString()
        ?: System.getenv("SONATYPE_USER")
}

fun Project.sonatypePassword(): String? {
    return findProperty("sonatypePassword")?.toString()
        ?: System.getenv("SONATYPE_PASSWORD")
}

fun MavenPublication.configurePom() = pom {
    name.set("elasticart")
    description.set("Simple elasticsearch transport that uses kotlin serialization")
    url.set("https://github.com/anti-social/elasticart")

    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }

    scm {
        url.set("https://github.com/anti-social/elasticart")
        connection.set("scm:https://github.com/anti-social/elasticart.git")
        developerConnection.set("scm:git://github.com/anti-social/elasticart.git")
    }

    developers {
        developer {
            id.set("anti-social")
            name.set("Oleksandr Koval")
            email.set("kovalidis@gmail.com")
        }
    }
}
