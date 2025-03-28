package io.github.sgtsilvio.gradle.mavencentral.publishing

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import org.json.JSONObject
import java.net.URI
import java.util.*

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

    @TaskAction
    protected fun run() {
        val bundleFile = bundleFile.get().asFile
        val credentials = credentials.get()
        val baseUrl = baseUrl.get()

        val token = Base64.getEncoder().encodeToString("${credentials.username}:${credentials.password}".toByteArray())

//        val httpClient = HttpClient.newHttpClient()
//        val boundary = UUID.randomUUID().toString()
//        val filePublisher = HttpRequest.BodyPublishers.ofFile(bundleFile.toPath())
//        val request = HttpRequest.newBuilder(baseUrl.resolve("api/v1/publisher/upload?publishingType=USER_MANAGED"))
//            .POST(
//                HttpRequest.BodyPublishers.concat(
//                    HttpRequest.BodyPublishers.ofString(
//                        "--$boundary\r\n" +
//                        "content-disposition: form-data; name=\"bundle\"; filename=\"bundle.zip\"\r\n" +
//                        "content-type: application/zip\r\n" +
//                        "content-length: ${filePublisher.contentLength()}\r\n" +
//                        "\r\n"
//                    ),
//                    filePublisher,
//                    HttpRequest.BodyPublishers.ofString("\r\n--$boundary--\r\n"),
//                )
//            )
//            .header("authorization", "Bearer $token")
//            .header("content-type", "multipart/form-data; boundary=$boundary")
//            .build()
//        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        val httpClient = OkHttpClient()
        val uploadRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/upload").toString())
            .post(
                MultipartBody.Builder().addFormDataPart(
                    "bundle", "bundle.zip", bundleFile.asRequestBody("application/zip".toMediaType())
                ).build()
            )
            .header("authorization", "Bearer $token")
            .build()
        val deploymentId = httpClient.newCall(uploadRequest).execute().use { response ->
            check(response.code == 201) { "unexpected response code ${response.code} for ${response.request.url}" }
            response.body!!.string()
        }
        val statusRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/status?id=$deploymentId").toString())
            .post("".toRequestBody())
            .header("authorization", "Bearer $token")
            .build()
        val statusResponse = httpClient.newCall(statusRequest).execute().use { response ->
            check(response.code == 200) { "unexpected response code ${response.code} for ${response.request.url}" }
            response.body!!.string()
        }
        val state = JSONObject(statusResponse).getString("deploymentState")
        // repeat while state != VALIDATED or FAILED
//        val publishRequest = Request.Builder()
//            .url(baseUrl.resolve("api/v1/publisher/deployment/$deploymentId").toString())
//            .post("".toRequestBody())
//            .header("authorization", "Bearer $token")
//            .build()
//        httpClient.newCall(publishRequest).execute().use { response ->
//            check(response.code == 204) { "unexpected response code ${response.code} for ${response.request.url}" }
//        }
    }
}
