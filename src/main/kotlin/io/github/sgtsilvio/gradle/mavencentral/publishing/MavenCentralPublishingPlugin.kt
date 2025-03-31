package io.github.sgtsilvio.gradle.mavencentral.publishing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
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
        val cleanStagingRepositoryTask = registerCleanStagingRepositoryTask(project, stagingRepositoryDirectory)
        val bundleTask = registerBundleTask(project, stagingRepositoryDirectory, outputDirectory)
        val uploadBundleTask = registerUploadBundleTask(project, bundleTask, outputDirectory)
        publishingExtension.publications.withType<MavenPublication> {
            val publicationName = name
            val publishToStagingRepositoryTask = getPublishToStagingRepositoryTask(project, publicationName)
            publishToStagingRepositoryTask { dependsOn(cleanStagingRepositoryTask) }
            bundleTask { mustRunAfter(publishToStagingRepositoryTask) }
            registerPublishTask(project, publicationName, publishToStagingRepositoryTask, uploadBundleTask)
        }
        registerPublishAllTask(project, uploadBundleTask)
    }

    private fun registerCleanStagingRepositoryTask(
        project: Project,
        stagingRepositoryDirectory: Provider<Directory>,
    ) = project.tasks.register<Delete>("clean${STAGING_REPOSITORY_NAME.capitalize()}Repository") {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Cleans the local '$STAGING_REPOSITORY_NAME' repository directory."
        delete(stagingRepositoryDirectory)
    }

    private fun registerBundleTask(
        project: Project,
        stagingRepositoryDirectory: Provider<Directory>,
        outputDirectory: Provider<Directory>,
    ) = project.tasks.register<Zip>("mavenCentralBundle") {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Bundles the local '$STAGING_REPOSITORY_NAME' repository content."
        from(stagingRepositoryDirectory)
        exclude("**/maven-metadata.xml*")
        destinationDirectory.set(outputDirectory)
        archiveFileName.set("bundle.zip")
    }

    private fun registerUploadBundleTask(
        project: Project,
        bundleTask: TaskProvider<Zip>,
        outputDirectory: Provider<Directory>,
    ) = project.tasks.register<MavenCentralUploadTask>("uploadMavenCentralBundle") {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Uploads, validates and publishes the '${bundleTask.name}' to Maven Central."
        bundleFile.set(bundleTask.flatMap { it.archiveFile })
        deploymentName.set(project.provider { "${project.group}:${project.name}:${project.version}" })
        credentials.set(project.providers.credentials(PasswordCredentials::class, "mavenCentral"))
        deploymentIdFile.set(outputDirectory.map { it.file("deployment-id.txt") })
    }

    private fun getPublishToStagingRepositoryTask(project: Project, publicationName: String) =
        project.tasks.named("publish${publicationName.capitalize()}PublicationTo${STAGING_REPOSITORY_NAME.capitalize()}Repository")

    private fun registerPublishTask(
        project: Project,
        publicationName: String,
        publishToStagingRepositoryTask: TaskProvider<*>,
        uploadBundleTask: TaskProvider<MavenCentralUploadTask>,
    ) = project.tasks.register("publish${publicationName.capitalize()}PublicationToMavenCentral") {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Publishes Maven publication '$publicationName' to Maven Central."
        dependsOn(publishToStagingRepositoryTask)
        dependsOn(uploadBundleTask)
    }

    private fun registerPublishAllTask(
        project: Project,
        uploadBundleTask: TaskProvider<MavenCentralUploadTask>,
    ) = project.tasks.register("publishToMavenCentral") {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Publishes all Maven publications produced by this project to Maven Central."
        dependsOn(project.tasks.named("publishAllPublicationsTo${STAGING_REPOSITORY_NAME.capitalize()}Repository"))
        dependsOn(uploadBundleTask)
    }
}

private const val STAGING_REPOSITORY_NAME = "mavenCentralStaging"

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
