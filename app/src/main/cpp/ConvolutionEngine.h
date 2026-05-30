#pragma once
#include <vector>
#include <unordered_map>
#include <complex>
#include <cstdint>

class ConvolutionEngine {
public:
    // 4 IRs: ll, lr, rl, rr
    ConvolutionEngine(
        const std::vector<float>& ir_ll,
        const std::vector<float>& ir_lr,
        const std::vector<float>& ir_rl,
        const std::vector<float>& ir_rr,
        int sample_rate = 44100
    );

    // Processa um bloco stereo in-place
    // in_l/in_r: entrada, out_l/out_r: saída, n: número de frames
    void process(
        const float* in_l, const float* in_r,
        float* out_l,       float* out_r,
        int n
    );

    void reset();

private:
    struct IrData {
        std::vector<float> ir;
        std::vector<float> overlap;  // tail do bloco anterior
    };

    // cache: fft_size → {ll, lr, rl, rr} FFTs
    struct IrFfts {
        std::vector<std::complex<float>> ll, lr, rl, rr;
    };

    IrData _ll, _lr, _rl, _rr;
    int _ir_len;
    int _sample_rate;

    std::unordered_map<int, IrFfts> _fft_cache;

    // FFT/IFFT helpers
    static int nextPow2(int n);
    static void rfft(const std::vector<float>& in, int fft_size,
                     std::vector<std::complex<float>>& out);
    static void irfft(const std::vector<std::complex<float>>& in, int fft_size,
                      std::vector<float>& out);

    const IrFfts& getIrFfts(int fft_size);

    void convolveBlock(
        const float* signal, int sig_len,
        const std::vector<std::complex<float>>& ir_fft,
        const std::vector<float>& ir,
        std::vector<float>& overlap,
        float* out
    );
};
