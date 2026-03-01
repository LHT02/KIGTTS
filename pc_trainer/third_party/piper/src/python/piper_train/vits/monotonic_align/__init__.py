import numpy as np
import torch

try:
    from .monotonic_align.core import maximum_path_c  # type: ignore
except Exception:  # pragma: no cover - fallback for portable builds
    maximum_path_c = None


def _maximum_path_numpy(neg_cent: np.ndarray, mask: np.ndarray) -> np.ndarray:
    """Fallback monotonic alignment using DP (CPU/Numpy)."""
    batch, t_t, t_s = neg_cent.shape
    path = np.zeros((batch, t_t, t_s), dtype=np.int32)
    for b in range(batch):
        mask_b = mask[b].astype(np.bool_)
        if mask_b.size == 0:
            continue
        # mask is full rectangle; infer valid lengths
        t_t_max = int(mask_b.sum(axis=0)[0])
        t_s_max = int(mask_b.sum(axis=1)[0])
        if t_t_max <= 0 or t_s_max <= 0:
            continue
        neg = neg_cent[b, :t_t_max, :t_s_max]
        dp = np.full((t_t_max, t_s_max), -1.0e9, dtype=np.float32)
        dp[0, 0] = neg[0, 0]
        for t in range(1, t_t_max):
            j_min = max(0, t - (t_t_max - t_s_max))
            j_max = min(t_s_max - 1, t)
            for j in range(j_min, j_max + 1):
                if j == 0:
                    prev = dp[t - 1, 0]
                else:
                    prev = dp[t - 1, j] if dp[t - 1, j] >= dp[t - 1, j - 1] else dp[t - 1, j - 1]
                dp[t, j] = prev + neg[t, j]

        j = t_s_max - 1
        for t in range(t_t_max - 1, -1, -1):
            path[b, t, j] = 1
            if t == 0:
                break
            if j > 0 and dp[t - 1, j - 1] >= dp[t - 1, j]:
                j -= 1
    return path


def maximum_path(neg_cent, mask):
    """Cython optimized version if available; else numpy fallback.
    neg_cent: [b, t_t, t_s]
    mask: [b, t_t, t_s]
    """
    device = neg_cent.device
    dtype = neg_cent.dtype
    neg_cent = neg_cent.data.cpu().numpy().astype(np.float32)
    mask_np = mask.data.cpu().numpy()
    if maximum_path_c is not None:
        path = np.zeros(neg_cent.shape, dtype=np.int32)
        t_t_max = mask.sum(1)[:, 0].data.cpu().numpy().astype(np.int32)
        t_s_max = mask.sum(2)[:, 0].data.cpu().numpy().astype(np.int32)
        maximum_path_c(path, neg_cent, t_t_max, t_s_max)
    else:
        path = _maximum_path_numpy(neg_cent, mask_np)
    return torch.from_numpy(path).to(device=device, dtype=dtype)
