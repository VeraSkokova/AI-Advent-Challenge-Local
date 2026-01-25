import kotlinx.coroutines.runBlocking
import client.OllamaClient
import client.OllamaOptions
import model.Message
import mcp.LocalMCPServer
import java.io.File
import kotlinx.serialization.json.*

fun main(args: Array<String>) = runBlocking {
    println("--- Day 29: Local Git Analyst (MCP Edition v2) 🤖🔧 ---")
    
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
        
        CRITICAL RULES:
        - Parameter values must be STRINGS. Do not use arrays [] or objects {}.
        - Do not invent parameters not listed in AVAILABLE TOOLS.
        - ALWAYS use relative paths from the project root (e.g., "src/main/kotlin/Main.kt").
        - If the tool output is sufficient, do not call the same tool again.
        
        EXAMPLE:
        User: "Show me recent commits"
        Assistant: {"tool": "git_log", "params": {"limit": "10"}}
    """.trimIndent()

    history.add(Message("system", systemPrompt))
    
    println("\n✅ Agent initialized with tools: git_log, git_stats, list_files, read_file")
    println("🤖 Ask me anything! (e.g., 'Who committed last?', 'Show me Main.kt')")
    println("(Type 'exit' to quit)\n")

    try {
        while (true) {
            print("\nYou: ")
            val input = readlnOrNull()?.trim() ?: break
            if (input.equals("exit", ignoreCase = true)) break
            if (input.isBlank()) continue

            history.add(Message("user", input))
            
            var turns = 0
            val maxTurns = 5
            var finalAnswerGiven = false

            while (turns < maxTurns && !finalAnswerGiven) {
                print("Thinking... ")
                // Use extremely low temp for tool calling precision
                val options = OllamaOptions(temperature = 0.0, num_ctx = 4096)
                val response = client.generate(history, model = modelName, options = options)
                
                // Parse Response
                val toolCall = parseToolCall(response)
                
                if (toolCall != null) {
                    println("\n⚙️ Executing tool: ${toolCall.name} with ${toolCall.params}")
                    
                    val toolResult = mcpServer.executeTool(toolCall.name, toolCall.params)
                    // Truncate output to avoid flooding context
                    val truncatedResult = toolResult.take(2000) + if (toolResult.length > 2000) "\n...[truncated]" else ""
                    
                    println("📝 Tool Output (${toolResult.length} chars)")
                    
                    history.add(Message("assistant", response))
                    history.add(Message("user", "TOOL_OUTPUT:\n$truncatedResult\n\nAnalyze this data. If you have the answer, output it as text. If you need more data, use another tool."))
                    turns++
                } else {
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
        val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
        val jsonString = jsonRegex.find(response)?.value ?: response.trim()
        
        if (!jsonString.startsWith("{")) return null
        
        val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject
        if (!jsonElement.containsKey("tool")) return null
        
        val tool = jsonElement["tool"]?.jsonPrimitive?.content ?: return null
        val paramsElement = jsonElement["params"]?.jsonObject
        
        // Robust parsing: handle Arrays or non-Strings by converting to String
        val params = paramsElement?.entries?.associate { entry -> 
            val value = when (val element = entry.value) {
                is JsonPrimitive -> element.content
                is JsonArray -> element.joinToString(",") { 
                    if (it is JsonPrimitive) it.content else it.toString() 
                } // Flatten arrays to comma-separated strings
                else -> element.toString()
            }
            entry.key to value
        } ?: emptyMap()
        
        return ToolCall(tool, params)
    } catch (e: Exception) {
        return null 
    }
}
