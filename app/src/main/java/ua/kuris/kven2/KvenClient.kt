package ua.kuris.kven2

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class KvenClient(
    private val baseUrl: String,
    private val apiKey: String,
) {
    fun isConfigured(): Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank()

    fun sendMessage(message: String): String {
        if (!isConfigured()) {
            throw IOException("Kven client is not provisioned")
        }

        val model = resolveModel()
        val requestBody = JSONObject()
            .put("model", model)
            .put("stream", false)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", message),
                ),
            )

        val response = request(
            method = "POST",
            path = "/v1/chat/completions",
            body = requestBody.toString(),
        )
        val json = JSONObject(response)
        val choices = json.optJSONArray("choices")
            ?: throw IOException("Kven response has no choices")
        if (choices.length() == 0) {
            throw IOException("Kven response has no choices")
        }
        val content = choices
            .getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()
        if (content.isEmpty()) {
            throw IOException("Kven response is empty")
        }
        return content
    }

    private fun resolveModel(): String {
        val response = request(method = "GET", path = "/v1/models")
        val data = JSONObject(response).optJSONArray("data")
            ?: throw IOException("Kven gateway returned no models")
        if (data.length() == 0) {
            throw IOException("Kven gateway returned no models")
        }
        val model = data.getJSONObject(0).optString("id").trim()
        if (model.isEmpty()) {
            throw IOException("Kven gateway returned an invalid model")
        }
        return model
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
    ): String {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 300_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = response.take(1000).ifBlank { "HTTP $status" }
                throw IOException("Gateway error $status: $detail")
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}
