#include "ConvolutionEngine.h"
#include <cmath>
#include <algorithm>
#include <stdexcept>
#include <cassert>

// ---------------------------------------------------------------------------
// FFT de Cooley-Tukey sem dependência externa
// Suficiente pra IRs de até ~8192 samples (SADIE II tem ~256 samples)
// ---------------------------------------------------------------------------

static void fft_inplace(std::vector<std::complex<float>>& x) {
    int n = x.size();
    // bit-reversal
    for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(x[i], x[j]);
    }
    // butterfly
    for (int len = 2; len <= n; len <<= 1) {
        float ang = -2.0f * M_PI / len;
        std::complex<float> wlen(std::cos(ang), std::sin(ang));
        for (int i = 0; i < n; i += len) {
            std::complex<float> w(1.0f, 0.0f);
            for (int j = 0; j < len / 2; j++) {
                auto u = x[i+j], v = x[i+j+len/2] * w;
                x[i+j]         = u + v;
                x[i+j+len/2]   = u - v;
                w *= wlen;
            }
        }
    }
}

static void ifft_inplace(std::vector<std::complex<float>>& x) {
    // IFFT = conjugar, FFT, conjugar, dividir por N
    for (auto& v : x) v = std::conj(v);
    fft_inplace(x);
    for (auto& v : x) v = std::conj(v);
    float n = x.size();
    for (auto& v : x) v /= n;
}

// ---------------------------------------------------------------------------

int ConvolutionEngine::nextPow2(int n) {
    int p = 1;
    while (p < n) p <<= 1;
    return p;
}

void ConvolutionEngine::rfft(const std::vector<float>& in, int fft_size,
                              std::vector<std::complex<float>>& out) {
    std::vector<std::complex<float>> x(fft_size, {0.f, 0.f});
    int copy_len = std::min((int)in.size(), fft_size);
    for (int i = 0; i < copy_len; i++) x[i] = {in[i], 0.f};
    fft_inplace(x);
    out.assign(x.begin(), x.begin() + fft_size / 2 + 1);
}

void ConvolutionEngine::irfft(const std::vector<std::complex<float>>& in,
                               int fft_size, std::vector<float>& out) {
    // reconstrói espectro completo (simetria hermitiana)
    std::vector<std::complex<float>> x(fft_size);
    x[0] = in[0];
    for (int i = 1; i < fft_size / 2; i++) {
        x[i]             = in[i];
        x[fft_size - i]  = std::conj(in[i]);
    }
    x[fft_size / 2] = in[fft_size / 2];
    ifft_inplace(x);
    out.resize(fft_size);
    for (int i = 0; i < fft_size; i++) out[i] = x[i].real();
}

// ---------------------------------------------------------------------------

ConvolutionEngine::ConvolutionEngine(
    const std::vector<float>& ir_ll,
    const std::vector<float>& ir_lr,
    const std::vector<float>& ir_rl,
    const std::vector<float>& ir_rr,
    int sample_rate)
    : _sample_rate(sample_rate)
{
    _ll.ir = ir_ll; _lr.ir = ir_lr;
    _rl.ir = ir_rl; _rr.ir = ir_rr;

    _ir_len = 0;
    for (auto* d : {&_ll, &_lr, &_rl, &_rr})
        _ir_len = std::max(_ir_len, (int)d->ir.size());

    reset();
}

void ConvolutionEngine::reset() {
    int ov_len = _ir_len - 1;
    for (auto* d : {&_ll, &_lr, &_rl, &_rr})
        d->overlap.assign(ov_len, 0.f);
}

const ConvolutionEngine::IrFfts& ConvolutionEngine::getIrFfts(int fft_size) {
    auto it = _fft_cache.find(fft_size);
    if (it != _fft_cache.end()) return it->second;

    IrFfts ffts;
    rfft(_ll.ir, fft_size, ffts.ll);
    rfft(_lr.ir, fft_size, ffts.lr);
    rfft(_rl.ir, fft_size, ffts.rl);
    rfft(_rr.ir, fft_size, ffts.rr);
    _fft_cache[fft_size] = std::move(ffts);
    return _fft_cache[fft_size];
}

void ConvolutionEngine::convolveBlock(
    const float* signal, int sig_len,
    const std::vector<std::complex<float>>& ir_fft,
    const std::vector<float>& ir,
    std::vector<float>& overlap,
    float* out)
{
    int fft_size = nextPow2(sig_len + (int)ir.size() - 1);

    // FFT do sinal
    std::vector<std::complex<float>> sig_fft;
    {
        std::vector<float> sig_padded(signal, signal + sig_len);
        rfft(sig_padded, fft_size, sig_fft);
    }

    // multiplicação no domínio da frequência
    std::vector<std::complex<float>> prod(sig_fft.size());
    for (int i = 0; i < (int)sig_fft.size(); i++)
        prod[i] = sig_fft[i] * ir_fft[i];

    // IFFT
    std::vector<float> conv;
    irfft(prod, fft_size, conv);

    int conv_len = sig_len + (int)ir.size() - 1;
    int ov_len   = (int)overlap.size();

    // overlap-add
    for (int i = 0; i < ov_len && i < conv_len; i++)
        conv[i] += overlap[i];

    // salva tail
    for (int i = 0; i < ov_len; i++)
        overlap[i] = (sig_len + i < conv_len) ? conv[sig_len + i] : 0.f;

    // copia saída
    for (int i = 0; i < sig_len; i++)
        out[i] = conv[i];
}

void ConvolutionEngine::process(
    const float* in_l, const float* in_r,
    float* out_l,       float* out_r,
    int n)
{
    int fft_size = nextPow2(n + _ir_len - 1);
    const auto& ffts = getIrFfts(fft_size);

    const float GAIN = 0.35f;
    std::vector<float> gl(n), gr(n);
    for (int i = 0; i < n; i++) { gl[i] = in_l[i] * GAIN; gr[i] = in_r[i] * GAIN; }

    // True-Stereo: 4 convoluções
    std::vector<float> ll(n), lr(n), rl(n), rr(n);
    convolveBlock(gl.data(), n, ffts.ll, _ll.ir, _ll.overlap, ll.data());
    convolveBlock(gl.data(), n, ffts.lr, _lr.ir, _lr.overlap, lr.data());
    convolveBlock(gr.data(), n, ffts.rl, _rl.ir, _rl.overlap, rl.data());
    convolveBlock(gr.data(), n, ffts.rr, _rr.ir, _rr.overlap, rr.data());

    for (int i = 0; i < n; i++) {
        out_l[i] = std::max(-1.f, std::min(1.f, ll[i] + rl[i]));
        out_r[i] = std::max(-1.f, std::min(1.f, rr[i] + lr[i]));
    }
}
