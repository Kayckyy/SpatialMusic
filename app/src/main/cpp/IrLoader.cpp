#include "IrLoader.h"
#include <fstream>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <sstream>
#include <iomanip>

// ---------------------------------------------------------------------------
// Parser WAV mínimo — suporta PCM16 e IEEE Float32, mono e stereo
// Sem dependência de libsndfile
// ---------------------------------------------------------------------------

struct WavHeader {
    uint32_t chunk_size;
    uint16_t audio_format;   // 1=PCM, 3=IEEE float
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;
    uint16_t block_align;
    uint16_t bits_per_sample;
    uint32_t data_size;
};

static bool parseWav(const std::string& path,
                     std::vector<float>& left,
                     std::vector<float>& right,
                     int& sample_rate)
{
    std::ifstream f(path, std::ios::binary);
    if (!f) return false;

    // RIFF header
    char riff[4]; f.read(riff, 4);
    if (std::strncmp(riff, "RIFF", 4) != 0) return false;

    uint32_t chunk_size; f.read((char*)&chunk_size, 4);

    char wave[4]; f.read(wave, 4);
    if (std::strncmp(wave, "WAVE", 4) != 0) return false;

    WavHeader hdr{}; bool got_fmt = false; bool got_data = false;
    uint32_t data_offset = 0;

    // percorre chunks até achar fmt e data
    while (f && !(got_fmt && got_data)) {
        char id[4]; f.read(id, 4);
        uint32_t size; f.read((char*)&size, 4);
        if (!f) break;

        if (std::strncmp(id, "fmt ", 4) == 0) {
            f.read((char*)&hdr.audio_format,   2);
            f.read((char*)&hdr.num_channels,   2);
            f.read((char*)&hdr.sample_rate,    4);
            f.read((char*)&hdr.byte_rate,      4);
            f.read((char*)&hdr.block_align,    2);
            f.read((char*)&hdr.bits_per_sample,2);
            if (size > 16) f.seekg(size - 16, std::ios::cur);
            got_fmt = true;
        } else if (std::strncmp(id, "data", 4) == 0) {
            hdr.data_size = size;
            data_offset   = f.tellg();
            got_data      = true;
        } else {
            f.seekg(size, std::ios::cur);
        }
    }

    if (!got_fmt || !got_data) return false;
    if (hdr.num_channels < 1 || hdr.num_channels > 2) return false;

    sample_rate = hdr.sample_rate;
    f.seekg(data_offset, std::ios::beg);

    int bytes_per_sample = hdr.bits_per_sample / 8;
    int num_frames = hdr.data_size / (bytes_per_sample * hdr.num_channels);

    left.resize(num_frames);
    right.resize(num_frames);

    for (int i = 0; i < num_frames; i++) {
        float l = 0.f, r = 0.f;

        if (hdr.audio_format == 3 && hdr.bits_per_sample == 32) {
            // IEEE float32
            f.read((char*)&l, 4);
            if (hdr.num_channels == 2) f.read((char*)&r, 4);
            else r = l;
        } else if (hdr.audio_format == 1 && hdr.bits_per_sample == 16) {
            // PCM16
            int16_t sl, sr = 0;
            f.read((char*)&sl, 2);
            if (hdr.num_channels == 2) f.read((char*)&sr, 2);
            else sr = sl;
            l = sl / 32768.f;
            r = sr / 32768.f;
        } else if (hdr.audio_format == 1 && hdr.bits_per_sample == 24) {
            // PCM24
            uint8_t buf[3];
            f.read((char*)buf, 3);
            int32_t sl = (buf[2] << 16) | (buf[1] << 8) | buf[0];
            if (sl & 0x800000) sl |= 0xFF000000;
            int32_t sr = sl;
            if (hdr.num_channels == 2) {
                f.read((char*)buf, 3);
                sr = (buf[2] << 16) | (buf[1] << 8) | buf[0];
                if (sr & 0x800000) sr |= 0xFF000000;
            }
            l = sl / 8388608.f;
            r = sr / 8388608.f;
        } else {
            return false;  // formato não suportado
        }

        left[i]  = l;
        right[i] = r;
    }

    return true;
}

// ---------------------------------------------------------------------------

IrLoader::IrLoader(const std::string& hrtf_dir) : _hrtf_dir(hrtf_dir) {}

// Mesmo padrão de nome do Python: azi_90,0_ele_0,0.wav
std::string IrLoader::angleToFilename(float az, float el) {
    auto fmt = [](float v) {
        std::ostringstream ss;
        ss << std::fixed << std::setprecision(1) << v;
        std::string s = ss.str();
        // substitui '.' por ','
        std::replace(s.begin(), s.end(), '.', ',');
        return s;
    };
    return "azi_" + fmt(az) + "_ele_" + fmt(el) + ".wav";
}

// Mapeia azimute para as 4 IRs disponíveis
std::string IrLoader::nearestAvailable(float az) {
    az = std::fmod(az, 360.f);
    if (az < 0) az += 360.f;

    const float candidates[] = {0.f, 90.f, 180.f, 270.f};
    float best_dist = 999.f;
    float best_az   = 0.f;

    for (float c : candidates) {
        float d = std::abs(c - az);
        if (d > 180.f) d = 360.f - d;
        if (d < best_dist) { best_dist = d; best_az = c; }
    }

    return angleToFilename(best_az, 0.f);
}

void IrLoader::normalize(std::vector<float>& l, std::vector<float>& r) {
    float peak = 0.f;
    for (float v : l) peak = std::max(peak, std::abs(v));
    for (float v : r) peak = std::max(peak, std::abs(v));
    if (peak > 0.f) {
        for (float& v : l) v /= peak;
        for (float& v : r) v /= peak;
    }
}

IrData IrLoader::load(const std::string& filename) const {
    std::string path = _hrtf_dir + "/" + filename;
    IrData result;
    if (!parseWav(path, result.left, result.right, result.sample_rate))
        throw std::runtime_error("IrLoader: falha ao ler " + path);
    normalize(result.left, result.right);
    return result;
}

IrData IrLoader::loadByAngle(float az, float el) const {
    return load(nearestAvailable(az));
}
