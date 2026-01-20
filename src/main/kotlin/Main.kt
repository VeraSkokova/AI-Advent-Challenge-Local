import kotlinx.coroutines.runBlocking
import client.OllamaClient
import model.Message

fun main() = runBlocking {
    println("--- Day 25: Local LLM Chat (with History) ---")
    val client = OllamaClient()
    val history = mutableListOf<Message>()
    var summary = ""
    val MAX_HISTORY_SIZE = 10
    val MODEL_NAME = "qwen2.5:1.5b"

    try {
        while (true) {
            print("\nYou: ")
            val input = readlnOrNull()?.trim() ?: break
            if (input.equals("exit", ignoreCase = true)) break
            if (input.isBlank()) continue

            // 1. Prepare context
            val currentContext = mutableListOf<Message>()
            if (summary.isNotEmpty()) {
                currentContext.add(Message("system", "Summary of previous conversation: $summary"))
            }
            currentContext.addAll(history)
            currentContext.add(Message("user", input))

            // 2. Generate response
            println("\n[System] Sending request to $MODEL_NAME...")
            print("AI: ")
            
            // Explicitly passing model name
            val response = client.generate(currentContext, model = MODEL_NAME) 
            println(response)

            // 3. Update history
            history.add(Message("user", input))
            history.add(Message("assistant", response))

            // 4. Compress if needed
            if (history.size >= MAX_HISTORY_SIZE) {
                println("\n[System] Compressing history with $MODEL_NAME...")
                val toSummarize = history.dropLast(2) // Keep last 2 messages
                val keep = history.takeLast(2)
                
                val summaryPrompt = """
                    Summarize the following conversation history into a single concise paragraph.
                    Previous summary: $summary
                    
                    New messages:
                    ${toSummarize.joinToString("\n") { "${it.role}: ${it.content}" }}
                """.trimIndent()

                val newSummary = client.generate(
                    listOf(Message("user", summaryPrompt)),
                    model = MODEL_NAME
                )
                
                summary = newSummary
                history.clear()
                history.addAll(keep)
                println("[System] History compressed. Summary: ${summary.take(50)}...")
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
