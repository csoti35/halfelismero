"""A felhasználó listájában szereplő Wikimedia Commons képek felvétele a fotók közé.
Csak szabad licencű (CC / közkincs) képek kerülnek be, a szerző nevével; a licencet a Commons adataiból ellenőrizzük."""
import json, os, re, time, urllib.error, urllib.parse, urllib.request
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "scripts", "wikimedia_kepek.json"); OUT = os.path.join(ROOT, "data", "fotok.json")
UA = {"User-Agent": "AquaFaj-zsebkonyv/1.0 (github.com/csoti35/halfelismero)"}
def strip(h): return re.sub(r"<[^>]+>", "", h or "").strip()
items = json.load(open(SRC, encoding="utf-8")); fot = json.load(open(OUT, encoding="utf-8"))
ok = bad = 0
for it in items:
    fn = re.sub(r"^\d+px-", "", urllib.parse.unquote(it["kep"].rsplit("/", 1)[-1]))
    if any(p.get("src") == "Wikimedia Commons" for p in fot.get(it["latin"], [])): continue
    url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=500&titles=" + urllib.parse.quote("File:" + fn)
    try:
        for t in range(5):
            try:
                page = next(iter(json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30))["query"]["pages"].values())); break
            except urllib.error.HTTPError as e:
                if e.code != 429 or t == 4: raise
                time.sleep(20 * (t + 1))
        ii = page["imageinfo"][0]; md = ii.get("extmetadata", {})
        lic = (md.get("LicenseShortName", {}).get("value") or "").strip()
        if not re.search(r"^(CC|Public domain|PD)", lic, re.I) or re.search(r"fair use|non-free", lic, re.I):
            bad += 1; print("::warning::licenc:", fn, lic); continue
        photo = {"u": ii.get("thumburl") or ii["url"], "a": strip(md.get("Artist", {}).get("value"))[:60] or "Wikimedia Commons", "l": lic, "src": "Wikimedia Commons"}
        cur = [p for p in fot.get(it["latin"], []) if p.get("u") != photo["u"]]
        fot[it["latin"]] = [photo] + cur[:5]
        ok += 1
    except Exception as e:
        bad += 1; print("::warning::hiba:", fn, str(e)[:80]); time.sleep(3)
    time.sleep(1.2)
json.dump(fot, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print(f"::notice::Wikimedia: {ok} kép felvéve, {bad} kihagyva")
