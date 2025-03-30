package io.github.sgtsilvio.gradle.mavencentral.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.tasks.*
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
    val baseUrl = project.objects.property<URI>().convention(URI("https://central.sonatype.com/"))

    @get:Input
    val publish = project.objects.property<Boolean>().convention(true)

    @get:Input
    val waitUntilFullyPublished = project.objects.property<Boolean>().convention(false)

    @get:Input
    val deploymentName = project.objects.property<String>()

    @get:OutputFile
    val deploymentIdFile = project.objects.fileProperty()

    @TaskAction
    protected fun run() {
        val bundleFile = bundleFile.get().asFile
        val credentials = credentials.get()
        val baseUrl = baseUrl.get()
        val isPublish = publish.get()
        val waitUntilFullyPublished = waitUntilFullyPublished.get()
        val deploymentName = deploymentName.get()
        val deploymentIdFile = deploymentIdFile.get().asFile

        val publisherApi = MavenCentralPublisherApi(baseUrl, credentials.username!!, credentials.password!!)
        val deploymentId = publisherApi.upload(bundleFile, deploymentName, isPublish)
        deploymentIdFile.writeText(deploymentId)

        val message = "maven central deployment id: $deploymentId"
        logger.quiet(message)
        val expectedStates = when {
            !isPublish -> setOf("VALIDATED")
            !waitUntilFullyPublished -> setOf("PUBLISHING", "PUBLISHED")
            else -> setOf("PUBLISHED")
        }
        val unexpectedStates = when {
            !isPublish -> setOf("PUBLISHING", "PUBLISHED", "FAILED")
            else -> setOf("FAILED")
        }
        var previousState: String? = null
        var errorCount = 0
        while (true) {
            val status = try {
                publisherApi.getStatus(deploymentId)
            } catch (e: IllegalStateException) {
                if (++errorCount > 3) {
                    throw e
                }
                logger.lifecycle("$message, state request failed for $errorCount times, treating as temporary error: ${e.message}")
                TimeUnit.SECONDS.sleep(1)
                continue
            }
            errorCount = 0
            val state = status.getString("deploymentState")
            val stateMessage = "$message, state: $state"
            if (state in expectedStates) {
                logger.lifecycle(stateMessage)
                break
            }
            if (state in unexpectedStates) {
                val failedMessage = "$stateMessage (expected ${expectedStates.joinToString(" or ")})"
                logger.lifecycle(failedMessage)
                val errors = status.optJSONObject("errors")?.toString(2)
                val errorMessage = if (errors == null) failedMessage else "$failedMessage, errors:\n$errors"
                error(errorMessage)
            }
            val waitingMessage = "$stateMessage (waiting for ${expectedStates.joinToString(" or ")})"
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
