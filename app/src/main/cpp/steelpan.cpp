#include <jni.h>
#include <aaudio/AAudio.h>
#include <android/log.h>
#include <cmath>
#include <memory>
#include <atomic>
#include <thread>
#include <chrono>
#include <array>

#define LOG_TAG "SteelpanNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class SteelpanVoice {
public:
    std::atomic<float> frequency{0.0f};
    std::atomic<float> amplitude{0.0f};
    std::atomic<bool> active{false};
    float phase = 0.0f;

    void trigger(float freq) {
        frequency.store(freq);
        amplitude.store(0.3f);
        active.store(true);
        phase = 0.0f;

        // Fade out after a short duration
        std::thread([this]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            for (int i = 0; i < 100; ++i) {
                float currentAmp = amplitude.load();
                if (currentAmp < 0.001f) {
                    active.store(false);
                    break;
                }
                amplitude.store(currentAmp * 0.95f);
                std::this_thread::sleep_for(std::chrono::milliseconds(20));
            }
            active.store(false);
        }).detach();
    }

    float generateSample(float sampleRate) {
        if (!active.load()) return 0.0f;

        float freq = frequency.load();
        float amp = amplitude.load();

        if (amp < 0.001f) {
            active.store(false);
            return 0.0f;
        }

        // Generate steelpan-like sound using multiple harmonics
        float sample = 0.0f;

        // Fundamental frequency
        sample += std::sin(phase) * 0.6f;

        // Add harmonics for steelpan timbre
        sample += std::sin(phase * 2.0f) * 0.3f;
        sample += std::sin(phase * 3.0f) * 0.15f;
        sample += std::sin(phase * 4.0f) * 0.08f;

        // Apply amplitude envelope
        sample *= amp;

        // Update phase
        phase += 2.0f * static_cast<float>(M_PI) * freq / sampleRate;
        if (phase >= 2.0f * static_cast<float>(M_PI)) {
            phase -= 2.0f * static_cast<float>(M_PI);
        }

        return sample;
    }
};

class SteelpanEngine {
private:
    AAudioStream* stream = nullptr;
    static constexpr int32_t kMaxVoices = 8;  // Support up to 8 simultaneous notes
    std::array<SteelpanVoice, kMaxVoices> voices;
    std::atomic<int> nextVoiceIndex{0};
    float sampleRate = 48000.0f;
    static constexpr int32_t kChannelCount = 1;

    // Simple low-pass filter state
    float prevSample = 0.0f;

public:
    bool start() {
        AAudioStreamBuilder* builder = nullptr;
        aaudio_result_t result = AAudio_createStreamBuilder(&builder);
        if (result != AAUDIO_OK) {
            LOGI("Failed to create stream builder: %s", AAudio_convertResultToText(result));
            return false;
        }

        AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
        AAudioStreamBuilder_setSampleRate(builder, static_cast<int32_t>(sampleRate));
        AAudioStreamBuilder_setChannelCount(builder, kChannelCount);
        AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setDataCallback(builder, dataCallback, this);

        result = AAudioStreamBuilder_openStream(builder, &stream);
        AAudioStreamBuilder_delete(builder);

        if (result != AAUDIO_OK) {
            LOGI("Failed to open stream: %s", AAudio_convertResultToText(result));
            return false;
        }

        sampleRate = static_cast<float>(AAudioStream_getSampleRate(stream));
        LOGI("Stream opened with sample rate: %.0f", sampleRate);

        result = AAudioStream_requestStart(stream);
        if (result != AAUDIO_OK) {
            LOGI("Failed to start stream: %s", AAudio_convertResultToText(result));
            return false;
        }

        LOGI("Audio engine started successfully with %d voices", kMaxVoices);
        return true;
    }

    void stop() {
        if (stream != nullptr) {
            AAudioStream_requestStop(stream);
            AAudioStream_close(stream);
            stream = nullptr;
            LOGI("Audio engine stopped");
        }
    }

    void playNote(float frequency) {
        // Find an available voice or use the next one in round-robin fashion
        int voiceIndex = -1;

        // First try to find an inactive voice
        for (int i = 0; i < kMaxVoices; ++i) {
            if (!voices[i].active.load()) {
                voiceIndex = i;
                break;
            }
        }

        // If no inactive voice found, use round-robin allocation
        if (voiceIndex == -1) {
            voiceIndex = nextVoiceIndex.load();
            nextVoiceIndex.store((voiceIndex + 1) % kMaxVoices);
        }

        // Trigger the selected voice
        voices[voiceIndex].trigger(frequency);

        LOGI("Playing note %.1f Hz on voice %d", frequency, voiceIndex);
    }

private:
    static aaudio_data_callback_result_t dataCallback(
            AAudioStream *stream,
            void *userData,
            void *audioData,
            int32_t numFrames) {

        auto* engine = static_cast<SteelpanEngine*>(userData);
        return engine->processAudio(static_cast<float*>(audioData), numFrames);
    }

    aaudio_data_callback_result_t processAudio(float* audioData, int32_t numFrames) {
        for (int32_t i = 0; i < numFrames; ++i) {
            float mixedSample = 0.0f;

            // Mix all active voices
            for (int v = 0; v < kMaxVoices; ++v) {
                mixedSample += voices[v].generateSample(sampleRate);
            }

            // Apply gentle low-pass filtering for warmth
            mixedSample = mixedSample * 0.7f + prevSample * 0.3f;
            prevSample = mixedSample;

            // Apply soft limiting to prevent clipping when multiple voices play
            if (mixedSample > 0.8f) mixedSample = 0.8f;
            else if (mixedSample < -0.8f) mixedSample = -0.8f;

            audioData[i] = mixedSample;
        }

        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }
};

static std::unique_ptr<SteelpanEngine> engine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_steelpan_MainActivity_initializeAudio(JNIEnv *env, jobject thiz) {
    engine = std::make_unique<SteelpanEngine>();
    if (!engine->start()) {
        LOGI("Failed to start audio engine");
    }
}

JNIEXPORT void JNICALL
Java_com_example_steelpan_MainActivity_destroyAudio(JNIEnv *env, jobject thiz) {
    if (engine) {
        engine->stop();
        engine.reset();
    }
}

JNIEXPORT void JNICALL
Java_com_example_steelpan_MainActivity_playNote(JNIEnv *env, jobject thiz, jfloat frequency) {
    if (engine) {
        engine->playNote(frequency);
    }
}

}