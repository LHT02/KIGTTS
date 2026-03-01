import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import tkinter as tk
import unicodedata
import wave
import zipfile
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

try:
    from tkinterdnd2 import DND_FILES, TkinterDnD
    BaseTk = TkinterDnD.Tk  # type: ignore
except ImportError:  # graceful fallback
    BaseTk = tk.Tk  # type: ignore
    DND_FILES = None

from engine import ProjectPaths, TrainingOptions, run_pipeline
from engine.logger import setup_logger


class TrainerGUI(BaseTk):  # type: ignore[misc]
    def __init__(self) -> None:
        super().__init__()
        self.title("KGTTS 训练器")
        self.geometry("960x720")

        self.audio_files: list[Path] = []
        self.output_dir = tk.StringVar(value=str(Path.cwd() / "projects" / "demo"))
        self.quality = tk.StringVar(value="A")
        self.denoise = tk.BooleanVar(value=False)
        self.asr_model = tk.StringVar()
        self.base_ckpt = tk.StringVar()
        self.use_espeak = tk.BooleanVar(value=False)
        self.piper_config = tk.StringVar()
        self.sample_rate = tk.IntVar(value=22050)
        self.device = tk.StringVar(value="cpu")
        self.voicepack_zip = tk.StringVar()
        self.voicepack_name = tk.StringVar(value="未命名")
        self.voicepack_remark = tk.StringVar()
        self.voicepack_avatar = tk.StringVar()
        self.status = tk.StringVar(value="待命")
        self.last_test_audio: Path | None = None
        self.progress_vars = {
            "preprocess": tk.DoubleVar(value=0),
            "vad": tk.DoubleVar(value=0),
            "asr": tk.DoubleVar(value=0),
            "train": tk.DoubleVar(value=0),
            "export": tk.DoubleVar(value=0),
        }


        self._build_layout()
        self.logger = setup_logger(Path.cwd() / "logs")
        self._prefill_model_paths()

    def _build_layout(self) -> None:
        top = ttk.Frame(self, padding=10)
        top.pack(fill="both", expand=True)

        # Project
        proj = ttk.LabelFrame(top, text="项目与输出")
        proj.pack(fill="x", pady=5)
        ttk.Label(proj, text="输出目录:").grid(row=0, column=0, sticky="w", padx=5, pady=5)
        ttk.Entry(proj, textvariable=self.output_dir, width=60).grid(row=0, column=1, sticky="we", padx=5)
        ttk.Button(proj, text="选择", command=self._choose_output).grid(row=0, column=2, padx=5)
        ttk.Label(proj, text="缓存目录: <输出目录>/work").grid(row=1, column=0, columnspan=2, sticky="w", padx=5, pady=5)
        ttk.Button(proj, text="清理缓存", command=self._clear_work_cache).grid(row=1, column=2, padx=5)
        proj.columnconfigure(1, weight=1)

        # Audio list
        audio = ttk.LabelFrame(top, text="音频导入（可拖拽多个文件）")
        audio.pack(fill="both", expand=False, pady=5)
        self.listbox = tk.Listbox(audio, height=6)
        self.listbox.pack(side="left", fill="both", expand=True, padx=5, pady=5)
        if hasattr(self.listbox, "drop_target_register") and DND_FILES:
            self.listbox.drop_target_register(DND_FILES)
            self.listbox.dnd_bind("<<Drop>>", self._on_drop)
        btns = ttk.Frame(audio)
        btns.pack(side="right", fill="y", padx=5)
        ttk.Button(btns, text="添加文件", command=self._add_files).pack(fill="x", pady=2)
        ttk.Button(btns, text="移除选中", command=self._remove_selected).pack(fill="x", pady=2)
        ttk.Button(btns, text="清空", command=self._clear_files).pack(fill="x", pady=2)

        # Options
        opts_frame = ttk.LabelFrame(top, text="参数")
        opts_frame.pack(fill="x", pady=5)
        ttk.Label(opts_frame, text="音频等级:").grid(row=0, column=0, padx=5, pady=5, sticky="w")
        ttk.Radiobutton(opts_frame, text="A 档", variable=self.quality, value="A").grid(row=0, column=1, padx=2)
        ttk.Radiobutton(opts_frame, text="B 档(降噪)", variable=self.quality, value="B").grid(row=0, column=2, padx=2)
        ttk.Checkbutton(opts_frame, text="开启降噪/筛选", variable=self.denoise).grid(row=0, column=3, padx=5)
        ttk.Label(opts_frame, text="采样率").grid(row=0, column=4, padx=5)
        ttk.Entry(opts_frame, textvariable=self.sample_rate, width=8).grid(row=0, column=5, padx=2)

        ttk.Label(opts_frame, text="ASR 模型 zip").grid(row=1, column=0, padx=5, pady=5)
        ttk.Entry(opts_frame, textvariable=self.asr_model, width=50).grid(row=1, column=1, columnspan=3, sticky="we", padx=2)
        ttk.Button(opts_frame, text="选择", command=self._choose_asr).grid(row=1, column=4, padx=5)

        ttk.Label(opts_frame, text="Piper 基线 ckpt (可选)").grid(row=2, column=0, padx=5, pady=5)
        ttk.Entry(opts_frame, textvariable=self.base_ckpt, width=50).grid(
            row=2, column=1, columnspan=3, sticky="we", padx=2
        )
        ttk.Button(opts_frame, text="选择", command=self._choose_ckpt).grid(row=2, column=4, padx=5)

        ttk.Checkbutton(
            opts_frame,
            text="使用 espeak-ng (兼容基线)",
            variable=self.use_espeak,
        ).grid(row=3, column=0, columnspan=2, padx=5, pady=5, sticky="w")

        ttk.Label(opts_frame, text="Piper config.json (espeak)").grid(row=4, column=0, padx=5, pady=5)
        ttk.Entry(opts_frame, textvariable=self.piper_config, width=50).grid(
            row=4, column=1, columnspan=3, sticky="we", padx=2
        )
        ttk.Button(opts_frame, text="选择", command=self._choose_piper_config).grid(row=4, column=4, padx=5)

        ttk.Label(opts_frame, text="训练设备").grid(row=5, column=0, padx=5, pady=5, sticky="w")
        ttk.Radiobutton(opts_frame, text="CPU", variable=self.device, value="cpu").grid(row=5, column=1, padx=2, sticky="w")
        ttk.Radiobutton(opts_frame, text="GPU/CUDA", variable=self.device, value="cuda").grid(row=5, column=2, padx=2, sticky="w")

        opts_frame.columnconfigure(2, weight=1)

        # Voicepack meta
        meta_frame = ttk.LabelFrame(top, text="语音包信息")
        meta_frame.pack(fill="x", pady=5)
        ttk.Label(meta_frame, text="名称").grid(row=0, column=0, padx=5, pady=5, sticky="w")
        ttk.Entry(meta_frame, textvariable=self.voicepack_name, width=30).grid(row=0, column=1, padx=5, sticky="we")
        ttk.Label(meta_frame, text="备注").grid(row=0, column=2, padx=5, pady=5, sticky="w")
        ttk.Entry(meta_frame, textvariable=self.voicepack_remark, width=40).grid(row=0, column=3, padx=5, sticky="we")
        ttk.Label(meta_frame, text="头像").grid(row=1, column=0, padx=5, pady=5, sticky="w")
        ttk.Entry(meta_frame, textvariable=self.voicepack_avatar, width=50).grid(row=1, column=1, columnspan=2, padx=5, sticky="we")
        ttk.Button(meta_frame, text="选择", command=self._choose_voicepack_avatar).grid(row=1, column=3, padx=5)
        ttk.Label(meta_frame, text="建议 400x400 PNG").grid(row=1, column=4, padx=5, sticky="w")
        meta_frame.columnconfigure(1, weight=1)
        meta_frame.columnconfigure(3, weight=1)

        # Voicepack test
        test_frame = ttk.LabelFrame(top, text="语音包测试")
        test_frame.pack(fill="x", pady=5)
        ttk.Label(test_frame, text="语音包 zip").grid(row=0, column=0, padx=5, pady=5, sticky="w")
        ttk.Entry(test_frame, textvariable=self.voicepack_zip, width=50).grid(
            row=0, column=1, columnspan=3, sticky="we", padx=2
        )
        ttk.Button(test_frame, text="选择", command=self._choose_voicepack).grid(row=0, column=4, padx=5)
        ttk.Label(test_frame, text="测试文本").grid(row=1, column=0, padx=5, pady=5, sticky="nw")
        self.test_text = tk.Text(test_frame, height=3, wrap="word")
        self.test_text.grid(row=1, column=1, columnspan=3, sticky="we", padx=2, pady=5)
        self.test_text.insert("1.0", "你好，这是语音包测试。")
        ttk.Button(test_frame, text="生成试听", command=self._start_test_voicepack).grid(
            row=0, column=5, rowspan=2, padx=5, pady=5, sticky="ns"
        )
        ttk.Button(test_frame, text="打开试听", command=self._open_test_audio).grid(
            row=0, column=6, rowspan=2, padx=5, pady=5, sticky="ns"
        )
        test_frame.columnconfigure(2, weight=1)

        # Progress
        prog = ttk.LabelFrame(top, text="进度")
        prog.pack(fill="x", pady=5)
        self._add_progress(prog, "预处理", "preprocess", 0)
        self._add_progress(prog, "VAD", "vad", 1)
        self._add_progress(prog, "ASR", "asr", 2)
        self._add_progress(prog, "训练", "train", 3)
        self._add_progress(prog, "导出", "export", 4)
        ttk.Button(prog, text="一键训练并导出", command=self._start_pipeline).grid(row=0, column=5, rowspan=5, padx=10, pady=10)

        # Log
        log_box = ttk.LabelFrame(top, text="日志")
        log_box.pack(fill="both", expand=True, pady=5)
        self.log_text = tk.Text(log_box, height=12, wrap="word")
        self.log_text.pack(fill="both", expand=True, padx=5, pady=5)
        self.status_label = ttk.Label(self, textvariable=self.status, anchor="w")
        self.status_label.pack(fill="x")

    def _add_progress(self, parent: ttk.LabelFrame, label: str, key: str, row: int) -> None:
        ttk.Label(parent, text=label).grid(row=row, column=0, padx=5, pady=2, sticky="w")
        bar = ttk.Progressbar(parent, variable=self.progress_vars[key], maximum=1.0, length=200)
        bar.grid(row=row, column=1, padx=5, pady=2, sticky="we")
        parent.columnconfigure(1, weight=1)

    def _append_log(self, text: str) -> None:
        self.log_text.insert("end", text + "\n")
        self.log_text.see("end")
        print(text)

    def _choose_output(self) -> None:
        path = filedialog.askdirectory()
        if path:
            self.output_dir.set(path)

    def _clear_work_cache(self) -> None:
        work_dir = Path(self.output_dir.get()) / "work"
        if not work_dir.exists():
            messagebox.showinfo("提示", f"未找到缓存目录: {work_dir}")
            return
        if not work_dir.is_dir():
            messagebox.showwarning("提示", f"缓存路径不是目录: {work_dir}")
            return
        if not messagebox.askyesno("确认清理", f"将清空缓存目录：\n{work_dir}\n\n此操作不可恢复，继续？"):
            return
        try:
            shutil.rmtree(work_dir)
            work_dir.mkdir(parents=True, exist_ok=True)
            self.status.set("缓存已清理")
            self._append_log(f"已清理缓存: {work_dir}")
        except Exception as exc:  # noqa: BLE001
            self.status.set("缓存清理失败")
            messagebox.showerror("出错了", str(exc))
            self._append_log(f"[ERROR] 清理缓存失败: {exc}")

    def _choose_asr(self) -> None:
        path = filedialog.askopenfilename(filetypes=[("Zip", "*.zip")])
        if path:
            self.asr_model.set(path)

    def _choose_ckpt(self) -> None:
        path = filedialog.askopenfilename(filetypes=[("Checkpoint", "*.ckpt *.pth *.pt"), ("All", "*.*")])
        if path:
            self.base_ckpt.set(path)

    def _choose_piper_config(self) -> None:
        path = filedialog.askopenfilename(filetypes=[("Config", "*.json"), ("All", "*.*")])
        if path:
            self.piper_config.set(path)

    def _choose_voicepack(self) -> None:
        path = filedialog.askopenfilename(filetypes=[("Voicepack", "*.zip"), ("All", "*.*")])
        if path:
            self.voicepack_zip.set(path)

    def _choose_voicepack_avatar(self) -> None:
        path = filedialog.askopenfilename(filetypes=[("Image", "*.png *.jpg *.jpeg *.webp"), ("All", "*.*")])
        if path:
            self.voicepack_avatar.set(path)

    def _add_files(self) -> None:
        paths = filedialog.askopenfilenames(filetypes=[("Audio", "*.wav *.mp3 *.m4a *.flac"), ("All", "*.*")])
        self._ingest_files(paths)

    def _on_drop(self, event) -> None:  # type: ignore[override]
        raw = event.data
        paths = self.splitlist(raw)
        self._ingest_files(paths)

    def _ingest_files(self, paths) -> None:
        for p in paths:
            path = Path(p)
            if path.exists():
                self.audio_files.append(path)
                self.listbox.insert("end", str(path))

    def _remove_selected(self) -> None:
        selected = list(self.listbox.curselection())
        for idx in reversed(selected):
            self.listbox.delete(idx)
            del self.audio_files[idx]

    def _clear_files(self) -> None:
        self.listbox.delete(0, "end")
        self.audio_files.clear()

    def _start_pipeline(self) -> None:
        if not self.audio_files:
            messagebox.showwarning("缺少音频", "请先添加音频文件")
            return
        if not self.asr_model.get():
            messagebox.showwarning("缺少 ASR 模型", "请选择 sosv.zip / sosv-int8.zip")
            return
        for var in self.progress_vars.values():
            var.set(0.0)
        self.status.set("运行中…")
        thread = threading.Thread(target=self._run_pipeline_thread, daemon=True)
        thread.start()

    def _start_test_voicepack(self) -> None:
        voicepack = self.voicepack_zip.get().strip()
        if not voicepack:
            messagebox.showwarning("缺少语音包", "请选择 voicepack.zip")
            return
        text = self.test_text.get("1.0", "end").strip()
        if not text:
            messagebox.showwarning("缺少文本", "请输入测试文本")
            return
        self.status.set("语音包试听生成中…")
        thread = threading.Thread(
            target=self._run_test_voicepack_thread,
            args=(Path(voicepack), text),
            daemon=True,
        )
        thread.start()

    def _run_pipeline_thread(self) -> None:
        try:
            opts = TrainingOptions(
                quality=self.quality.get(),
                denoise=self.denoise.get(),
                asr_model_zip=Path(self.asr_model.get()),
                piper_base_checkpoint=Path(self.base_ckpt.get()) if self.base_ckpt.get() else None,
                use_espeak=self.use_espeak.get(),
                piper_config=Path(self.piper_config.get()) if self.piper_config.get() else None,
                sample_rate=self.sample_rate.get(),
                device=self.device.get(),
                voicepack_name=self.voicepack_name.get().strip() or "未命名",
                voicepack_remark=self.voicepack_remark.get().strip(),
                voicepack_avatar=Path(self.voicepack_avatar.get()) if self.voicepack_avatar.get() else None,
            )
            paths = ProjectPaths(
                project_root=Path(self.output_dir.get()),
                input_audio=self.audio_files,
            )
            self.logger.info("启动流水线，输出目录：%s", paths.project_root)
            result = run_pipeline(paths, opts, self._on_progress)
            self.status.set(f"完成，voicepack: {result.voicepack_path}")
            self._append_log(f"导出完成: {result.voicepack_path}")
        except Exception as exc:  # noqa: BLE001
            self.status.set("失败")
            messagebox.showerror("出错了", str(exc))
            self._append_log(f"[ERROR] {exc}")

    def _run_test_voicepack_thread(self, voicepack_path: Path, text: str) -> None:
        try:
            out_dir = Path(self.output_dir.get()) / "preview"
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / "voicepack_test.wav"
            self._synthesize_voicepack(voicepack_path, text, out_path)
            self.last_test_audio = out_path
            self.status.set(f"试听完成: {out_path}")
            self._append_log(f"试听生成: {out_path}")
        except Exception as exc:  # noqa: BLE001
            self.status.set("试听失败")
            messagebox.showerror("出错了", str(exc))
            self._append_log(f"[ERROR] {exc}")

    def _open_test_audio(self) -> None:
        if not self.last_test_audio or not self.last_test_audio.exists():
            messagebox.showwarning("无试听文件", "请先生成试听")
            return
        try:
            os.startfile(self.last_test_audio)  # type: ignore[attr-defined]
        except Exception:
            messagebox.showinfo("提示", f"试听文件: {self.last_test_audio}")

    def _synthesize_voicepack(self, voicepack_path: Path, text: str, out_path: Path) -> None:
        try:
            import numpy as np
            import onnxruntime as ort
        except Exception as exc:  # noqa: BLE001
            raise RuntimeError("缺少 onnxruntime/numpy，无法测试语音包") from exc

        with tempfile.TemporaryDirectory() as tmp_dir:
            base_dir, manifest = self._load_voicepack_base(voicepack_path, Path(tmp_dir))
            model_path, config_path, dict_path = self._resolve_voicepack_files(base_dir, manifest)
            cfg = json.loads(config_path.read_text(encoding="utf-8"))
            phoneme_type = str(cfg.get("phoneme_type", "text")).lower()
            id_map = cfg.get("phoneme_id_map") or {}
            if not id_map:
                raise RuntimeError("语音包缺少 phoneme_id_map，无法合成")
            if phoneme_type == "espeak":
                ids = self._text_to_espeak_ids(text, cfg)
            elif phoneme_type == "text":
                phoneme_map = self._load_phonemizer_dict(dict_path)
                ids = self._text_to_phoneme_ids(text, phoneme_map, id_map)
            else:
                raise RuntimeError(f"不支持的 phoneme_type: {phoneme_type}")
            if not ids:
                raise RuntimeError("测试文本无法转换为 phoneme ids")

            infer_cfg = cfg.get("inference") or {}
            noise_scale = float(infer_cfg.get("noise_scale", 0.667))
            length_scale = float(infer_cfg.get("length_scale", 1.0))
            noise_w = float(infer_cfg.get("noise_w", infer_cfg.get("noise_scale_w", 0.8)))

            try:
                available = list(ort.get_available_providers() or [])
            except Exception:
                available = []
            providers = ["CPUExecutionProvider"] if "CPUExecutionProvider" in available else available
            sess = ort.InferenceSession(str(model_path), providers=providers)
            input_names = [i.name for i in sess.get_inputs()]
            input_name = self._pick_input(input_names, ["input", "text", "phoneme"], fallback_first=True)
            length_name = self._pick_input(input_names, ["length", "len"])
            if not input_name or not length_name:
                raise RuntimeError("语音包模型输入不完整，无法合成")
            scale_name = self._pick_input(input_names, ["scale"])
            sid_name = self._pick_input(input_names, ["sid", "speaker"])

            inputs = {
                input_name: np.array([ids], dtype=np.int64),
                length_name: np.array([len(ids)], dtype=np.int64),
            }
            if scale_name:
                inputs[scale_name] = np.array([noise_scale, length_scale, noise_w], dtype=np.float32)
            if sid_name:
                inputs[sid_name] = np.array([0], dtype=np.int64)

            audio = sess.run(None, inputs)[0]
            audio = np.squeeze(audio)
            audio = np.clip(audio, -1.0, 1.0)
            audio_i16 = (audio * 32767.0).astype(np.int16)
            sample_rate = int(
                manifest.get("sample_rate")
                or cfg.get("audio", {}).get("sample_rate")
                or cfg.get("sample_rate")
                or 22050
            )
            out_path.parent.mkdir(parents=True, exist_ok=True)
            with wave.open(str(out_path), "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(sample_rate)
                wf.writeframes(audio_i16.tobytes())

    def _load_voicepack_base(self, voicepack_path: Path, temp_dir: Path) -> tuple[Path, dict]:
        if voicepack_path.is_dir():
            manifest_path = voicepack_path / "manifest.json"
            if not manifest_path.exists():
                zip_candidate = voicepack_path / "voicepack.zip"
                if zip_candidate.exists():
                    return self._load_voicepack_base(zip_candidate, temp_dir)
                raise FileNotFoundError("语音包目录缺少 manifest.json")
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            return voicepack_path, manifest
        if not zipfile.is_zipfile(voicepack_path):
            raise RuntimeError("语音包不是有效的 zip 文件")
        with zipfile.ZipFile(voicepack_path, "r") as zf:
            zf.extractall(temp_dir)
        manifest_path = temp_dir / "manifest.json"
        if not manifest_path.exists():
            raise FileNotFoundError("语音包缺少 manifest.json")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        return temp_dir, manifest

    def _resolve_voicepack_files(self, base_dir: Path, manifest: dict) -> tuple[Path, Path, Path]:
        files = manifest.get("files") or {}
        model_rel = files.get("model")
        config_rel = files.get("config")
        dict_rel = files.get("phonemizer")
        if not model_rel or not config_rel or not dict_rel:
            raise RuntimeError("语音包 manifest 缺少 files/model/config/phonemizer")
        model_path = base_dir / model_rel
        config_path = base_dir / config_rel
        dict_path = base_dir / dict_rel
        for path in (model_path, config_path, dict_path):
            if not path.exists():
                raise FileNotFoundError(f"缺少语音包文件: {path}")
        return model_path, config_path, dict_path

    def _load_phonemizer_dict(self, dict_path: Path) -> dict[str, list[str]]:
        mapping: dict[str, list[str]] = {}
        for line in dict_path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split()
            if len(parts) >= 2:
                mapping[parts[0]] = parts[1:]
        return mapping

    def _find_espeak_ng(self) -> tuple[Path, Path]:
        if getattr(sys, "frozen", False):
            base_dir = Path(sys.executable).resolve().parent
        else:
            base_dir = Path(__file__).resolve().parent
        candidates: list[Path] = []
        env_path = os.environ.get("ESPEAK_NG_PATH")
        if env_path:
            candidates.append(Path(env_path))
        candidates.append(base_dir / "tools" / "espeak-ng" / "eSpeak NG" / "espeak-ng.exe")
        candidates.append(base_dir / "tools" / "espeak-ng" / "espeak-ng.exe")
        for exe in candidates:
            if exe.exists():
                data_dir = exe.parent / "espeak-ng-data"
                if data_dir.exists():
                    return exe, data_dir
        raise RuntimeError("未找到 espeak-ng，请先集成 espeak-ng")

    def _strip_language_flags(self, text: str) -> str:
        out: list[str] = []
        in_flag = False
        for ch in text:
            if in_flag:
                if ch == ")":
                    in_flag = False
                continue
            if ch == "(":
                in_flag = True
                continue
            if ch in "\r\n":
                continue
            out.append(ch)
        return "".join(out)

    def _phonemize_espeak(self, text: str, voice: str) -> list[str]:
        exe, data_dir = self._find_espeak_ng()
        env = os.environ.copy()
        env["ESPEAK_DATA_PATH"] = str(data_dir)
        cmd = [
            str(exe),
            "-q",
            "--ipa",
            "-b",
            "1",
            "-v",
            voice,
            "--path",
            str(data_dir.parent),
            "--stdin",
        ]
        proc = subprocess.run(
            cmd,
            input=text,
            text=True,
            encoding="utf-8",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=env,
        )
        if proc.returncode != 0:
            err = (proc.stderr or proc.stdout).strip()
            raise RuntimeError(f"espeak-ng 失败: {err}")
        out = proc.stdout.strip()
        if not out:
            return []
        out = self._strip_language_flags(out)
        out = unicodedata.normalize("NFD", out)
        return [ch for ch in out if ch not in "\r\n"]

    def _pick_trailing_punct(self, text: str) -> str | None:
        mapping = {
            "，": ",",
            "。": ".",
            "！": "!",
            "？": "?",
            "：": ":",
            "；": ";",
            "、": ",",
        }
        for ch in reversed(text):
            if ch.isspace():
                continue
            if ch in mapping:
                return mapping[ch]
            if ch in ",.!?;:":
                return ch
            break
        return None

    def _text_to_espeak_ids(self, text: str, cfg: dict) -> list[int]:
        raw_id_map = cfg.get("phoneme_id_map") or {}
        if not raw_id_map:
            return []
        id_map: dict[str, list[int]] = {}
        for key, value in raw_id_map.items():
            if isinstance(value, list):
                id_map[key] = [int(v) for v in value]
            else:
                id_map[key] = [int(value)]
        voice = ""
        espeak_cfg = cfg.get("espeak") or {}
        if isinstance(espeak_cfg, dict):
            voice = str(espeak_cfg.get("voice") or "")
        if not voice:
            lang_cfg = cfg.get("language") or {}
            if isinstance(lang_cfg, dict):
                voice = str(lang_cfg.get("code") or "")
        if not voice:
            voice = "en"
        phoneme_map_raw = cfg.get("phoneme_map") or {}
        phoneme_map: dict[str, list[str]] = {}
        for key, value in phoneme_map_raw.items():
            if isinstance(value, list):
                phoneme_map[key] = [str(v) for v in value]
            else:
                phoneme_map[key] = [str(value)]
        phonemes = self._phonemize_espeak(text, voice)
        if phoneme_map:
            mapped: list[str] = []
            for ph in phonemes:
                repl = phoneme_map.get(ph)
                if repl:
                    mapped.extend(repl)
                else:
                    mapped.append(ph)
            phonemes = mapped
        tail = self._pick_trailing_punct(text)
        if tail and tail in id_map:
            phonemes.append(tail)
        pad = id_map.get("_", [0])
        ids: list[int] = []
        bos = id_map.get("^")
        eos = id_map.get("$")
        if bos:
            ids.extend(bos)
            ids.extend(pad)
        for ph in phonemes:
            mapped_ids = id_map.get(ph)
            if not mapped_ids:
                continue
            ids.extend(mapped_ids)
            ids.extend(pad)
        if eos:
            ids.extend(eos)
        return ids

    def _text_to_phoneme_ids(self, text: str, mapping: dict, id_map: dict) -> list[int]:
        ids: list[int] = []
        fallback = int(id_map.get("_", 0))
        for ch in text:
            if ch.isspace():
                continue
            phones = mapping.get(ch)
            if phones:
                for ph in phones:
                    ids.append(int(id_map.get(ph, fallback)))
            else:
                ids.append(int(id_map.get(ch, fallback)))
        return ids

    def _pick_input(self, names: list[str], keys: list[str], fallback_first: bool = False) -> str | None:
        for key in keys:
            for name in names:
                if key in name.lower():
                    return name
        if fallback_first and names:
            return names[0]
        return None

    def _on_progress(self, stage: str, value: float, message: str) -> None:
        self.after(0, self._update_progress, stage, value, message)

    def _update_progress(self, stage: str, value: float, message: str) -> None:
        if stage in self.progress_vars:
            self.progress_vars[stage].set(value)
        self.status.set(message)
        self._append_log(f"[{stage}] {message}")

    def _prefill_model_paths(self) -> None:
        """Auto-fill ASR / Piper基线路径：优先使用与可执行文件同级的 Model 目录。"""
        if getattr(sys, "frozen", False):
            base_dir = Path(sys.executable).resolve().parent
        else:
            base_dir = Path(__file__).resolve().parent
        model_dir = base_dir / "Model"
        if not model_dir.exists():
            alt_dir = Path(__file__).resolve().parent / "Model"
            if not alt_dir.exists():
                return
            model_dir = alt_dir
        if not self.asr_model.get():
            asr = next((p for p in model_dir.glob("*.zip") if "sosv" in p.name.lower()), None)
            if asr:
                self.asr_model.set(str(asr))
                self._append_log(f"自动加载 ASR 模型: {asr.name}")
        if not self.base_ckpt.get():
            ckpt_candidates: list[Path] = []
            ckpt_candidates.extend(
                p for p in model_dir.glob("*") if p.suffix.lower() in {".ckpt", ".pth", ".pt"}
            )
            ckpt_candidates.extend((model_dir / "piper_checkpoints").rglob("*.ckpt"))
            ckpt_dir = base_dir / "CKPT"
            if ckpt_dir.exists():
                ckpt_candidates.extend(ckpt_dir.rglob("*.ckpt"))
            ckpt = max(ckpt_candidates, key=lambda p: p.stat().st_mtime) if ckpt_candidates else None
            if ckpt:
                self.base_ckpt.set(str(ckpt))
                self._append_log(f"自动加载 Piper 基线: {ckpt.name}")
        if not self.piper_config.get():
            cfg_root = model_dir / "piper_checkpoints"
            if cfg_root.exists():
                cfg = next(cfg_root.rglob("config.json"), None)
                if cfg:
                    self.piper_config.set(str(cfg))
                    self._append_log(f"自动加载 Piper config: {cfg.name}")
                    if self.base_ckpt.get():
                        self.use_espeak.set(True)
        if not self.voicepack_zip.get():
            default_vp = Path(self.output_dir.get()) / "export" / "voicepack.zip"
            if default_vp.exists():
                self.voicepack_zip.set(str(default_vp))
                self._append_log(f"自动加载语音包: {default_vp.name}")


if __name__ == "__main__":
    app = TrainerGUI()
    app.mainloop()
