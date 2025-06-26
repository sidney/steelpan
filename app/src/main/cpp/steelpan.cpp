#include <jni.h>
#include <aaudio/AAudio.h>
#include <android/log.h>
#include <cmath>
#include <memory>
#include <atomic>
#include <thread>
#include <chrono>

#define LOG_TAG "SteelpanNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class SteelpanEngine {
private:
    AAudioStream* stream = nullptr;
    std::atomic<float> currentFreq{0.0f};
    std::atomic<float> amplitude{0.0f};
    float phase = 0.0f;
    float sampleRate = 48000.0f;
    static constexpr int32_t kChannelCount = 1;
    
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
        
        LOGI("Audio engine started successfully");
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
        currentFreq.store(frequency);
        amplitude.store(0.3f);
        
        // Fade out after a short duration
        std::thread([this]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            for (int i = 0; i < 100; ++i) {
                float currentAmp = amplitude.load();
                amplitude.store(currentAmp * 0.95f);
                std::this_thread::sleep_for(std::chrono::milliseconds(20));
                if (currentAmp < 0.001f) break;
            }
        }).detach();
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
        float freq = currentFreq.load();
        float amp = amplitude.load();
        
        for (int32_t i = 0; i < numFrames; ++i) {
            if (amp < 0.001f) {
                audioData[i] = 0.0f;
                continue;
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
            
            // Apply gentle low-pass filtering for warmth
            static float prevSample = 0.0f;
            sample = sample * 0.7f + prevSample * 0.3f;
            prevSample = sample;
            
            audioData[i] = sample;
            
            // Update phase
            phase += 2.0f * static_cast<float>(M_PI) * freq / sampleRate;
            if (phase >= 2.0f * static_cast<float>(M_PI)) {
                phase -= 2.0f * static_cast<float>(M_PI);
            }
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
