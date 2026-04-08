# Third-Party Licenses

This repository includes or depends on multiple third-party components.

## 1) Piper (training/inference toolchain)
- Local vendored snapshot source: https://github.com/rhasspy/piper
- Active upstream location: https://github.com/OHF-Voice/piper1-gpl
- Local license file: `pc_trainer/third_party/piper/LICENSE.md` (MIT for vendored snapshot)

## 2) piper-phonemize (Android native subtree)
- Source: https://github.com/rhasspy/piper-phonemize
- Local license file: `android-app/app/src/main/cpp/piper-phonemize/LICENSE.md` (MIT)

## 3) eSpeak NG
- Source: https://github.com/espeak-ng/espeak-ng
- Upstream license family: GPL v3+
- Local bundled artifacts include:
  - `pc_trainer/tools/espeak-ng/**`
  - `pc_trainer/resources_pack/tools/espeak-ng/**`
  - `android-app/app/src/main/jniLibs/arm64-v8a/libespeak-ng.so`
  - `android-app/app/src/main/assets/espeak-ng-data.zip`

## 4) sherpa-onnx
- Source: https://github.com/k2-fsa/sherpa-onnx
- Android AAR path: `android-app/app/libs/sherpa-onnx-*.aar`
- Python dist-info license path:
  - `pc_trainer/piper_env/Lib/site-packages/sherpa_onnx-*.dist-info/LICENSE`

## 5) ONNX Runtime
- Source: https://github.com/microsoft/onnxruntime
- License: MIT
- Local license paths:
  - `pc_trainer/piper_env/Lib/site-packages/onnxruntime/LICENSE`
  - `pc_trainer/resources_pack/piper_env/Lib/site-packages/onnxruntime/LICENSE`

## 6) Python dependency tree
`pc_trainer/piper_env` and `pc_trainer/resources_pack/piper_env` include many Python packages with their own licenses.

Representative locations:
- `*/site-packages/*dist-info/licenses/*`
- `*/site-packages/*dist-info/LICENSE*`

---

If a third-party component is missing in this list, please add:
- component name/version
- upstream URL
- license type
- local license file path
