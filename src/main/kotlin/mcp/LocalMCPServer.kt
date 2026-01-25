package mcp

import java.util.concurrent.TimeUnit
import java.io.File

data class ToolInfo(
    val name: String,
    val description: String,
    val parameters: List<String>
)

class LocalMCPServer(private val rootDir: File) {

    fun getToolsList(): List<ToolInfo> {
        return listOf(
            ToolInfo(
                name = "git_log",
                description = "Get recent git commit history. Returns: Hash, Author, Date, Message.",
                parameters = listOf("limit")
            ),
            ToolInfo(
                name = "list_files",
                description = "List files in the project directory (non-recursive).",
                parameters = listOf("path")
            ),
             ToolInfo(
                name = "read_file",
                description = "Read the content of a specific file. Use this to inspect code.",
                parameters = listOf("path")
            )
        )
    }

    fun executeTool(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "git_log" -> {
                val limit = params["limit"]?.toIntOrNull() ?: 20
                runGitLog(limit)
            }
            "list_files" -> {
                val path = params["path"] ?: "."
                val targetDir = File(rootDir, path)
                if (!targetDir.exists()) return "Error: Path '$path' not found."
                
                targetDir.listFiles()
                    ?.joinToString("\n") { 
                        val type = if (it.isDirectory) "[DIR]" else "[FILE]"
                        "$type ${it.name}" 
                    } 
                    ?: "Empty directory"
            }
            "read_file" -> {
                val path = params["path"] ?: return "Error: path required"
                val file = File(rootDir, path)
                if (!file.exists()) return "Error: File '$path' not found."
                if (file.isDirectory) return "Error: '$path' is a directory, not a file."
                
                // Limit size to avoid context overflow
                file.readText().take(2000)
            }
            else -> "Error: Unknown tool '$toolName'"
        }
    }

    private fun runGitLog(limit: Int): String {
        return try {
            val process = ProcessBuilder("git", "log", "--pretty=format:%h | %an | %ad | %s", "--date=short", "-n", limit.toString())
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
            
            process.waitFor(5, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Error reading git logs: ${e.message}"
        }
    }
}
