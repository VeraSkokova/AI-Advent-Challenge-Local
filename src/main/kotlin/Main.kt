import client.EmbeddedLlmClient
import model.Message
import java.io.File
import java.util.Scanner

fun main() {
    println("=== AI Advent Day 27: Embedded LLM Chat ===")
    
    val modelPath = "model.gguf"
    val modelFile = File(modelPath)
    
    if (!modelFile.exists()) {
        println("ERROR: Model file '$modelPath' not found!")
        println("Please download a GGUF model (e.g., Phi-3 Mini) and place it in the project root as 'model.gguf'.")
        println("Download link example: https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf")
        return
    }

    println("Loading model from $modelPath... Please wait.")
    val client = try {
        EmbeddedLlmClient(modelPath)
    } catch (e: Exception) {
        println("Failed to load model: ${e.message}")
        e.printStackTrace()
        return
    }
    println("Model loaded successfully!")
    println("Commands: /exit to quit, /system <text> to set system prompt")

    val scanner = Scanner(System.`in`)
    val history = mutableListOf<Message>()
    var systemPrompt = "You are a helpful AI assistant."

    // Initialize history with system prompt
    history.add(Message("system", systemPrompt))

    while (true) {
        print("\nYou: ")
        if (!scanner.hasNextLine()) break
        val input = scanner.nextLine().trim()

        if (input.isEmpty()) continue

        if (input.equals("/exit", ignoreCase = true)) {
            println("Goodbye!")
            break
        }

        if (input.startsWith("/system ")) {
            val newSystem = input.removePrefix("/system ").trim()
            if (newSystem.isNotEmpty()) {
                systemPrompt = newSystem
                // Reset history or just update system prompt? 
                // Usually changing system prompt implies a fresh start or re-contextualization.
                // For simplicity, let's just clear history and start fresh with new system prompt.
                history.clear()
                history.add(Message("system", systemPrompt))
                println("System prompt updated and conversation reset.")
                continue
            }
        }

        history.add(Message("user", input))
        
        print("AI: ")
        // Flush to ensure "AI: " prints before generation starts if we were streaming, 
        // but here we get full string. Still good practice.
        System.out.flush() 

        try {
            val response = client.chat(history)
            println(response)
            history.add(Message("assistant", response))
        } catch (e: Exception) {
            println("\nError generating response: ${e.message}")
        }
    }
    
    client.close()
}
