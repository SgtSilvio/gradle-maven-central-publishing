package io.github.sgtsilvio.gradle.mavencentral.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * @author Silvio Giebl
 */
@DisableCachingByDefault(because = "Uploading to an external repository")
abstract class MavenCentralUploadTask : DefaultTask() {

    @get:InputFile
    val bundleFile = project.objects.fileProperty()

    @get:Internal
    val credentials = project.objects.property<PasswordCredentials>()

    @get:Input
    val baseUrl = project.objects.property<URI>().convention(URI("https://central.sonatype.com"))

    @get:Input
    val isPublish = project.objects.property<Boolean>().convention(true)

    @TaskAction
    protected fun run() {
        val bundleFile = bundleFile.get().asFile
        val credentials = credentials.get()
        val baseUrl = baseUrl.get()

        val publisherApi = MavenCentralPublisherApi(baseUrl, credentials.username!!, credentials.password!!)
        val deploymentId = publisherApi.upload(bundleFile)
        logger.quiet("maven central deployment id: $deploymentId")
        publisherApi.waitForState(deploymentId, "VALIDATED")
        if (isPublish.get()) {
            publisherApi.publish(deploymentId)
            publisherApi.waitForState(deploymentId, "PUBLISHED")
        }
    }

    private fun MavenCentralPublisherApi.waitForState(deploymentId: String, expectedState: String) {
        while (true) {
            val state = getState(deploymentId)
            when (state) {
                expectedState -> {
                    logger.lifecycle("maven central deployment id: $deploymentId, state: $state")
                    return
                }
                "FAILED" -> {
                    val message = "maven central deployment id: $deploymentId, state: $state (expected $expectedState)"
                    logger.lifecycle(message)
                    error(message)
                }
            }
            logger.lifecycle("maven central deployment id: $deploymentId, state: $state (waiting for $expectedState)")
            TimeUnit.SECONDS.sleep(1)
        }
    }
}
