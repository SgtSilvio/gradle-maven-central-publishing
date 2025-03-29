plugins {
    `kotlin-dsl`
    signing
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
}

group = "io.github.sgtsilvio.gradle"

metadata {
    readableName = "Gradle Maven Central Publishing Plugin"
    description = "Gradle plugin to ease publishing to Maven Central"
    license {
        apache2()
    }
    developers {
        register("SgtSilvio") {
            fullName = "Silvio Giebl"
        }
    }
    github {
        org = "SgtSilvio"
        issues()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.json)
}

gradlePlugin {
    plugins {
        create("mavenCentralPublishing") {
            id = "$group.maven-central-publishing"
            implementationClass = "$group.mavencentral.publishing.MavenCentralPublishingPlugin"
            tags = listOf("maven-central", "maven-central-portal", "publishing")
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}
