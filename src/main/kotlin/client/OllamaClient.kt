package client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.Message

class OllamaClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            socketTimeoutMillis = 60000
            connectTimeoutMillis = 10000
        }
    }

    suspend fun generate(prompt: String, model: String = "qwen2.5:1.5b"): String {
        val requestBody = OllamaRequest(
            model = model,
            messages = listOf(Message("user", prompt)),
            stream = false
        )
        
        val response = client.post("http://localhost:11434/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        val responseBody = response.bodyAsText()
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<OllamaResponse>(responseBody)
        return parsed.message.content
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class OllamaRequest(val model: String, val messages: List<Message>, val stream: Boolean)

@Serializable
data class OllamaResponse(val model: String, val message: Message)
