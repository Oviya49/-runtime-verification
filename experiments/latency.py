import glob
import os
import numpy as np
import pandas as pd

BASE = r"C:\Projects\tenantguard-project\experiments\runs"
OUT = r"C:\Projects\tenantguard-project\experiments\analysis"
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(20261023)
B = 2000



rows = []
for det in ["VERIFIER", "HEADER_ONLY"]:
   
    files = glob.glob(os.path.join(BASE, f"timing-{det}-s*", "observed_NONE.csv"))
    t = pd.concat([pd.read_csv(f) for f in files])["detectionTimeMs"].astype(float).values
    row = {"detector": det, "n": len(t)}
    for q in [50, 95, 99]:
        boots = [np.percentile(t[rng.integers(0, len(t), len(t))], q) for _ in range(B)]
        row[f"P{q}"] = np.percentile(t, q)
        row[f"P{q}_CI_low"], row[f"P{q}_CI_high"] = np.percentile(boots, [2.5, 97.5])
    rows.append(row)

res = pd.DataFrame(rows)
res.to_csv(os.path.join(OUT, "latency_NONE.csv"), index=False)
print(res.to_string(index=False))