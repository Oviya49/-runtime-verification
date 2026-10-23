import math
import os
import numpy as np
import pandas as pd

BASE = r"C:\Projects\tenantguard-project\experiments\runs"
OUT = r"C:\Projects\tenantguard-project\experiments\analysis"
SEEDS = [1, 2, 3]
B = 2000
rng = np.random.default_rng(20261023)
os.makedirs(OUT, exist_ok=True)

def load_scored(det, seed):
    df = pd.read_csv(os.path.join(BASE, f"main-{det}-s{seed}", "scored.csv"))
    df["expectedPositive"] = df["expectedPositive"].astype(str).str.lower() == "true"
    df["detected"] = df["detected"].astype(str).str.lower() == "true"
    df["idx"] = df.groupby("condition").cumcount()
    df["correct"] = df["detected"] == df["expectedPositive"]
    df["seed"] = seed
    return df

def load_ledger(det, seed):
    led = pd.read_csv(os.path.join(BASE, f"main-{det}-s{seed}", "ledger.csv"))
    led["idx"] = led.groupby("expectedFault").cumcount()
    return led

def boot_ci(x):
    x = np.asarray(x, dtype=float)
    if len(x) == 0:
        return (float("nan"), float("nan"), float("nan"))
    idx = rng.integers(0, len(x), size=(B, len(x)))
    means = x[idx].mean(axis=1)
    return (x.mean(), np.percentile(means, 2.5), np.percentile(means, 97.5))

def mcnemar_p(b, c):
    if b + c == 0:
        return float("nan")
    stat = (abs(b - c) - 1) ** 2 / (b + c)
    return math.erfc(math.sqrt(stat / 2))  # chi-square, 1 df

# 1. Check that both detectors saw the same workload in each seed
for s in SEEDS:
    lv = load_ledger("VERIFIER", s)
    lh = load_ledger("HEADER_ONLY", s)
    same = (lv[["expectedFault", "idx", "tenantId"]].values == lh[["expectedFault", "idx", "tenantId"]].values).all()
    print(f"seed {s}: identical workload across detectors = {same}")

# 2. Recall and false-positive rate with 95% bootstrap intervals
rows = []
for det in ["VERIFIER", "HEADER_ONLY"]:
    pooled = pd.concat([load_scored(det, s) for s in SEEDS])
    pos = pooled[pooled.expectedPositive]
    neg = pooled[~pooled.expectedPositive]
    tp = int(pos.detected.sum())
    fp = int(neg.detected.sum())
    rec = boot_ci(pos.detected)
    fpr = boot_ci(neg.detected)
    prec = tp / (tp + fp) if tp + fp else float("nan")
    rows.append({
        "detector": det, "positives": len(pos), "negatives": len(neg),
        "TP": tp, "FP": fp, "precision": round(prec, 4),
        "recall": round(rec[0], 4), "recall_CI_low": round(rec[1], 4), "recall_CI_high": round(rec[2], 4),
        "FPR": round(fpr[0], 4), "FPR_CI_low": round(fpr[1], 4), "FPR_CI_high": round(fpr[2], 4),
    })
    for cond, g in pos.groupby("condition"):
        r = boot_ci(g.detected)
        rows.append({"detector": det, "condition": cond, "positives": len(g),
                     "recall": round(r[0], 4), "recall_CI_low": round(r[1], 4), "recall_CI_high": round(r[2], 4)})

result = pd.DataFrame(rows)
result.to_csv(os.path.join(OUT, "metrics.csv"), index=False)
print(result.to_string(index=False))

# 3. Paired McNemar test on per-message correctness
v = pd.concat([load_scored("VERIFIER", s) for s in SEEDS])
h = pd.concat([load_scored("HEADER_ONLY", s) for s in SEEDS])
m = v.merge(h, on=["seed", "condition", "idx"], suffixes=("_v", "_h"))
assert (m.expectedPositive_v == m.expectedPositive_h).all(), "pairing mismatch"
b = int((m.correct_v & ~m.correct_h).sum())
c = int((~m.correct_v & m.correct_h).sum())
print(f"McNemar: verifier-only correct b={b}, baseline-only correct c={c}, p={mcnemar_p(b, c):.3g}")
print("Note: messages within a seed share one run, so the test treats them as independent; state this in the paper.")