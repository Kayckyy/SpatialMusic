#include "AudioPlayer.h"
#include <android/log.h>

#define LOG_TAG "OpenMind"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioPlayer::AudioPlayer(std::shared_ptr<ConvolutionEngine> engine)
    : _engine(std::move(engine))
{
    // pré-aloca buffers — zero alloc no callback de áudio
    _in_l.resize(BLOCK);  _in_r.resize(BLOCK);
    _out_l.resize(BLOCK); _out_r.resize(BLOCK);
}

AudioPlayer::~AudioPlayer() {
    stop();
}

bool AudioPlayer::openStream(int sample_rate) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           .setPerformanceMode(oboe::PerformanceMode::LowLatency)
           .setSharingMode(oboe::SharingMode::Exclusive)
           .setFormat(oboe::AudioFormat::Float)
           .setChannelCount(oboe::ChannelCount::Stereo)
           .setSampleRate(sample_rate)
           .setFramesPerDataCallback(BLOCK)
           .setDataCallback(this)
           .setErrorCallback(this);

    oboe::Result result = builder.openStream(_stream);
    if (result != oboe::Result::OK) {
        LOGE("Falha ao abrir stream: %s", oboe::convertToText(result));
        return false;
    }

    LOGI("Stream aberto: SR=%d latência=%dms",
         _stream->getSampleRate(),
         (int)(_stream->getBufferSizeInFrames() * 1000 / _stream->getSampleRate()));

    return true;
}

bool AudioPlayer::start(AudioSourceCallback source, int sample_rate) {
    if (_playing.load()) stop();

    _source = std::move(source);
    _engine->reset();

    if (!openStream(sample_rate)) return false;

    oboe::Result result = _stream->start();
    if (result != oboe::Result::OK) {
        LOGE("Falha ao iniciar stream: %s", oboe::convertToText(result));
        return false;
    }

    _playing.store(true);
    LOGI("Reprodução iniciada");
    return true;
}

void AudioPlayer::stop() {
    _playing.store(false);
    if (_stream) {
        _stream->stop();
        _stream->close();
        _stream.reset();
    }
    LOGI("Reprodução encerrada");
}

oboe::DataCallbackResult AudioPlayer::onAudioReady(
    oboe::AudioStream* stream,
    void* audio_data,
    int32_t num_frames)
{
    auto* out = static_cast<float*>(audio_data);
    int frames_left = num_frames;
    int offset      = 0;

    while (frames_left > 0) {
        int block = std::min(frames_left, BLOCK);

        // pede amostras pra fonte (MediaCodec decoder, file reader, etc.)
        bool has_more = _source(_in_l.data(), _in_r.data(), block);

        // processa HRTF
        _engine->process(
            _in_l.data(), _in_r.data(),
            _out_l.data(), _out_r.data(),
            block
        );

        // interleave L R L R no buffer de saída do Oboe
        for (int i = 0; i < block; i++) {
            out[(offset + i) * 2]     = _out_l[i];
            out[(offset + i) * 2 + 1] = _out_r[i];
        }

        offset      += block;
        frames_left -= block;

        if (!has_more) {
            // preenche silêncio no resto do buffer e encerra
            for (int i = offset * 2; i < num_frames * 2; i++) out[i] = 0.f;
            _playing.store(false);
            return oboe::DataCallbackResult::Stop;
        }
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioPlayer::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("Erro no stream: %s", oboe::convertToText(error));
    _playing.store(false);

    // tenta reabrir (ex: fone desconectado e reconectado)
    if (error == oboe::Result::ErrorDisconnected) {
        LOGI("Dispositivo desconectado — tentando reabrir");
        if (_source && openStream(stream->getSampleRate())) {
            _stream->start();
            _playing.store(true);
        }
    }
}
