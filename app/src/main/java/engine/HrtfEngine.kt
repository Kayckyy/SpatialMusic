package com.openmind.engine

// JNI bridge para o engine C++ (ConvolutionEngine + AudioPlayer + IrLoader)
class HrtfEngine {

    external fun init(
        hrtfDir: String,
        azimuth: Float,
        elevation: Float,
        crossfeed: Float,
        inputGain: Float,
    ): Boolean

    external fun play(path: String): Boolean
    external fun pause()
    external fun resume()
    external fun stop()
    external fun seek(progress: Float)
    external fun getProgress(): Float

    external fun setAzimuth(az: Float)
    external fun setElevation(el: Float)
    external fun setCrossfeed(cf: Float)
    external fun setInputGain(gain: Float)
    external fun setBlockSize(size: Int)

    external fun release()
}
