# Gradle Maven Central Publishing Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.sgtsilvio.gradle.maven-central-publishing?color=brightgreen&style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.sgtsilvio.gradle.maven-central-publishing)
[![GitHub](https://img.shields.io/github/license/sgtsilvio/gradle-maven-central-publishing?color=brightgreen&style=for-the-badge)](LICENSE)
[![GitHub Workflow Status (with branch)](https://img.shields.io/github/actions/workflow/status/sgtsilvio/gradle-maven-central-publishing/check.yml?branch=main&style=for-the-badge)](https://github.com/SgtSilvio/gradle-maven-central-publishing/actions/workflows/check.yml?query=branch%3Amain)

Gradle plugin to ease publishing to Maven Central.

> [!NOTE]
> This plugin uses the Maven Central Portal Publishing API and can not be used for publishing via the legacy OSSRH mechanism.
> If you still publish via OSSRH please migrate your namespace to the Maven Central Portal: https://central.sonatype.com/publishing/namespaces)

## How to Use

The following is an example configuration for a Java library.

```kotlin
plugins {
    `java-library`
    id("io.github.sgtsilvio.gradle.maven-central-publishing") version "0.3.0"
    id("io.github.sgtsilvio.gradle.metadata") version "0.6.0"
}

group = "org.example"
version = "0.1.0"

metadata {
    readableName = "Example library"
    description = "Example library description"
    license {
        apache2()
    }
    developers {
        register("jdoe") {
            fullName = "John Doe"
            email = "john.doe@example.org"
        }
    }
    github {}
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("main") {
            from(components["java"])
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["main"])
}
```

Run the `publishToMavenCentral` task to publish all registered publications to Maven Central.

Run the `publish<PublicationName>PublicationToMavenCentral` task to publish only a specific publication to Maven Central.

## Configuration

### Maven Central Portal Credentials

You need to create a user token for the Maven Central portal: https://central.sonatype.com/account.

Set the project properties `mavenCentralUsername` and `mavenCentralPassword` to your user token.

- Set credentials via command line

  ```shell
  ./gradlew publishToMavenCentral -PmavenCentralUsername=<tokenUsername> -PmavenCentralPassword=<tokenPassword>
  ```

- Set credentials via `~/.gradle/gradle.properties`

  ```properties
  mavenCentralUsername=<tokenUsername>
  mavenCentralPassword=<tokenPassword>
  ```

- Set credentials via environment variables

  ```shell
  export ORG_GRADLE_PROJECT_mavenCentralUsername=<tokenUsername>
  export ORG_GRADLE_PROJECT_mavenCentralPassword=<tokenPassword>
  ```

### PGP Credentials

Because Maven Central requires signatures, you also need to configure PGP credentials.
The specific configuration depends on your `signing` plugin configuration.
For instance, the example above requires to set the project properties `signingKey` and `signingPassword` to the PGP key and passphrase.

## Integration with Other Plugins

This plugin does not implement functionality already provided by core Gradle plugins like `maven-publish` and `signing`.
Because this plugin integrates with core Gradle plugins, it enables you to use any other plugin that integrates with these plugins, for example [gradle-metadata](https://github.com/SgtSilvio/gradle-metadata) to easily configure pom metadata.
