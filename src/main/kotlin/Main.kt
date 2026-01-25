import kotlinx.coroutines.runBlocking
import client.OllamaClient
import client.OllamaOptions
import model.Message
import java.io.File
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) = runBlocking {
    println("--- Day 29: Local Git Analyst 📊 ---")
    
    // 1. Get Git Logs
    println("🔍 extracting git logs...")
    val gitLogs = getGitLogs()
    
    if (gitLogs.isEmpty()) {
        println("❌ No logs found or not a git repository.")
        return@runBlocking
    }
    
    println("✅ Loaded ${gitLogs.lines().size} recent commits.")
    println("📝 Sample:\n${gitLogs.lines().take(3).joinToString("\n")}\n...")

    // 2. Initialize Client
    val client = OllamaClient()
    val modelName = "qwen2.5:1.5b"
    val history = mutableListOf<Message>()
    
    // 3. System Prompt with Data
    val systemPrompt = """
        You are a Data Analyst specializing in Git history analysis.
        
        DATA SOURCE (Git Logs):
        Format: Hash | Author | Date | Message
        ----------------------------------------
        $gitLogs
        ----------------------------------------
        
        INSTRUCTIONS:
        - Analyze the provided logs to answer user questions.
        - Be specific. Count commits, identify authors, look for patterns in dates or messages.
        - If the answer is not in the logs, say "I don't see that in the provided logs".
        - Keep answers concise and factual.
    """.trimIndent()
    
    // Initialize context with system prompt
    history.add(Message("system", systemPrompt))
    
    println("\n🤖 Analyst is ready! Ask questions like:")
    println("- 'Who made the most commits?'")
    println("- 'What did we do on Day 25?'")
    println("- 'List all features added recently'")
    println("(Type 'exit' to quit)\n")

    try {
        while (true) {
            print("\nYou: ")
            val input = readlnOrNull()?.trim() ?: break
            if (input.equals("exit", ignoreCase = true)) break
            if (input.isBlank()) continue

            history.add(Message("user", input))

            print("Analyst: ")
            // Low temperature for analytical precision
            val options = OllamaOptions(temperature = 0.2, num_ctx = 4096)
            
            val response = client.generate(history, model = modelName, options = options)
            println(response)

            history.add(Message("assistant", response))
            
            // Basic history management: Keep System Prompt + last 6 messages
            if (history.size > 8) {
                val kept = mutableListOf<Message>()
                kept.add(history.first()) // Keep System Prompt with Data
                kept.addAll(history.takeLast(6))
                history.clear()
                history.addAll(kept)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}

fun getGitLogs(): String {
    return try {
        // Format: Hash | Author | Date | Subject
        val process = ProcessBuilder("git", "log", "--pretty=format:%h | %an | %ad | %s", "--date=short", "-n", "50")
            .redirectErrorStream(true)
            .start()
        
        process.waitFor(5, TimeUnit.SECONDS)
        process.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "Error reading git logs: ${e.message}"
    }
}
