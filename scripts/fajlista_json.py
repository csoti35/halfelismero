"""A fajlista + bevett magyar nevek -> data/fajlista.json (az app ezt adja a felismerésnek választéklistaként)."""
import json, os
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
hu = {}
for line in open(os.path.join(R, "scripts/magyar_nevek.txt"), encoding="utf-8"):
    line = line.strip()
    if line and not line.startswith("#") and ";" in line:
        lat, name = line.split(";", 1); hu[lat.strip()] = name.strip()
out = {"hal": [], "noveny": []}
for line in open(os.path.join(R, "scripts/fajlista.txt"), encoding="utf-8"):
    line = line.strip()
    if not line or line.startswith("#") or ";" not in line: continue
    t, lat = [x.strip() for x in line.split(";", 1)]
    item = lat + (f" ({hu[lat]})" if lat in hu else "")
    out["noveny" if t == "noveny" else "hal"].append(item)
json.dump(out, open(os.path.join(R, "data/fajlista.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print({k: len(v) for k, v in out.items()})
