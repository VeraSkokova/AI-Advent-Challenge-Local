# Day 27: Local LLM (Embedded Chat)

Autonomous console chat application that carries its own brain. Runs GGUF models locally using JVM bindings for llama.cpp.

## Features
- **100% Offline**: No Ollama, no API keys, no internet required during inference.
- **Embedded Inference**: Uses `de.kherud:llama` library to run models directly in the application process.
- **REPL Interface**: Interactive console chat with command support.
- **Custom System Prompts**: Change the AI persona on the fly.

## Prerequisites
- JDK 17 or higher.
- A GGUF format LLM model (e.g., Phi-3, Llama-3, Mistral).

## Setup

1. **Download a Model**
   You need a GGUF model file. Small models (<= 3GB) recommended for CPU inference.
   *   **Recommended**: [Phi-3 Mini 4k Instruct (Q4)](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf) (~2.4GB)
   *   Alternative: [TinyLlama 1.1B](https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF) (~600MB)

2. **Place the Model**
   *   Rename the downloaded file to `model.gguf`.
   *   Place it in the **root directory** of the project.

## How to Run

```bash
./gradlew run
```

## Commands
*   `/system <text>` - Update the system prompt (resets conversation context).
*   `/exit` - Quit the application.

## Troubleshooting
*   **"Model not found"**: Ensure `model.gguf` is in the project root (where `build.gradle.kts` is).
*   **Slow performance**: This runs on CPU by default. Performance depends on your hardware and model size (parameter count/quantization).

## Tech Stack
*   Kotlin JVM
*   [java-llama.cpp](https://github.com/kherud/java-llama.cpp) (v4.2.0)
