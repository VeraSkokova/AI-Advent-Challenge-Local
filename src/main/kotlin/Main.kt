import kotlinx.coroutines.runBlocking
import client.OllamaClient
import model.Message
import java.io.File

fun main(args: Array<String>) = runBlocking {
    println("--- Day 26: Local LLM README Generator ---")
    
    // 1. Argument parsing (Manual, without extra libs)
    if (args.isEmpty()) {
        println("Usage: ./gradlew run --args=\"<path_to_project_root>\"")
        println("Example: ./gradlew run --args=\".\"")
        return@runBlocking
    }

    val projectPath = args[0]
    val rootDir = File(projectPath)

    if (!rootDir.exists() || !rootDir.isDirectory) {
        println("Error: Directory '$projectPath' does not exist.")
        return@runBlocking
    }

    // 2. Scan files (simplified: build.gradle.kts + src/**/*.kt)
    println("Scanning project in: ${rootDir.absolutePath}...")
    val context = StringBuilder()
    
    // Add build.gradle.kts for dependencies context
    val buildFile = File(rootDir, "build.gradle.kts")
    if (buildFile.exists()) {
        context.append("\n=== build.gradle.kts ===\n")
        context.append(buildFile.readText().take(2000)) // Limit size
    }

    // Add Kotlin source files (limit to top 3 to avoid context overflow)
    val srcDir = File(rootDir, "src/main/kotlin")
    if (srcDir.exists()) {
        srcDir.walkTopDown()
            .filter { it.extension == "kt" }
            .take(3) 
            .forEach { file ->
                context.append("\n=== ${file.name} ===\n")
                context.append(file.readText().take(3000))
            }
    }

    if (context.isEmpty()) {
        println("No suitable files found to analyze.")
        return@runBlocking
    }

    // 3. Generate README via Ollama
    val client = OllamaClient()
    val modelName = "qwen2.5:1.5b"
    
    println("Analyzing code with $modelName...")
    val prompt = """
        You are a Technical Writer. Generate a professional README.md for this Kotlin project.
        
        Project Code Context:
        $context
        
        Requirements:
        1. Title and Description (infer from code)
        2. Tech Stack (libraries from build.gradle)
        3. Key Features (based on code logic)
        4. Usage Example
        
        Output ONLY the Markdown content.
    """.trimIndent()

    try {
        val readmeContent = client.generate(prompt, model = modelName)
        
        // 4. Save result
        val outputFile = File(rootDir, "README_GENERATED.md")
        outputFile.writeText(readmeContent)
        println("\n✅ Success! README generated at: ${outputFile.absolutePath}")
        println("--- Preview ---\n")
        println(readmeContent.take(500) + "...")
        
    } catch (e: Exception) {
        println("Error generating README: ${e.message}")
    } finally {
        client.close()
    }
}
