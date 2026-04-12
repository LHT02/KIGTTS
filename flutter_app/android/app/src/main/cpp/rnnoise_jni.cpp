#include <jni.h>

#include <cstdint>
#include <memory>
#include <vector>

#include "rnnoise.h"

namespace {

constexpr int kRnNoiseFrameSize = 480;

struct RnNoiseHandle {
    DenoiseState* state = nullptr;
};

RnNoiseHandle* fromHandle(jlong handle) {
    return reinterpret_cast<RnNoiseHandle*>(static_cast<intptr_t>(handle));
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_kgtts_kgtts_1app_audio_RnNoiseNative_nativeCreate(JNIEnv*, jclass) {
    auto* handle = new (std::nothrow) RnNoiseHandle();
    if (handle == nullptr) {
        return 0;
    }
    handle->state = rnnoise_create(nullptr);
    if (handle->state == nullptr) {
        delete handle;
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<intptr_t>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_kgtts_kgtts_1app_audio_RnNoiseNative_nativeDestroy(JNIEnv*, jclass, jlong handlePtr) {
    auto* handle = fromHandle(handlePtr);
    if (handle == nullptr) {
        return;
    }
    if (handle->state != nullptr) {
        rnnoise_destroy(handle->state);
        handle->state = nullptr;
    }
    delete handle;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_kgtts_kgtts_1app_audio_RnNoiseNative_nativeFrameSize(JNIEnv*, jclass) {
    return kRnNoiseFrameSize;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_kgtts_kgtts_1app_audio_RnNoiseNative_nativeProcessFrame(
    JNIEnv* env,
    jclass,
    jlong handlePtr,
    jfloatArray inputArray,
    jfloatArray outputArray) {
    auto* handle = fromHandle(handlePtr);
    if (handle == nullptr || handle->state == nullptr || inputArray == nullptr || outputArray == nullptr) {
        return 0.0f;
    }
    const int frameSize = kRnNoiseFrameSize;
    if (env->GetArrayLength(inputArray) < frameSize || env->GetArrayLength(outputArray) < frameSize) {
        return 0.0f;
    }
    std::vector<float> input(static_cast<size_t>(frameSize));
    std::vector<float> output(static_cast<size_t>(frameSize));
    env->GetFloatArrayRegion(inputArray, 0, frameSize, input.data());
    const float vad = rnnoise_process_frame(handle->state, output.data(), input.data());
    env->SetFloatArrayRegion(outputArray, 0, frameSize, output.data());
    return vad;
}
