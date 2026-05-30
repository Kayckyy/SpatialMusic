#pragma once
#include <oboe/Oboe.h>
#include <memory>
#include <atomic>
#include <functional>
#include "ConvolutionEngine.h"

// Callback chamado quando o player precisa de mais amostras PCM
// Retorna false quando acabou o áudio
using AudioSourceCallback = std::function<bool(float* left, float* right, int frames)>;

class AudioPlayer : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    explicit AudioPlayer(std::shared_ptr<ConvolutionEngine> engine);
    ~AudioPlayer();

    bool start(AudioSourceCallback source, int sample_rate = 44100);
    void stop();
    bool isPlaying() const { return _playing.load(); }

    // Troca o engine em tempo real (mudança de azimute/crossfeed)
    void updateEngine(std::shared_ptr<ConvolutionEngine> engine) { _engine = std::move(engine); }

    // oboe callbacks
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audio_data,
        int32_t num_frames) override;

    void onErrorAfterClose(
        oboe::AudioStream* stream,
        oboe::Result error) override;

private:
    std::shared_ptr<ConvolutionEngine> _engine;
    std::shared_ptr<oboe::AudioStream> _stream;
    AudioSourceCallback _source;
    std::atomic<bool> _playing{false};

    // buffers pré-alocados — sem alloc no callback de áudio
    std::vector<float> _in_l, _in_r, _out_l, _out_r;

    static constexpr int BLOCK = 2048;

    bool openStream(int sample_rate);
};
