#include <jni.h>
#include <string>
#include <memory>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "ConvolutionEngine.h"
#include "IrLoader.h"
#include "AudioPlayer.h"

// Estado global da sessão
struct OpenMindSession {
    std::shared_ptr<ConvolutionEngine> engine;
    std::shared_ptr<AudioPlayer>       player;
    std::string                        hrtf_dir;
    float azimuth   = 90.f;
    float elevation = 0.f;
    float crossfeed = 0.08f;
    float input_gain = 0.35f;
    int   block_size = 2048;
    std::atomic<float> progress{0.f};
};

static std::unique_ptr<OpenMindSession> gSession;

static ConvolutionEngine* buildEngine(OpenMindSession& s) {
    IrLoader loader(s.hrtf_dir);
    auto ir_l = loader.loadByAngle(s.azimuth,         s.elevation);
    auto ir_r = loader.loadByAngle(360.f - s.azimuth, s.elevation);

    // True-Stereo: direto + crossfeed atenuado
    std::vector<float> lr = ir_r.left,  rl = ir_l.right;
    for (auto& v : lr) v *= s.crossfeed;
    for (auto& v : rl) v *= s.crossfeed;

    return new ConvolutionEngine(ir_l.left, lr, rl, ir_r.right);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_openmind_engine_HrtfEngine_init(
    JNIEnv* env, jobject,
    jstring hrtfDir, jfloat az, jfloat el, jfloat cf, jfloat gain)
{
    gSession = std::make_unique<OpenMindSession>();
    gSession->hrtf_dir   = env->GetStringUTFChars(hrtfDir, nullptr);
    gSession->azimuth    = az;
    gSession->elevation  = el;
    gSession->crossfeed  = cf;
    gSession->input_gain = gain;

    gSession->engine.reset(buildEngine(*gSession));
    gSession->player = std::make_shared<AudioPlayer>(gSession->engine);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_openmind_engine_HrtfEngine_play(JNIEnv* env, jobject, jstring path)
{
    if (!gSession) return JNI_FALSE;
    const char* cpath = env->GetStringUTFChars(path, nullptr);

    // Fonte de áudio: lê arquivo PCM via ffmpeg (simplificado)
    // Em produção substituir por MediaCodec NDK
    gSession->progress.store(0.f);
    gSession->player->start([cpath, &s = *gSession](float* l, float* r, int n) -> bool {
        // TODO: integrar MediaCodec decoder aqui
        // Por ora retorna silêncio
        std::fill(l, l+n, 0.f);
        std::fill(r, r+n, 0.f);
        return false;
    }, 44100);

    env->ReleaseStringUTFChars(path, cpath);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_pause(JNIEnv*, jobject)
{ if (gSession) gSession->player->stop(); }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_resume(JNIEnv*, jobject)
{ /* TODO: retomar do ponto pausado */ }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_stop(JNIEnv*, jobject)
{ if (gSession) gSession->player->stop(); }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_seek(JNIEnv*, jobject, jfloat progress)
{ if (gSession) gSession->progress.store(progress); }

JNIEXPORT jfloat JNICALL
Java_com_openmind_engine_HrtfEngine_getProgress(JNIEnv*, jobject)
{ return gSession ? gSession->progress.load() : 0.f; }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_setAzimuth(JNIEnv*, jobject, jfloat az)
{
    if (!gSession) return;
    gSession->azimuth = az;
    gSession->engine.reset(buildEngine(*gSession));
    gSession->player->updateEngine(gSession->engine);
}

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_setElevation(JNIEnv*, jobject, jfloat el)
{
    if (!gSession) return;
    gSession->elevation = el;
    gSession->engine.reset(buildEngine(*gSession));
    gSession->player->updateEngine(gSession->engine);
}

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_setCrossfeed(JNIEnv*, jobject, jfloat cf)
{
    if (!gSession) return;
    gSession->crossfeed = cf;
    gSession->engine.reset(buildEngine(*gSession));
    gSession->player->updateEngine(gSession->engine);
}

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_setInputGain(JNIEnv*, jobject, jfloat gain)
{ if (gSession) gSession->input_gain = gain; }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_setBlockSize(JNIEnv*, jobject, jint size)
{ if (gSession) gSession->block_size = size; }

JNIEXPORT void JNICALL
Java_com_openmind_engine_HrtfEngine_release(JNIEnv*, jobject)
{
    if (gSession) {
        gSession->player->stop();
        gSession.reset();
    }
}

} // extern "C"
