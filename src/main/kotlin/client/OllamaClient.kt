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
            requestTimeoutMillis = 300_000 
            socketTimeoutMillis = 300_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun generate(
        messages: List<Message>, 
        model: String = "qwen2.5:1.5b",
        options: OllamaOptions? = null
    ): String {
        val requestBody = OllamaRequest(
            model = model,
            messages = messages,
            stream = false,
            options = options
        )
        
        val response = client.post("http://localhost:11434/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        val responseBody = response.bodyAsText()
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<OllamaResponse>(responseBody)
        return parsed.message.content
    }
    
    // Convenience overload
    suspend fun generate(
        prompt: String, 
        model: String = "qwen2.5:1.5b",
        options: OllamaOptions? = null
    ): String {
        return generate(listOf(Message("user", prompt)), model, options)
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class OllamaRequest(
    val model: String, 
    val messages: List<Message>, 
    val stream: Boolean,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    val num_ctx: Int? = null,    // Context window size (default 2048)
    val num_predict: Int? = null, // Max tokens to generate
    val top_k: Int? = null,
    val top_p: Double? = null
)

@Serializable
data class OllamaResponse(val model: String, val message: Message)
