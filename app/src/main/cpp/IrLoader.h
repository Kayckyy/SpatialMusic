#pragma once
#include <vector>
#include <string>
#include <stdexcept>

struct IrData {
    std::vector<float> left;
    std::vector<float> right;
    int sample_rate;
};

class IrLoader {
public:
    explicit IrLoader(const std::string& hrtf_dir);

    // Carrega IR stereo WAV e retorna left/right normalizados
    IrData load(const std::string& filename) const;

    // Conveniência: carrega IR de azimute/elevação
    // az em graus (0=frente, 90=esquerda, 270=direita)
    IrData loadByAngle(float az, float el = 0.0f) const;

private:
    std::string _hrtf_dir;

    static std::string angleToFilename(float az, float el);
    static std::string nearestAvailable(float az);
    static void normalize(std::vector<float>& l, std::vector<float>& r);
};
