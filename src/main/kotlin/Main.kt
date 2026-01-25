import kotlinx.coroutines.runBlocking
import client.OllamaClient
import client.OllamaOptions
import model.Message
import java.io.File
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) = runBlocking {
    println("--- Day 29: Local Git Analyst 📊 (Smart Edition) ---")
    
    // 1. Get Git Logs
    println("🔍 extracting git logs...")
    val rawLogs = getGitLogs()
    
    if (rawLogs.isEmpty()) {
        println("❌ No logs found or not a git repository.")
        return@runBlocking
    }
    
    // 2. Pre-calculate Stats (Help the LLM!)
    val logLines = rawLogs.lines().filter { it.isNotBlank() }
    val commitCount = logLines.size
    
    // Parse authors (Format: Hash | Author | Date | Msg)
    val authors = logLines.mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size >= 2) parts[1].trim() else null
    }.groupingBy { it }.eachCount()
    
    val topAuthor = authors.maxByOrNull { it.value }
    val authorsStat = authors.entries.joinToString(", ") { "${it.key} (${it.value})" }

    val statsSummary = """
        *** PRE-CALCULATED STATISTICS ***
        - Total Commits Loaded: $commitCount
        - Contributors: $authorsStat
        - Most Active Author: ${topAuthor?.key ?: "Unknown"} with ${topAuthor?.value ?: 0} commits
        *********************************
    """.trimIndent()

    println("✅ Loaded $commitCount commits.")
    println("📊 Stats: Top Author is ${topAuthor?.key}")

    // 3. Initialize Client
    val client = OllamaClient()
    val modelName = "qwen2.5:1.5b"
    val history = mutableListOf<Message>()
    
    // 4. System Prompt with Data AND Stats
    val systemPrompt = """
        You are a Data Analyst specializing in Git history analysis.
        
        $statsSummary
        
        RAW GIT LOGS:
        Format: Hash | Author | Date | Message
        ----------------------------------------
        $rawLogs
        ----------------------------------------
        
        INSTRUCTIONS:
        - Use the PRE-CALCULATED STATISTICS to answer questions about counts and top authors.
        - Use RAW GIT LOGS to answer questions about specific features, dates, or task details.
        - If the answer is not in the logs, say "I don't see that in the provided logs".
        - Keep answers concise and factual.
    """.trimIndent()
    
    history.add(Message("system", systemPrompt))
    
    println("\n🤖 Analyst is ready! Ask questions like:")
    println("- 'Who made the most commits?'")
    println("- 'What did we do on Day 25?'")
    println("(Type 'exit' to quit)\n")

    try {
        while (true) {
            print("\nYou: ")
            val input = readlnOrNull()?.trim() ?: break
            if (input.equals("exit", ignoreCase = true)) break
            if (input.isBlank()) continue

            history.add(Message("user", input))

            print("Analyst: ")
            val options = OllamaOptions(temperature = 0.1, num_ctx = 4096)
            
            val response = client.generate(history, model = modelName, options = options)
            println(response)

            history.add(Message("assistant", response))
            
            if (history.size > 8) {
                val kept = mutableListOf<Message>()
                kept.add(history.first())
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
        val process = ProcessBuilder("git", "log", "--pretty=format:%h | %an | %ad | %s", "--date=short", "-n", "50")
            .redirectErrorStream(true)
            .start()
        
        process.waitFor(5, TimeUnit.SECONDS)
        process.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "Error reading git logs: ${e.message}"
    }
}
