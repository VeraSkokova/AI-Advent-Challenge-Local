package client

import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import model.Message
import java.io.File

class EmbeddedLlmClient(modelPath: String) {

    private val model: LlamaModel

    init {
        val params = ModelParameters()
            .setNContext(4096) // Set context window
            .setNGpuLayers(0) // CPU only for safety, or adjust if GPU is available
        
        model = LlamaModel(modelPath, params)
    }

    fun chat(history: List<Message>): String {
        val prompt = buildPrompt(history)
        
        val inferenceParams = InferenceParameters(prompt)
            .setTemperature(0.7f)
            .setPenalizeNl(true)
            .setMirostat(InferenceParameters.MiroStat.V2)
            .setAntiPrompt("User:") // Stop generation when User: appears

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
