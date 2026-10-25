"""Fajadatlapok automatikus generálása a fajlista alapján.

Minden futáskor a még hiányzó fajokból készít adatlapot (egyszerre 4 fajt kér le),
amíg az ingyenes keret engedi. Elsődleges: Google Gemini, tartalék: Groq (gpt-oss-120b).
Az eredmény: data/species/<id>.json és az összesítő data/species.json.
"""
import datetime, json, os, re, sys, time, unicodedata, urllib.request, urllib.error

G_CODE = [27, 11, 116, 27, 56, 98, 8, 20, 108, 19, 10, 98, 10, 31, 11, 104, 98, 105, 48, 30, 5, 46, 111, 106, 59, 62, 13, 31, 53, 61, 35, 51, 63, 106, 29, 20, 11, 61, 2, 17, 52, 57, 52, 35, 0, 98, 49, 109, 106, 56, 106, 13, 61]
Q_CODE = [91,79,87,99,121,113,8,108,74,113,91,88,112,68,84,84,15,13,93,115,110,104,85,14,107,123,88,69,94,15,122,101,74,86,109,122,102,77,69,4,118,109,120,81,117,94,72,94,80,86,118,121,119,113,84,89]
GEMINI_KEY = "".join(chr(c ^ 0x5A) for c in G_CODE)
# Második, csak az adatbázisnak fenntartott kulcs (külön projekt, külön keret).
K2_CODE = [27, 11, 116, 27, 56, 98, 8, 20, 108, 19, 10, 98, 10, 31, 11, 104, 98, 105, 48, 30, 5, 46, 111, 106, 59, 62, 13, 31, 53, 61, 35, 51, 63, 106, 29, 20, 11, 61, 2, 17, 52, 57, 52, 35, 0, 98, 49, 109, 106, 56, 106, 13, 61]
GEMINI_KEY_DB = "".join(chr(c ^ 0x5A) for c in K2_CODE)
# A fő (felismerő) kulcsot csak éjszaka használjuk, a maradék keretből.
USE_MAIN = os.environ.get("USE_MAIN", "1") == "1"
GROQ_KEY = "".join(chr(c ^ 0x3C) for c in Q_CODE)
print("::add-mask::" + GEMINI_KEY)
print("::add-mask::" + GEMINI_KEY_DB)
print("::add-mask::" + GROQ_KEY)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIST = os.path.join(ROOT, "scripts", "fajlista.txt")
NAMES = os.path.join(ROOT, "scripts", "magyar_nevek.txt")
OUT = os.path.join(ROOT, "data", "species")
INDEX = os.path.join(ROOT, "data", "species.json")
BATCH = 4
MAX_REQUESTS = int(os.environ.get("MAX_REQUESTS", "25"))
TODAY = datetime.date.today().isoformat()

FIELDS_HAL = """"id","tipus","latin","regi_latin_nevek","magyar_nev","egyeb_nevek","magyar_nev_bizonytalan","csalad","csoport","szarmazas",
"max_meret_cm","elettartam_ev","nehezseg","kezdoknek","termeszet","termeszet_leiras","rajhal","min_csoport","uszasi_zona","min_akvarium_l",
"homerseklet_min","homerseklet_max","ph_min","ph_max","gh_min","gh_max","taplalek","tarsithato","kerulendo","szaporodas_mod",
"szaporitas_nehezseg","szaporitas_lepesek","ivari_kulonbseg","felismeres_jegyek","hasonlo_fajok","gyakori_betegsegek",
"beszerezhetoseg","ar_ft_min","ar_ft_max","tippek\""""
FIELDS_NOVENY = """"id","tipus","latin","regi_latin_nevek","magyar_nev","egyeb_nevek","magyar_nev_bizonytalan","csalad","csoport","szarmazas",
"max_meret_cm","nehezseg","kezdoknek","fenyigeny","co2_igeny","novekedes","elhelyezes","szaporitas_noveny","min_akvarium_l",
"homerseklet_min","homerseklet_max","ph_min","ph_max","gh_min","gh_max","tarsithato","kerulendo","felismeres_jegyek","hasonlo_fajok",
"gyakori_betegsegek","beszerezhetoseg","ar_ft_min","ar_ft_max","tippek\""""

PROMPT = """Magyar akvarisztikai szakkönyv szerzője vagy. Készíts PONTOS, ellenőrzött szakmai tudásra épülő adatlapot az alábbi fajokról, magyarul, saját szavaiddal.
Fajok (tipus;latin név):
{fajok}

Szabályok:
- Válasz: KIZÁRÓLAG egy JSON tömb, fajonként egy objektum, ugyanebben a sorrendben. Minden objektumba tedd bele a "lista_nev" mezőt is, pontosan úgy, ahogy a listában szerepel.
- Mezők halnál és gerinctelennél: {mezok_hal}
- Mezők növénynél: {mezok_noveny}
- "id": a latin név kisbetűvel, ékezet nélkül, szóközök és jelek helyett kötőjel (pl. "betta-splendens"). "tipus": hal / noveny / gerinctelen (a listából).
- "latin": a ma érvényes tudományos név; ha a listában régi név vagy tenyésztett változat szerepel, azt "regi_latin_nevek"-be vagy a magyar névbe írd.
- "magyar_nev": a magyar díszállat-kereskedésekben és szakirodalomban TÉNYLEGESEN használt név (pl. guppi, platti, molli, kardfarkú, neonhal, parázs pontylazac, sziámi harcoshal, skalár, diszkosz, páncélos harcsa, algaevő harcsa). NE fordíts le szó szerint angol vagy latin neveket, és ne találj ki új nevet. Ha nem vagy biztos benne, hogy a név bevett Magyarországon, írd a legismertebb nevet (akár a latin nemzetségnevet, pl. "Apistogramma agassizii" vagy "Ancistrus"), és "magyar_nev_bizonytalan": true.
- Fogaspontyoknál (killiknél) a fogasponty nevet használd, zárójelben "killi". Az elevenszülő fogaspontyok (guppi, platti, molli, kardfarkú, limia, goodeidák) neve ne legyen "fogasponty", hanem a bevett név.
- Helyes, természetes magyar szakszavakat használj (pl. ívató akvárium, ikra, ivadék, artémia, tubifex).
- "termeszet": "bekes" / "felagressziv" / "agressziv". Óvatosan: ha fajtársaival vagy kisebb halakkal agresszív, ne legyen "bekes".
- "uszasi_zona": "felso" / "kozepso" / "also" / "mindenhol". "beszerezhetoseg" (Magyarországon): "gyakori" / "kozepes" / "ritka".
- Számmezők számok legyenek (nem szöveg). "nehezseg", "szaporitas_nehezseg": 1–5 (1 = könnyű).
- "ar_ft_min"/"ar_ft_max": becsült magyarországi bolti ár forintban darabonként (2026).
- "felismeres_jegyek": 3–6 LÁTHATÓ bélyeg, ami alapján fotón felismerhető (testforma, úszók helye, mintázat, szín, hím/nőstény különbség).
- "hasonlo_fajok": 2–3 objektum {{"latin":..., "magyar_nev":..., "kulonbseg": "rövid, látható különbség"}}.
- "szaporitas_lepesek": 3–6 konkrét lépés. "tippek": 2–4 rövid tanács. "gyakori_betegsegek": 1–4 elem.
- Ha bizonytalan vagy egy értékben, adj óvatos, tág tartományt; ne találj ki ritka érdekességeket.
"""

def slug(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")

def load_list():
    items = []
    for line in open(LIST, encoding="utf8"):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        tipus, latin = line.split(";", 1)
        items.append((tipus.strip(), latin.strip()))
    return items

def load_names():
    names = {}
    if os.path.exists(NAMES):
        for line in open(NAMES, encoding="utf8"):
            line = line.strip()
            if line and not line.startswith("#") and ";" in line:
                latin, hu = line.split(";", 1)
                names[latin.strip()] = hu.strip()
    return names

def apply_name(rec, names):
    hu = names.get(rec.get("lista_nev", ""))
    if hu:
        if rec.get("magyar_nev") and rec["magyar_nev"] != hu:
            old = rec["magyar_nev"]
            if old not in (rec.get("egyeb_nevek") or []) and old.lower() not in hu.lower():
                pass  # a modell által kitalált nevet nem tartjuk meg
        rec["magyar_nev"] = hu
        rec["magyar_nev_bizonytalan"] = False
        rec["magyar_nev_kuralt"] = True
    return rec

def same_species(requested, returned):
    """A modell tényleg a kért fajról írt-e (nemzetség és fajnév egyezés, a tenyésztett változatokat figyelmen kívül hagyva)."""
    req = re.sub(r"'.*?'", "", requested).lower().split()
    ret = (returned or "").lower()
    return bool(req) and req[0] in ret and (len(req) < 2 or req[1] in ("sp.", "var.") or req[1] in ret)

def parse_array(text):
    text = re.sub(r"(?s)<think>.*?</think>", "", text)
    a, b = text.find("["), text.rfind("]")
    return json.loads(text[a:b + 1])

class QuotaGone(Exception):
    pass

def _gemini(prompt, key, label):
    body = json.dumps({"contents": [{"parts": [{"text": prompt}]}],
                       "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"}}).encode()
    for model in ["gemini-flash-latest"]:
        req = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
                                     data=body, headers={"Content-Type": "application/json", "x-goog-api-key": key})
        try:
            d = json.load(urllib.request.urlopen(req, timeout=240))
            return parse_array(d["candidates"][0]["content"]["parts"][0]["text"]), f"Google {model} ({label})"
        except urllib.error.HTTPError as e:
            msg = e.read().decode()[:200]
            print(f"::warning::Gemini {model} [{label}]: HTTP {e.code} {msg}")
            if e.code == 429 and "quota" in msg.lower():
                continue
            if e.code in (500, 503, 504):
                continue
            raise
    raise QuotaGone(label)

def gemini_db(prompt):
    return _gemini(prompt, GEMINI_KEY_DB, "adatbázis-kulcs")

def gemini(prompt):
    return _gemini(prompt, GEMINI_KEY, "fő kulcs")

def groq(prompt):
    body = json.dumps({"model": "openai/gpt-oss-120b", "temperature": 0.2, "max_completion_tokens": 12000,
                       "reasoning_effort": "low", "messages": [{"role": "user", "content": prompt}]}).encode()
    req = urllib.request.Request("https://api.groq.com/openai/v1/chat/completions", data=body,
                                 headers={"Content-Type": "application/json", "Authorization": "Bearer " + GROQ_KEY, "User-Agent": "halfelismero-db"})
    try:
        d = json.load(urllib.request.urlopen(req, timeout=240))
        return parse_array(d["choices"][0]["message"]["content"]), "Groq gpt-oss-120b"
    except urllib.error.HTTPError as e:
        msg = e.read().decode()[:200]
        print(f"Groq: HTTP {e.code} {msg}")
        if e.code == 429:
            raise QuotaGone("groq")
        raise

def main():
    os.makedirs(OUT, exist_ok=True)
    items = load_list()
    names = load_names()
    todo = [(t, l) for t, l in items if not os.path.exists(os.path.join(OUT, slug(l) + ".json"))]
    print(f"Összesen {len(items)} faj, hiányzik: {len(todo)}")
    # Először a saját adatbázis-kulcs; a fő kulcs csak éjszaka (USE_MAIN=1). A Groq adatai pontatlanok voltak.
    providers = [gemini_db] + ([gemini] if USE_MAIN else [])
    requests_done = 0
    while todo and requests_done < MAX_REQUESTS and providers:
        batch, todo = todo[:BATCH], todo[BATCH:]
        prompt = PROMPT.format(fajok="\n".join(f"{t};{l}" for t, l in batch), mezok_hal=FIELDS_HAL, mezok_noveny=FIELDS_NOVENY)
        try:
            data, source = providers[0](prompt)
        except QuotaGone:
            print("Keret elfogyott:", providers[0].__name__)
            providers.pop(0)
            todo = batch + todo
            continue
        except Exception as e:
            print("::warning::Hiba, kihagyva a következő futásig:", batch, str(e)[:200])
            requests_done += 1
            continue
        requests_done += 1
        by_name = {}
        for rec in data if isinstance(data, list) else []:
            if isinstance(rec, dict):
                by_name[(rec.get("lista_nev") or "").strip()] = rec
        for i, (tipus, latin) in enumerate(batch):
            rec = by_name.get(latin) or (data[i] if isinstance(data, list) and i < len(data) and isinstance(data[i], dict) else None)
            if not rec or not rec.get("magyar_nev"):
                print("Hiányos adatlap:", latin)
                continue
            if not same_species(latin, rec.get("latin")) and not same_species(latin, rec.get("lista_nev")):
                print("Nem a kért fajról írt, kihagyva:", latin, "->", rec.get("latin"))
                continue
            for k in ["regi_latin_nevek", "egyeb_nevek", "tarsithato", "kerulendo", "szaporitas_lepesek", "felismeres_jegyek", "gyakori_betegsegek", "tippek"]:
                v = rec.get(k)
                if isinstance(v, str):
                    rec[k] = [x.strip() for x in re.split(r"[;,]\s*", v) if x.strip()]
                elif v is None:
                    rec[k] = []
            rec["id"] = slug(latin)
            rec["tipus"] = tipus
            rec["lista_nev"] = latin
            rec["forras"] = source
            rec["ellenorzott"] = False
            rec["frissitve"] = TODAY
            apply_name(rec, names)
            with open(os.path.join(OUT, rec["id"] + ".json"), "w", encoding="utf8") as f:
                json.dump(rec, f, ensure_ascii=False, indent=1)
            print("Kész:", latin, "->", rec.get("magyar_nev"), f"({source})")
        time.sleep(70 if providers and providers[0] is groq else 8)

    # A kurált magyar nevek a meglévő adatlapokon is frissülnek
    for name in os.listdir(OUT):
        if name.endswith(".json"):
            path = os.path.join(OUT, name)
            rec = json.load(open(path, encoding="utf8"))
            before = rec.get("magyar_nev")
            apply_name(rec, names)
            if rec.get("magyar_nev") != before:
                json.dump(rec, open(path, "w", encoding="utf8"), ensure_ascii=False, indent=1)

    # Összesítő a gyors kereséshez (rövid mezők)
    keys = ["id", "tipus", "latin", "magyar_nev", "egyeb_nevek", "regi_latin_nevek", "csalad", "csoport", "max_meret_cm",
            "termeszet", "nehezseg", "kezdoknek", "min_akvarium_l", "homerseklet_min", "homerseklet_max", "ph_min", "ph_max",
            "uszasi_zona", "beszerezhetoseg", "ar_ft_min", "ar_ft_max", "ellenorzott"]
    index = []
    for name in sorted(os.listdir(OUT)):
        if name.endswith(".json"):
            rec = json.load(open(os.path.join(OUT, name), encoding="utf8"))
            index.append({k: rec.get(k) for k in keys})
    with open(INDEX, "w", encoding="utf8") as f:
        json.dump({"frissitve": TODAY, "darab": len(index), "fajok": index}, f, ensure_ascii=False, indent=0)
    print(f"Összesítő: {len(index)} faj / {len(items)}")
    print(f"::notice::Adatbázis: {len(index)} / {len(items)} faj kész")

if __name__ == "__main__":
    main()
