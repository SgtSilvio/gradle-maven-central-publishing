package io.github.sgtsilvio.gradle.mavencentral.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
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
    val publish = project.objects.property<Boolean>().convention(true)

    @get:OutputFile
    val deploymentIdFile = project.objects.fileProperty()

    @TaskAction
    protected fun run() {
        val bundleFile = bundleFile.get().asFile
        val credentials = credentials.get()
        val baseUrl = baseUrl.get()
        val isPublish = publish.get()
        val deploymentIdFile = deploymentIdFile.get().asFile

        val publisherApi = MavenCentralPublisherApi(baseUrl, credentials.username!!, credentials.password!!)
        val deploymentId = publisherApi.upload(bundleFile)
        logger.quiet("maven central deployment id: $deploymentId")
        deploymentIdFile.writeText(deploymentId)
        publisherApi.waitForState(deploymentId, "VALIDATED")
        if (isPublish) { // TODO separate publish task?
            publisherApi.publish(deploymentId)
            publisherApi.waitForState(deploymentId, "PUBLISHED")
        }
    }

    private fun MavenCentralPublisherApi.waitForState(deploymentId: String, expectedState: String) {
        var previousState: String? = null
        while (true) {
            val status = getStatus(deploymentId)
            val state = status.getString("deploymentState")
            val message = "maven central deployment id: $deploymentId, state: $state"
            if (state == expectedState) {
                logger.lifecycle(message)
                return
            }
            if (state == "FAILED") {
                val failedMessage = "$message (expected $expectedState)"
                logger.lifecycle(failedMessage)
                error(failedMessage + '\n' + status.getJSONObject("errors").toString(2))
            }
            val waitingMessage = "$message (waiting for $expectedState)"
            if (state != previousState) {
                logger.lifecycle(waitingMessage)
            } else {
                logger.info(waitingMessage)
            }
            previousState = state
            TimeUnit.SECONDS.sleep(1)
        }
    }
}
