import kotlinx.coroutines.runBlocking
import client.OllamaClient
import client.OllamaOptions
import model.Message
import mcp.LocalMCPServer
import java.io.File
import kotlinx.serialization.json.*

fun main(args: Array<String>) = runBlocking {
    println("--- Day 29: Local Git Analyst (MCP Edition) 🤖🔧 ---")
    
    val rootDir = File(".")
    val mcpServer = LocalMCPServer(rootDir)
    val client = OllamaClient()
    val modelName = "qwen2.5:1.5b"
    val history = mutableListOf<Message>()

    // 1. Build System Prompt with Tool Definitions
    val toolsList = mcpServer.getToolsList().joinToString("\n") { 
        "- ${it.name}: ${it.description} Params: ${it.parameters}" 
    }

    val systemPrompt = """
        You are a smart DevOps Assistant with access to local tools.
        
        AVAILABLE TOOLS:
        $toolsList
        
        INSTRUCTIONS:
        1. When the user asks a question, decide if you need to use a tool.
        2. To use a tool, output JSON ONLY in this format:
           {"tool": "tool_name", "params": {"param_key": "param_value"}}
        3. Do not write any text before or after the JSON.
        4. If you have the information you need, answer the user normally (text).
        
        EXAMPLE:
        User: "Show me recent commits"
        Assistant: {"tool": "git_log", "params": {"limit": "10"}}
    """.trimIndent()

    history.add(Message("system", systemPrompt))
    
    println("\n✅ Agent initialized with tools: git_log, list_files, read_file")
    println("🤖 Ask me anything! (e.g., 'What is in Main.kt?', 'Who committed last?')")
    println("(Type 'exit' to quit)\n")

    try {
        while (true) {
            print("\nYou: ")
            val input = readlnOrNull()?.trim() ?: break
            if (input.equals("exit", ignoreCase = true)) break
            if (input.isBlank()) continue

            history.add(Message("user", input))
            
            // ReAct Loop: Allow up to 3 turns (Thought -> Tool -> Thought -> Answer)
            var turns = 0
            val maxTurns = 5
            var finalAnswerGiven = false

            while (turns < maxTurns && !finalAnswerGiven) {
                print("Thinking... ")
                // Use low temp for precise tool calling
                val options = OllamaOptions(temperature = 0.1, num_ctx = 4096)
                val response = client.generate(history, model = modelName, options = options)
                
                // Parse Response
                val toolCall = parseToolCall(response)
                
                if (toolCall != null) {
                    println("\n⚙️ Executing tool: ${toolCall.name} with ${toolCall.params}")
                    
                    val toolResult = mcpServer.executeTool(toolCall.name, toolCall.params)
                    println("📝 Tool Output (${toolResult.length} chars)")
                    
                    // Add interaction to history
                    history.add(Message("assistant", response))
                    history.add(Message("user", "TOOL_OUTPUT:\n$toolResult\n\nAnalyze this data and answer the user, or use another tool."))
                    turns++
                } else {
                    // It's a final text answer
                    println("\nAnalyst: $response")
                    history.add(Message("assistant", response))
                    finalAnswerGiven = true
                }
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}

data class ToolCall(val name: String, val params: Map<String, String>)

fun parseToolCall(response: String): ToolCall? {
    try {
        // Regex to find JSON block if model adds extra text
        val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
        val jsonString = jsonRegex.find(response)?.value ?: response.trim()
        
        if (!jsonString.startsWith("{")) return null
        
        val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject
        if (!jsonElement.containsKey("tool")) return null
        
        val tool = jsonElement["tool"]?.jsonPrimitive?.content ?: return null
        val paramsElement = jsonElement["params"]?.jsonObject
        
        val params = paramsElement?.entries?.associate { 
            it.key to it.value.jsonPrimitive.content 
        } ?: emptyMap()
        
        return ToolCall(tool, params)
    } catch (e: Exception) {
        return null // Not valid JSON, treat as text
    }
}
