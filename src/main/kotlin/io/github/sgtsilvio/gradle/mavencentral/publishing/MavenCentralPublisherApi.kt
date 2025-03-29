package io.github.sgtsilvio.gradle.mavencentral.publishing

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.*

/**
 * @author Silvio Giebl
 */
internal class MavenCentralPublisherApi(private val baseUrl: URI, tokenUsername: String, tokenPassword: String) {

    private val httpClient = OkHttpClient()
    private val token = Base64.getEncoder().encodeToString("$tokenUsername:$tokenPassword".toByteArray())

    fun upload(bundleFile: File, deploymentName: String, publish: Boolean): String {
        var query = "?name=$deploymentName"
        if (publish) {
            query += "&publishingType=AUTOMATIC"
        }
        val uploadRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/upload$query").toString())
            .post(
                MultipartBody.Builder().addFormDataPart(
                    "bundle", bundleFile.name, bundleFile.asRequestBody("application/zip".toMediaType())
                ).build()
            )
            .header("authorization", "Bearer $token")
            .build()
        val deploymentId = httpClient.newCall(uploadRequest).execute().use { response ->
            check(response.code == 201) { "unexpected response code ${response.code} for ${response.request.url}" }
            response.body!!.string()
        }
        return deploymentId
    }

    fun getStatus(deploymentId: String): JSONObject {
        val statusRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/status?id=$deploymentId").toString())
            .post("".toRequestBody())
            .header("authorization", "Bearer $token")
            .build()
        val status = httpClient.newCall(statusRequest).execute().use { response ->
            check(response.code == 200) { "unexpected response code ${response.code} for ${response.request.url}" }
            JSONObject(response.body!!.string())
        }
        return status
    }
}
