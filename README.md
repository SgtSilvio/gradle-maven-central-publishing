# Gradle Maven Central Publishing Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.sgtsilvio.gradle.maven-central-publishing?color=brightgreen&style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.sgtsilvio.gradle.maven-central-publishing)
[![GitHub](https://img.shields.io/github/license/sgtsilvio/gradle-maven-central-publishing?color=brightgreen&style=for-the-badge)](LICENSE)
[![GitHub Workflow Status (with branch)](https://img.shields.io/github/actions/workflow/status/sgtsilvio/gradle-maven-central-publishing/check.yml?branch=main&style=for-the-badge)](https://github.com/SgtSilvio/gradle-maven-central-publishing/actions/workflows/check.yml?query=branch%3Amain)

Gradle plugin to ease publishing to Maven Central.

## How to Use

```kotlin
plugins {
    `java-library`
    id("io.github.sgtsilvio.gradle.maven-central-publishing") version "0.1.0"
}

publishing {
    // Use the official maven-publish plugin configuration
    // The following is a default configuration for a java library
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

singing {
    // Use the official signing plugin configuration
    // The following requires to set the project properties signingKey and signingPassword to the PGP key and passphrase
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}
```

Run the `publishToMavenCentral` task to publish all registered publications to Maven Central.

Run the `publish<PublicationName>PublicationToMavenCentral` task to publish only a specific publication to Maven Central.

## Configuration

### Credentials

You need to create a user token for the Maven Central portal: https://central.sonatype.com/account.

Set the project properties `mavenCentralUsername` and `mavenCentralPassword` to your user token.

#### Set Credentials via Command Line

```shell
./gradlew publishToMavenCentral -PmavenCentralUsername=<tokenUsername> -PmavenCentralPassword=<tokenPassword>
```

#### Set Credentials via `~/.gradle/gradle.properties`

```properties
mavenCentralUsername=<tokenUsername>
mavenCentralPassword=<tokenPassword>
```

#### Set Credentials via Environment Variables

```shell
export ORG_GRADLE_PROJECT_mavenCentralUsername=<tokenUsername>
export ORG_GRADLE_PROJECT_mavenCentralPassword=<tokenPassword>
```

## Integration with Other Plugins

- This plugin does not implement functionality already provided by core Gradle plugins like `maven-publish` and `signing`.
- Because this plugin integrates with core Gradle plugins, it enables you to use any other plugin that integrates with these plugins, for example [gradle-metadata](https://github.com/SgtSilvio/gradle-metadata) to easily configure pom metadata.
