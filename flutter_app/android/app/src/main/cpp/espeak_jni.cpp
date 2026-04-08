#include <jni.h>
#include <string>
#include <vector>
#include <mutex>

#include <espeak-ng/speak_lib.h>

#include "phonemize.hpp"

namespace {

std::mutex g_mutex;
bool g_initialized = false;

std::string jstring_to_utf8(JNIEnv *env, jstring value_show) {
    if (!value_show) {
        return "";
    }
    const char *chars = env->GetStringUTFChars(value_show, nullptr);
    if (!chars) {
        return "";
    }
    std::string out(chars);
    env->ReleaseStringUTFChars(value_show, chars);
    return out;
}

void append_utf8(std::string &out, char32_t codepoint) {
    if (codepoint <= 0x7F) {
        out.push_back(static_cast<char>(codepoint));
    } else if (codepoint <= 0x7FF) {
        out.push_back(static_cast<char>(0xC0 | ((codepoint >> 6) & 0x1F)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3F)));
    } else if (codepoint <= 0xFFFF) {
        out.push_back(static_cast<char>(0xE0 | ((codepoint >> 12) & 0x0F)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3F)));
    } else if (codepoint <= 0x10FFFF) {
        out.push_back(static_cast<char>(0xF0 | ((codepoint >> 18) & 0x07)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 12) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3F)));
    }
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_kgtts_app_audio_EspeakNative_nativeInit(
        JNIEnv *env,
        jobject /* thiz */,
        jstring data_path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    std::string path = jstring_to_utf8(env, data_path);
    if (path.empty()) {
        g_initialized = false;
        return JNI_FALSE;
    }
    int result = espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, 0, path.c_str(), 0);
    if (result <= 0) {
        g_initialized = false;
        return JNI_FALSE;
    }
    g_initialized = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_kgtts_app_audio_EspeakNative_nativePhonemize(
        JNIEnv *env,
        jobject /* thiz */,
        jstring text,
        jstring voice) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_initialized) {
        return env->NewStringUTF("");
    }

    std::string text_utf8 = jstring_to_utf8(env, text);
    if (text_utf8.empty()) {
        return env->NewStringUTF("");
    }

    std::string voice_utf8 = jstring_to_utf8(env, voice);
    if (voice_utf8.empty()) {
        voice_utf8 = "en-us";
    }

    piper::eSpeakPhonemeConfig config;
    config.voice = voice_utf8;
    config.keepLanguageFlags = false;

    std::vector<std::vector<piper::Phoneme>> sentences;
    try {
        piper::phonemize_eSpeak(text_utf8, config, sentences);
    } catch (...) {
        return env->NewStringUTF("");
    }

    std::string out;
    for (size_t i = 0; i < sentences.size(); ++i) {
        for (auto phoneme : sentences[i]) {
            append_utf8(out, phoneme);
        }
        if ((i + 1) < sentences.size()) {
            append_utf8(out, config.space);
        }
    }

    return env->NewStringUTF(out.c_str());
}
