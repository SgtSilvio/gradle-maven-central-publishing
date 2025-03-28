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

    fun upload(bundleFile: File): String {
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
        return deploymentId
    }

    fun getState(deploymentId: String): String {
        val statusRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/status?id=$deploymentId").toString())
            .post("".toRequestBody())
            .header("authorization", "Bearer $token")
            .build()
        val state = httpClient.newCall(statusRequest).execute().use { response ->
            check(response.code == 200) { "unexpected response code ${response.code} for ${response.request.url}" }
            JSONObject(response.body!!.string()).getString("deploymentState")
        }
        return state
    }

    fun publish(deploymentId: String) {
        val publishRequest = Request.Builder()
            .url(baseUrl.resolve("api/v1/publisher/deployment/$deploymentId").toString())
            .post("".toRequestBody())
            .header("authorization", "Bearer $token")
            .build()
        httpClient.newCall(publishRequest).execute().use { response ->
            check(response.code == 204) { "unexpected response code ${response.code} for ${response.request.url}" }
        }
    }
}
