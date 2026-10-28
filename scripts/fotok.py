"""Fajfotók gyűjtése a fajlistához az iNaturalistról.
Csak szabad licencű (Creative Commons) fotókat veszünk fel, a szerző nevével és a licenccel.
A képeket nem másoljuk le: csak a linkjüket tároljuk (data/fotok.json), a kép az iNaturalistról töltődik be.
"""
import json, os, re, time, urllib.parse, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIST = os.path.join(ROOT, "scripts", "fajlista.txt")
OUT = os.path.join(ROOT, "data", "fotok.json")
UA = {"User-Agent": "AquaFaj-zsebkonyv/1.0 (github.com/csoti35/halfelismero)"}
OK_LIC = {"cc0", "cc-by", "cc-by-sa", "cc-by-nc", "cc-by-nc-sa", "cc-by-nd", "cc-by-nc-nd"}

def get(url):
    for i in range(4):
        try:
            return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=40))
        except Exception as e:
            if i == 3: raise
            time.sleep(5 * (i + 1))

def key(s):
    s = re.sub(r"\s*['\"(].*$", "", s).strip().lower()
    return " ".join(s.split()[:2])

def main():
    items = []
    for line in open(LIST, encoding="utf-8"):
        line = line.strip()
        if line and not line.startswith("#") and ";" in line:
            items.append(line.split(";", 1)[1].strip())
    out = json.load(open(OUT, encoding="utf-8")) if os.path.exists(OUT) else {}
    done = 0
    for latin in items:
        if latin in out and out[latin]:
            continue
        q = key(latin)
        if "hibrid" in latin.lower() or len(q.split()) < 2:
            out[latin] = []
            continue
        try:
            r = get("https://api.inaturalist.org/v1/taxa?rank=species,subspecies,variety,hybrid&per_page=3&q=" + urllib.parse.quote(q))
            t = next((x for x in r.get("results", []) if key(x.get("name", "")) == q), None)
            if not t:
                out[latin] = []
                print("nincs találat:", latin); continue
            full = get(f"https://api.inaturalist.org/v1/taxa/{t['id']}")["results"][0]
            photos = []
            for tp in full.get("taxon_photos", []):
                p = tp.get("photo") or {}
                if (p.get("license_code") or "").lower() in OK_LIC and p.get("medium_url"):
                    photos.append({"u": p["medium_url"], "a": re.sub(r"^\(c\)\s*", "", p.get("attribution") or "").split(",")[0].strip(), "l": p["license_code"].upper()})
                if len(photos) >= 6: break
            out[latin] = photos
            out.setdefault("_taxon", {})[latin] = t["id"]
            done += 1
            print(f"{latin}: {len(photos)} fotó")
        except Exception as e:
            print("hiba:", latin, e)
        time.sleep(1.1)  # az iNaturalist kérése: legfeljebb ~1 kérés/mp
        if done % 50 == 0:
            json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    n = sum(1 for k, v in out.items() if k != "_taxon" and v)
    print(f"::notice::Fotók: {n} / {len(items)} fajhoz van szabad licencű kép")

if __name__ == "__main__":
    main()
