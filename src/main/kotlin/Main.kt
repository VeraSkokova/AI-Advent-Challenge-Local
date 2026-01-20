import kotlinx.coroutines.runBlocking
import client.OllamaClient

fun main() = runBlocking {
    println("--- Day 25: Local LLM Client ---")
    print("Enter your question: ")
    val input = readlnOrNull()?.takeIf { it.isNotBlank() } ?: return@runBlocking
    
    val client = OllamaClient()
    try {
        println("Sending to Ollama (qwen2.5:1.5b)...")
        val answer = client.generate(input)
        println("\nAnswer:\n$answer")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
