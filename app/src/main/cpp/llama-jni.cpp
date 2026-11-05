#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <thread>
#include <chrono>

#define TAG "OpenX-LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// In full llama.cpp compilation, include:
// #include "llama.h"
// #include "common.h"

// Pointer holder structure for loaded model context
struct LlamaModelContext {
    std::string modelPath;
    int nThreads;
    int nContext;
    bool isGpu;
    bool isLoaded;
};

static LlamaModelContext* g_context = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_openx_engine_llm_LlamaNative_nativeIsGpuSupported(
        JNIEnv* env,
        jobject /* this */) {
    // Universal CPU fallback is always safe.
    // Vulkan or OpenCL GPU backend can be tested when compiled with GGML_VULKAN=1
#if defined(GGML_USE_VULKAN) || defined(GGML_USE_OPENCL)
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_example_openx_engine_llm_LlamaNative_nativeInitModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPathStr,
        jint nThreads,
        jint nContext,
        jboolean useGpu) {
    const char* path = env->GetStringUTFChars(modelPathStr, nullptr);
    LOGI("Initializing model from path: %s, threads: %d, ctx: %d, gpu: %d", path, nThreads, nContext, useGpu);

    if (g_context != nullptr) {
        LOGW("Freeing previous model context before loading new one");
        delete g_context;
        g_context = nullptr;
    }

    auto* ctx = new LlamaModelContext();
    ctx->modelPath = path;
    ctx->nThreads = nThreads > 0 ? nThreads : std::max(1u, std::thread::hardware_concurrency());
    ctx->nContext = nContext > 0 ? nContext : 1024;
    ctx->isGpu = useGpu;
    ctx->isLoaded = true;

    env->ReleaseStringUTFChars(modelPathStr, path);
    g_context = ctx;

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_example_openx_engine_llm_LlamaNative_nativeFreeModel(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr) {
    if (contextPtr != 0) {
        auto* ctx = reinterpret_cast<LlamaModelContext*>(contextPtr);
        LOGI("Freeing model context: %s", ctx->modelPath.c_str());
        delete ctx;
        if (g_context == ctx) {
            g_context = nullptr;
        }
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_openx_engine_llm_LlamaNative_nativeGenerate(
        JNIEnv* env,
        jobject thiz,
        jlong contextPtr,
        jstring promptStr,
        jfloat temperature,
        jfloat topP,
        jint maxTokens,
        jobject tokenCallback) {
    if (contextPtr == 0) {
        LOGE("Cannot generate: Model context pointer is null");
        return env->NewStringUTF("");
    }

    auto* ctx = reinterpret_cast<LlamaModelContext*>(contextPtr);
    const char* prompt = env->GetStringUTFChars(promptStr, nullptr);
    LOGI("Generating completion for prompt length: %zu with temp: %.2f", strlen(prompt), temperature);

    // If a Java callback object is provided, find its onToken(String) method for streaming
    jmethodID callbackMethod = nullptr;
    jclass callbackClass = nullptr;
    if (tokenCallback != nullptr) {
        callbackClass = env->GetObjectClass(tokenCallback);
        if (callbackClass != nullptr) {
            callbackMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
        }
    }

    // In full native llama.cpp execution, llama_eval and token sampler iterate here:
    // For standalone JNI stub before full NDK toolchain link:
    std::string response = "";

    env->ReleaseStringUTFChars(promptStr, prompt);
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_openx_engine_llm_LlamaNative_nativeTokenize(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jstring textStr) {
    if (contextPtr == 0) return 0;
    const char* text = env->GetStringUTFChars(textStr, nullptr);
    int estimatedTokens = static_cast<int>(strlen(text) / 4);
    env->ReleaseStringUTFChars(textStr, text);
    return estimatedTokens;
}

} // extern "C"
