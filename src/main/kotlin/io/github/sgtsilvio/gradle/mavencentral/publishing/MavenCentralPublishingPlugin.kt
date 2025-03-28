package io.github.sgtsilvio.gradle.mavencentral.publishing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningPlugin

/**
 * @author Silvio Giebl
 */
@Suppress("unused")
class MavenCentralPublishingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(MavenPublishPlugin::class)
        project.pluginManager.apply(SigningPlugin::class)
        val publishingExtension = project.extensions.getByType(PublishingExtension::class)
        val outputDirectory = project.layout.buildDirectory.dir("maven-central-publishing")
        val stagingRepositoryDirectory = outputDirectory.map { it.dir("staging-repository") }
        publishingExtension.repositories.maven {
            name = STAGING_REPOSITORY_NAME
            url = project.uri(stagingRepositoryDirectory)
        }
        val cleanTask = project.tasks.register<Delete>("clean${STAGING_REPOSITORY_NAME.capitalize()}Repository") {
            group = TASK_GROUP_NAME
            description = "Cleans the local '$STAGING_REPOSITORY_NAME' repository directory."
            delete(stagingRepositoryDirectory)
        }
        val bundleTask = project.tasks.register<Zip>("mavenCentralBundle") {
            group = TASK_GROUP_NAME
            description = "Bundles the local '$STAGING_REPOSITORY_NAME' repository content."
            from(stagingRepositoryDirectory)
            destinationDirectory.set(outputDirectory)
            archiveFileName.set("bundle.zip")
        }
        val uploadBundleTask = project.tasks.register<MavenCentralUploadTask>("uploadMavenCentralBundle") {
            group = TASK_GROUP_NAME
            description = "Uploads, validates and publishes the '${bundleTask.name}' to Maven Central."
            bundleFile.set(bundleTask.flatMap { it.archiveFile })
            credentials.set(project.providers.credentials(PasswordCredentials::class, "mavenCentral"))
            deploymentIdFile.set(outputDirectory.map { it.file("deployment-id.txt") })
        }
        publishingExtension.publications.withType<MavenPublication> {
            val publicationName = name
            val publishTask =
                project.tasks.named("publish${publicationName.capitalize()}PublicationTo${STAGING_REPOSITORY_NAME.capitalize()}Repository")
            publishTask {
                dependsOn(cleanTask)
            }
            bundleTask {
                mustRunAfter(publishTask)
            }
            project.tasks.register("publish${publicationName.capitalize()}PublicationToMavenCentral") {
                group = TASK_GROUP_NAME
                description = "Publishes Maven publication '$publicationName' to Maven Central."
                dependsOn(publishTask)
                dependsOn(uploadBundleTask)
            }
        }
        project.tasks.register("publishToMavenCentral") {
            group = TASK_GROUP_NAME
            description = "Publishes all Maven publications produced by this project to Maven Central."
            dependsOn(project.tasks.named("publishAllPublicationsTo${STAGING_REPOSITORY_NAME.capitalize()}Repository"))
            dependsOn(uploadBundleTask)
        }
    }
}

private const val TASK_GROUP_NAME = "publishing"
private const val STAGING_REPOSITORY_NAME = "mavenCentralStaging"

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
