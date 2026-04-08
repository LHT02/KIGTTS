#include <jni.h>
#include <vector>
#include <memory>
#include <algorithm>
#include <cstring>

#include "api/echo_canceller3_factory.h"
#include "api/echo_control.h"
#include "audio_processing/audio_buffer.h"
#include "audio_processing/include/audio_processing.h"

namespace {
struct Aec3Handle {
    std::unique_ptr<webrtc::EchoControl> echo;
    std::unique_ptr<webrtc::AudioBuffer> render_buffer;
    std::unique_ptr<webrtc::AudioBuffer> capture_buffer;
    webrtc::StreamConfig render_cfg;
    webrtc::StreamConfig capture_cfg;
    int channels = 1;
    std::vector<float> render_data;
    std::vector<float> capture_data;
    std::vector<float> capture_out;
};

void fill_frame(std::vector<float>& dst, const float* src, int offset, int length) {
    if (dst.empty()) return;
    std::fill(dst.begin(), dst.end(), 0.0f);
    const int count = std::min(length, static_cast<int>(dst.size()));
    if (count > 0) {
        std::memcpy(dst.data(), src + offset, sizeof(float) * count);
    }
}

void process_render(Aec3Handle* h, float* data, int offset, int length) {
    if (!h || !data || length <= 0) return;
    const size_t samples = h->render_cfg.num_samples();
    if (samples == 0) return;
    if (h->render_data.size() != samples) {
        h->render_data.assign(samples, 0.0f);
    }
    fill_frame(h->render_data, data, offset, length);
    float* channels[1] = { h->render_data.data() };
    h->render_buffer->CopyFrom(channels, h->render_cfg);
    h->echo->AnalyzeRender(h->render_buffer.get());
}

void process_capture(Aec3Handle* h, float* data, int offset, int length) {
    if (!h || !data || length <= 0) return;
    const size_t samples = h->capture_cfg.num_samples();
    if (samples == 0) return;
    if (h->capture_data.size() != samples) {
        h->capture_data.assign(samples, 0.0f);
        h->capture_out.assign(samples, 0.0f);
    }
    fill_frame(h->capture_data, data, offset, length);
    float* channels[1] = { h->capture_data.data() };
    h->capture_buffer->CopyFrom(channels, h->capture_cfg);
    h->echo->AnalyzeCapture(h->capture_buffer.get());
    h->echo->ProcessCapture(h->capture_buffer.get(), false);
    float* out_channels[1] = { h->capture_out.data() };
    h->capture_buffer->CopyTo(h->capture_cfg, out_channels);
    const int count = std::min(length, static_cast<int>(h->capture_out.size()));
    if (count > 0) {
        std::memcpy(data + offset, h->capture_out.data(), sizeof(float) * count);
    }
}
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_kgtts_app_audio_Aec3Processor_nativeCreate(JNIEnv* env, jobject /*thiz*/,
                                                 jint capture_rate, jint render_rate, jint channels) {
    auto handle = std::make_unique<Aec3Handle>();
    handle->channels = std::max(1, static_cast<int>(channels));

    const int rate = capture_rate;
    webrtc::EchoCanceller3Factory factory;
    handle->echo = factory.Create(rate, handle->channels, handle->channels);
    if (!handle->echo) {
        return 0;
    }

    handle->render_cfg = webrtc::StreamConfig(rate, handle->channels, false);
    handle->capture_cfg = webrtc::StreamConfig(rate, handle->channels, false);

    handle->render_buffer = std::make_unique<webrtc::AudioBuffer>(
        rate, handle->channels,
        rate, handle->channels,
        rate, handle->channels);
    handle->capture_buffer = std::make_unique<webrtc::AudioBuffer>(
        rate, handle->channels,
        rate, handle->channels,
        rate, handle->channels);

    return reinterpret_cast<jlong>(handle.release());
}

extern "C" JNIEXPORT void JNICALL
Java_com_kgtts_app_audio_Aec3Processor_nativeDestroy(JNIEnv* env, jobject /*thiz*/,
                                                  jlong ptr) {
    auto* handle = reinterpret_cast<Aec3Handle*>(ptr);
    delete handle;
}

extern "C" JNIEXPORT void JNICALL
Java_com_kgtts_app_audio_Aec3Processor_nativeProcessCapture(JNIEnv* env, jobject /*thiz*/,
                                                         jlong ptr, jfloatArray data,
                                                         jint offset, jint length) {
    auto* handle = reinterpret_cast<Aec3Handle*>(ptr);
    if (!handle || !data) return;
    jfloat* buf = env->GetFloatArrayElements(data, nullptr);
    process_capture(handle, buf, offset, length);
    env->ReleaseFloatArrayElements(data, buf, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_kgtts_app_audio_Aec3Processor_nativeProcessRender(JNIEnv* env, jobject /*thiz*/,
                                                        jlong ptr, jfloatArray data,
                                                        jint offset, jint length) {
    auto* handle = reinterpret_cast<Aec3Handle*>(ptr);
    if (!handle || !data) return;
    jfloat* buf = env->GetFloatArrayElements(data, nullptr);
    process_render(handle, buf, offset, length);
    env->ReleaseFloatArrayElements(data, buf, 0);
}
