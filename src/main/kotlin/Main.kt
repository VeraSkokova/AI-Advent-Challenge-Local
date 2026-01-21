import client.EmbeddedLlmClient
import model.Message
import java.io.File
import java.util.Scanner

fun main() {
    println("=== День 27: Локальный LLM Чат (Embedded) ===")
    
    val modelPath = "model.gguf"
    val modelFile = File(modelPath)
    
    if (!modelFile.exists()) {
        println("ОШИБКА: Файл модели '$modelPath' не найден!")
        println("Пожалуйста, скачайте GGUF модель (например, Phi-3 Mini) и положите её в корень проекта как 'model.gguf'.")
        println("Пример ссылки: https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf")
        return
    }

    println("Загрузка модели из $modelPath... Пожалуйста, подождите.")
    val client = try {
        EmbeddedLlmClient(modelPath)
    } catch (e: Exception) {
        println("Не удалось загрузить модель: ${e.message}")
        e.printStackTrace()
        return
    }
    println("Модель успешно загружена!")
    println("Команды: /exit - выход, /system <текст> - сменить системный промпт, /about - о программе")

    val scanner = Scanner(System.`in`)
    val history = mutableListOf<Message>()
    var systemPrompt = "You are a helpful AI assistant."

    // Initialize history with system prompt
    history.add(Message("system", systemPrompt))

    while (true) {
        print("\nВы: ")
        if (!scanner.hasNextLine()) break
        val input = scanner.nextLine().trim()

        if (input.isEmpty()) continue

        if (input.equals("/exit", ignoreCase = true)) {
            println("До свидания!")
            break
        }

        if (input.equals("/about", ignoreCase = true)) {
            println("\n=== О программе ===")
            println("AI Advent Challenge - День 27")
            println("Разработчик: Vera Skokova")
            println("\nИспользуемые библиотеки:")
            println("- java-llama.cpp (MIT License) - https://github.com/kherud/java-llama.cpp")
            println("- llama.cpp (MIT License) - https://github.com/ggerganov/llama.cpp")
            println("===================")
            continue
        }

        if (input.startsWith("/system ")) {
            val newSystem = input.removePrefix("/system ").trim()
            if (newSystem.isNotEmpty()) {
                systemPrompt = newSystem
                history.clear()
                history.add(Message("system", systemPrompt))
                println("Системный промпт обновлен, контекст сброшен.")
                continue
            }
        }

        history.add(Message("user", input))
        
        print("ИИ: ")
        System.out.flush() 

        try {
            val response = client.chat(history)
            println(response)
            history.add(Message("assistant", response))
        } catch (e: Exception) {
            println("\nОшибка генерации ответа: ${e.message}")
        }
    }
    
    client.close()
}
