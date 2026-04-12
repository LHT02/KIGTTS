#include <jni.h>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <vector>

#include "speex/speex_preprocess.h"

namespace {

struct SpeexHandle {
    SpeexPreprocessState* state = nullptr;
    int frameSize = 0;
};

SpeexHandle* fromHandle(jlong handle) {
    return reinterpret_cast<SpeexHandle*>(static_cast<intptr_t>(handle));
}

inline spx_int16_t floatToInt16(float value) {
    const float scaled = std::round(std::clamp(value, -1.0f, 1.0f) * 32767.0f);
    return static_cast<spx_int16_t>(scaled);
}

inline float int16ToFloat(spx_int16_t value) {
    return static_cast<float>(value) / 32768.0f;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_lhtstudio_kigtts_app_audio_SpeexNative_nativeCreate(
    JNIEnv*,
    jclass,
    jint frameSize,
    jint sampleRate) {
    auto* handle = new (std::nothrow) SpeexHandle();
    if (handle == nullptr) {
        return 0;
    }
    handle->frameSize = std::max(1, static_cast<int>(frameSize));
    handle->state = speex_preprocess_state_init(handle->frameSize, static_cast<int>(sampleRate));
    if (handle->state == nullptr) {
        delete handle;
        return 0;
    }
    int denoise = 1;
    int agc = 0;
    int vad = 0;
    int noiseSuppress = -30;
    speex_preprocess_ctl(handle->state, SPEEX_PREPROCESS_SET_DENOISE, &denoise);
    speex_preprocess_ctl(handle->state, SPEEX_PREPROCESS_SET_AGC, &agc);
    speex_preprocess_ctl(handle->state, SPEEX_PREPROCESS_SET_VAD, &vad);
    speex_preprocess_ctl(handle->state, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &noiseSuppress);
    return static_cast<jlong>(reinterpret_cast<intptr_t>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_lhtstudio_kigtts_app_audio_SpeexNative_nativeDestroy(JNIEnv*, jclass, jlong handlePtr) {
    auto* handle = fromHandle(handlePtr);
    if (handle == nullptr) {
        return;
    }
    if (handle->state != nullptr) {
        speex_preprocess_state_destroy(handle->state);
        handle->state = nullptr;
    }
    delete handle;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lhtstudio_kigtts_app_audio_SpeexNative_nativeProcessFrame(
    JNIEnv* env,
    jclass,
    jlong handlePtr,
    jfloatArray inputArray,
    jfloatArray outputArray) {
    auto* handle = fromHandle(handlePtr);
    if (handle == nullptr || handle->state == nullptr || inputArray == nullptr || outputArray == nullptr) {
        return;
    }
    const int frameSize = handle->frameSize;
    if (env->GetArrayLength(inputArray) < frameSize || env->GetArrayLength(outputArray) < frameSize) {
        return;
    }
    std::vector<float> input(static_cast<size_t>(frameSize));
    std::vector<spx_int16_t> frame(static_cast<size_t>(frameSize));
    std::vector<float> output(static_cast<size_t>(frameSize));
    env->GetFloatArrayRegion(inputArray, 0, frameSize, input.data());
    for (int i = 0; i < frameSize; ++i) {
        frame[static_cast<size_t>(i)] = floatToInt16(input[static_cast<size_t>(i)]);
    }
    speex_preprocess_run(handle->state, frame.data());
    for (int i = 0; i < frameSize; ++i) {
        output[static_cast<size_t>(i)] = int16ToFloat(frame[static_cast<size_t>(i)]);
    }
    env->SetFloatArrayRegion(outputArray, 0, frameSize, output.data());
}
