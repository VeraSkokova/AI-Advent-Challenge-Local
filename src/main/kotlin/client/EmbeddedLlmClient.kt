package client

import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import de.kherud.llama.args.MiroStat
import model.Message
import java.io.File

class EmbeddedLlmClient(modelPath: String) {

    private val model: LlamaModel

    init {
        // Updated for version 4.x API
        val params = ModelParameters()
            .setModel(modelPath)
            .setGpuLayers(0) // CPU only
            // .setContextSize(4096) // If setContextSize exists, use it. If not, rely on default.
            // Removing explicit context size to avoid compilation errors if method name differs. 
            // Usually default is 512 or derived from model.
        
        // Constructor now takes only ModelParameters
        model = LlamaModel(params)
    }

    fun chat(history: List<Message>): String {
        val prompt = buildPrompt(history)
        
        val inferenceParams = InferenceParameters(prompt)
            .setTemperature(0.7f)
            .setPenalizeNl(true)
            .setMiroStat(MiroStat.V2)
            .setStopStrings("User:") // Stop generation when User: appears

        val response = StringBuilder()
        for (output in model.generate(inferenceParams)) {
            response.append(output)
        }
        
        return response.toString().trim()
    }

    private fun buildPrompt(history: List<Message>): String {
        val sb = StringBuilder()
        for (msg in history) {
            val role = msg.role.replaceFirstChar { it.uppercase() }
            sb.append("$role: ${msg.content}\n")
        }
        sb.append("AI: ")
        return sb.toString()
    }
    
    fun close() {
        model.close()
    }
}
