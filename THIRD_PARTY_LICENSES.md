# Third-Party Licenses

This repository bundles or depends on multiple third-party components.
This document lists the main runtime/build-time components that are included in the project tree or release artifacts.

## 1) Piper (training/inference toolchain)
- Upstream: https://github.com/rhasspy/piper
- License: MIT
- Local paths:
  - `pc_trainer/third_party/piper/LICENSE.md`
  - `pc_trainer/piper_env/Lib/site-packages/piper_train-1.0.0.dist-info/METADATA`

## 2) piper-phonemize (Android native dependency subtree)
- Upstream: https://github.com/rhasspy/piper-phonemize
- License: MIT
- Local path:
  - `android-app/app/src/main/cpp/piper-phonemize/LICENSE.md`

## 3) eSpeak NG (phonemization runtime/tooling)
- Upstream: https://github.com/espeak-ng/espeak-ng
- License: GPL v3 or later (per upstream README)
- Local bundled artifacts include:
  - `pc_trainer/tools/espeak-ng/**`
  - `pc_trainer/resources_pack/tools/espeak-ng/**`
  - `android-app/app/src/main/jniLibs/arm64-v8a/libespeak-ng.so`
  - `android-app/app/src/main/assets/espeak-ng-data.zip`

## 4) sherpa-onnx (offline ASR runtime)
- Upstream: https://github.com/k2-fsa/sherpa-onnx
- License: see packaged license file
- Local paths:
  - `pc_trainer/piper_env/Lib/site-packages/sherpa_onnx-1.10.46.dist-info/LICENSE`
  - `android-app/app/libs/sherpa-onnx-*.aar` (license in upstream/package docs)

## 5) ONNX Runtime
- Upstream: https://github.com/microsoft/onnxruntime
- License: MIT
- Local paths:
  - `pc_trainer/piper_env/Lib/site-packages/onnxruntime/LICENSE`
  - `pc_trainer/resources_pack/piper_env/Lib/site-packages/onnxruntime/LICENSE`

## 6) Python runtime dependencies
`pc_trainer/piper_env` and `pc_trainer/resources_pack/piper_env` contain many Python packages with their own licenses.
Representative license files are under:
- `*/site-packages/*dist-info/licenses/*`
- `*/site-packages/*dist-info/LICENSE*`

When redistributing binaries, keep these license files intact.

---

If you find a missing third-party entry, open an issue/PR and include:
- component name and version
- upstream URL
- license type
- path to local license file
