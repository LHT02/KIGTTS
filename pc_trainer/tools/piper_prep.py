import argparse
import json
import logging
import os
import subprocess
import unicodedata
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

from piper_train.norm_audio import cache_norm_audio, make_silence_detector


def load_dict(dict_path: Path) -> Dict[str, List[str]]:
    mapping: Dict[str, List[str]] = {}
    if not dict_path.exists():
        raise FileNotFoundError(f"Dict not found: {dict_path}")
    with dict_path.open("r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) >= 2:
                mapping[parts[0]] = parts[1:]
    return mapping


def load_piper_config(config_path: Path) -> tuple[dict, Dict[str, List[int]], Dict[str, List[str]], str]:
    if not config_path.exists():
        raise FileNotFoundError(f"Piper config not found: {config_path}")
    cfg = json.loads(config_path.read_text(encoding="utf-8"))
    raw_id_map = cfg.get("phoneme_id_map") or {}
    id_map: Dict[str, List[int]] = {}
    for key, value in raw_id_map.items():
        if isinstance(value, list):
            id_map[key] = [int(v) for v in value]
        else:
            id_map[key] = [int(value)]
    raw_phone_map = cfg.get("phoneme_map") or {}
    phone_map: Dict[str, List[str]] = {}
    for key, value in raw_phone_map.items():
        if isinstance(value, list):
            phone_map[key] = [str(v) for v in value]
        else:
            phone_map[key] = [str(value)]
    voice = ""
    espeak_cfg = cfg.get("espeak") or {}
    if isinstance(espeak_cfg, dict):
        voice = str(espeak_cfg.get("voice") or "")
    if not voice:
        lang_cfg = cfg.get("language") or {}
        if isinstance(lang_cfg, dict):
            voice = str(lang_cfg.get("code") or "")
    return cfg, id_map, phone_map, voice


def find_espeak_ng(explicit: str | None) -> tuple[Path, Path]:
    candidates: List[Path] = []
    if explicit:
        candidates.append(Path(explicit))
    env_path = os.environ.get("ESPEAK_NG_PATH")
    if env_path:
        candidates.append(Path(env_path))
    tool_dir = Path(__file__).resolve().parent
    candidates.append(tool_dir / "espeak-ng" / "eSpeak NG" / "espeak-ng.exe")
    candidates.append(tool_dir / "espeak-ng" / "espeak-ng.exe")
    for exe in candidates:
        if exe and exe.exists():
            data_dir = exe.parent / "espeak-ng-data"
            if data_dir.exists():
                return exe, data_dir
    raise FileNotFoundError("espeak-ng executable not found")


def _strip_language_flags(phoneme_text: str) -> str:
    out: List[str] = []
    in_flag = False
    for ch in phoneme_text:
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


def phonemize_espeak(
    text: str,
    voice: str,
    espeak_exe: Path,
    data_dir: Path,
    phoneme_map: Dict[str, List[str]],
) -> List[str]:
    env = os.environ.copy()
    env["ESPEAK_DATA_PATH"] = str(data_dir)
    cmd = [
        str(espeak_exe),
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
        raise RuntimeError(f"espeak-ng failed: {err}")
    out = proc.stdout.strip()
    if not out:
        return []
    out = _strip_language_flags(out)
    out = unicodedata.normalize("NFD", out)
    phonemes: List[str] = []
    for ch in out:
        mapped = phoneme_map.get(ch)
        if mapped:
            phonemes.extend(mapped)
        else:
            phonemes.append(ch)
    return phonemes


def pick_trailing_punct(text: str) -> str | None:
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


def phonemes_to_ids_espeak(
    phonemes: List[str],
    id_map: Dict[str, List[int]],
) -> tuple[List[int], Dict[str, int]]:
    ids: List[int] = []
    missing: Dict[str, int] = {}
    pad = id_map.get("_", [0])
    bos = id_map.get("^")
    eos = id_map.get("$")
    if bos:
        ids.extend(bos)
        ids.extend(pad)
    for ph in phonemes:
        mapped = id_map.get(ph)
        if not mapped:
            missing[ph] = missing.get(ph, 0) + 1
            continue
        ids.extend(mapped)
        ids.extend(pad)
    if eos:
        ids.extend(eos)
    return ids, missing


def iter_metadata(path: Path) -> Iterable[Tuple[Path, str]]:
    if not path.exists():
        raise FileNotFoundError(f"metadata not found: {path}")
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if "|" not in line:
                continue
            audio_str, text = line.split("|", 1)
            yield Path(audio_str), text


def text_to_phonemes(text: str, mapping: Dict[str, List[str]]) -> List[str]:
    phones: List[str] = []
    for ch in text:
        key = ch
        mapped = mapping.get(key)
        if mapped:
            phones.extend(mapped)
        else:
            phones.append(key)
    return phones


def build_id_map(phonemes: Iterable[str]) -> Dict[str, int]:
    unique = sorted(set(p for p in phonemes if p))
    id_map: Dict[str, int] = {"_": 0}
    idx = 1
    for p in unique:
        if p == "_":
            continue
        id_map[p] = idx
        idx += 1
    return id_map


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--metadata", required=True, help="Path to metadata.csv")
    parser.add_argument("--out-dir", required=True, help="Output dir for dataset")
    parser.add_argument("--dict", help="Path to phonemizer dict")
    parser.add_argument("--sample-rate", type=int, required=True)
    parser.add_argument("--language", default="zh")
    parser.add_argument("--quality", default="medium")
    parser.add_argument("--speaker-id", type=int, default=0)
    parser.add_argument("--skip-audio", action="store_true")
    parser.add_argument("--phoneme-type", choices=["text", "espeak"], default="text")
    parser.add_argument("--piper-config", help="Path to piper config.json for espeak mode")
    parser.add_argument("--espeak-voice", help="Override espeak voice (default from config)")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    cache_dir = out_dir / "cache" / str(args.sample_rate)
    cache_dir.mkdir(parents=True, exist_ok=True)

    mapping: Dict[str, List[str]] = {}
    piper_cfg: dict | None = None
    espeak_id_map: Dict[str, List[int]] = {}
    espeak_phone_map: Dict[str, List[str]] = {}
    espeak_voice = ""
    espeak_exe: Path | None = None
    espeak_data: Path | None = None
    if args.phoneme_type == "text":
        if not args.dict:
            raise RuntimeError("phoneme_type=text 需要 --dict")
        mapping = load_dict(Path(args.dict))
    else:
        if not args.piper_config:
            raise RuntimeError("phoneme_type=espeak 需要 --piper-config")
        cfg, espeak_id_map, espeak_phone_map, cfg_voice = load_piper_config(Path(args.piper_config))
        piper_cfg = cfg
        espeak_voice = args.espeak_voice or cfg_voice or args.language
        espeak_exe, espeak_data = find_espeak_ng(None)
    entries: List[Tuple[Path, str, List[str]]] = []
    phoneme_pool: List[str] = []

    for audio_path, text in iter_metadata(Path(args.metadata)):
        if args.phoneme_type == "text":
            phones = text_to_phonemes(text, mapping)
            phoneme_pool.extend(phones)
            entries.append((audio_path, text, phones))
        else:
            phones = phonemize_espeak(
                text,
                espeak_voice,
                espeak_exe,
                espeak_data,
                espeak_phone_map,
            )
            tail = pick_trailing_punct(text)
            if tail and tail in espeak_id_map:
                phones.append(tail)
            entries.append((audio_path, text, phones))

    if not entries:
        raise RuntimeError("metadata is empty; cannot build dataset")

    id_map = build_id_map(phoneme_pool) if args.phoneme_type == "text" else {}

    use_speaker_id = args.speaker_id is not None and args.speaker_id > 0
    num_speakers = (args.speaker_id + 1) if use_speaker_id else 1
    speaker_id_map = {"speaker": args.speaker_id} if use_speaker_id else {}

    if args.phoneme_type == "text":
        config = {
            "dataset": out_dir.parent.name,
            "audio": {
                "sample_rate": args.sample_rate,
                "quality": args.quality,
            },
            "espeak": {"voice": args.language},
            "language": {"code": args.language},
            "inference": {"noise_scale": 0.667, "length_scale": 1, "noise_w": 0.8},
            "phoneme_type": "text",
            "phoneme_map": {},
            "phoneme_id_map": id_map,
            "num_symbols": max(id_map.values()) + 1 if id_map else 1,
            "num_speakers": num_speakers,
            "speaker_id_map": speaker_id_map,
            "piper_version": "custom",
        }
    else:
        max_id = max((max(v) for v in espeak_id_map.values()), default=0)
        num_symbols = int(piper_cfg.get("num_symbols") or (max_id + 1))
        config = {
            "dataset": out_dir.parent.name,
            "audio": {
                "sample_rate": args.sample_rate,
                "quality": args.quality,
            },
            "espeak": {"voice": espeak_voice},
            "language": {"code": args.language},
            "inference": {"noise_scale": 0.667, "length_scale": 1, "noise_w": 0.8},
            "phoneme_type": "espeak",
            "phoneme_map": espeak_phone_map,
            "phoneme_id_map": espeak_id_map,
            "num_symbols": num_symbols,
            "num_speakers": num_speakers,
            "speaker_id_map": speaker_id_map,
            "piper_version": "custom",
        }
    (out_dir / "config.json").write_text(
        json.dumps(config, ensure_ascii=False, indent=4),
        encoding="utf-8",
    )

    detector = make_silence_detector()
    dataset_path = out_dir / "dataset.jsonl"
    missing_total: Dict[str, int] = {}
    with dataset_path.open("w", encoding="utf-8") as out_f:
        for audio_path, text, phones in entries:
            if not audio_path.exists() and not args.skip_audio:
                logging.warning("missing audio: %s", audio_path)
                continue

            if args.phoneme_type == "text":
                phoneme_ids = [id_map.get(p, id_map.get("_", 0)) for p in phones]
            else:
                phoneme_ids, missing = phonemes_to_ids_espeak(phones, espeak_id_map)
                for key, count in missing.items():
                    missing_total[key] = missing_total.get(key, 0) + count

            if args.skip_audio:
                audio_norm_path = ""
                audio_spec_path = ""
            else:
                audio_norm_path, audio_spec_path = cache_norm_audio(
                    audio_path,
                    cache_dir,
                    detector,
                    args.sample_rate,
                )

            utt = {
                "phoneme_ids": phoneme_ids,
                "audio_norm_path": str(audio_norm_path),
                "audio_spec_path": str(audio_spec_path),
                "text": text,
                "audio_path": str(audio_path),
            }
            if use_speaker_id:
                utt["speaker_id"] = args.speaker_id
            json.dump(utt, out_f, ensure_ascii=False)
            out_f.write("\n")
    if args.phoneme_type == "espeak" and missing_total:
        sample = ", ".join(list(missing_total.keys())[:10])
        logging.warning("missing phonemes (sample): %s", sample)


if __name__ == "__main__":
    main()
