import kotlinx.coroutines.runBlocking
import client.OllamaClient
import client.OllamaOptions
import model.Message
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = runBlocking {
    println("--- Day 28: LLM Benchmark (Baseline vs Optimized) ---")
    
    if (args.isEmpty()) {
        println("Usage: ./gradlew run --args=\"<path_to_project_root>\"")
        return@runBlocking
    }

    val projectPath = args[0]
    val rootDir = File(projectPath)
    if (!rootDir.exists()) {
        println("Error: Directory '$projectPath' does not exist.")
        return@runBlocking
    }

    // 1. Prepare Context
    println("Scanning project...")
    val context = StringBuilder()
    File(rootDir, "build.gradle.kts").let { if (it.exists()) context.append("\n=== build.gradle.kts ===\n${it.readText().take(2000)}") }
    File(rootDir, "src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.take(3).forEach { 
        context.append("\n=== ${it.name} ===\n${it.readText().take(3000)}") 
    }

    if (context.isEmpty()) {
        println("No content found.")
        return@runBlocking
    }

    val client = OllamaClient()
    val modelName = "qwen2.5:1.5b"

    try {
        // --- Scenario 1: Baseline ---
        println("\n🚀 Running Scenario 1: BASELINE (High Temp, Basic Prompt)")
        val promptBaseline = """
            Generate a README.md for this project.
            
            Code:
            $context
        """.trimIndent()
        
        val optionsBaseline = OllamaOptions(temperature = 0.7, num_ctx = 2048)
        
        val timeBaseline = measureTimeMillis {
            val result = client.generate(promptBaseline, model = modelName, options = optionsBaseline)
            File(rootDir, "README_BASELINE.md").writeText(result)
        }
        println("✅ Baseline completed in ${timeBaseline}ms. Saved to README_BASELINE.md")


        // --- Scenario 2: Optimized ---
        println("\n🚀 Running Scenario 2: OPTIMIZED (Low Temp, Expert Persona, Higher Context)")
        val promptOptimized = """
            You are a Senior Technical Writer and Kotlin Expert.
            Your task is to write a comprehensive, professional README.md for the provided project.
            
            Structure the README as follows:
            1. **Project Title & One-Liner**: Clear and catchy.
            2. **Key Features**: Bullet points inferred from code logic.
            3. **Tech Stack**: List libraries from build.gradle.
            4. **Getting Started**: Steps to run the code.
            
            Style Guidelines:
            - Use clear, concise English.
            - Use emojis for section headers.
            - Do not invent features not present in the code.
            
            Project Context:
            $context
        """.trimIndent()
        
        // Lower temperature for factual consistency, higher context to fit more code
        val optionsOptimized = OllamaOptions(temperature = 0.2, num_ctx = 4096)
        
        val timeOptimized = measureTimeMillis {
            val result = client.generate(promptOptimized, model = modelName, options = optionsOptimized)
            File(rootDir, "README_OPTIMIZED.md").writeText(result)
        }
        println("✅ Optimized completed in ${timeOptimized}ms. Saved to README_OPTIMIZED.md")

        // --- Summary ---
        println("\n📊 BENCHMARK RESULTS:")
        println("Baseline:  ${timeBaseline}ms | Temp: 0.7 | Context: 2048")
        println("Optimized: ${timeOptimized}ms | Temp: 0.2 | Context: 4096")
        println("Compare the files to see quality differences!")

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
