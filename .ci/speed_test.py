# Sebességteszt: ugyanaz a kép és prompt, különböző beállításokkal.
import base64, json, re, textwrap, time, urllib.request, urllib.error
src = open("app/src/main/java/com/example/data/remote/GeminiFishIdentifier.kt", encoding="utf8").read()
prompt = textwrap.dedent(re.search(r'val prompt = """(.*?)"""\.trimIndent\(\)', src, re.S).group(1)).strip()
ks = open("app/src/main/java/com/example/data/local/ApiKeyStore.kt").read()
key = "".join(chr(int(x) ^ 0x5A) for x in re.search(r"intArrayOf\(([^)]*)\)", ks).group(1).split(","))
img = base64.b64encode(open(".ci/teszt-hal.jpg", "rb").read()).decode()
variants = [
    ("flash-latest alap", "gemini-flash-latest", {}),
    ("flash-latest think=low", "gemini-flash-latest", {"thinkingConfig": {"thinkingLevel": "low"}}),
    ("flash-latest think=minimal", "gemini-flash-latest", {"thinkingConfig": {"thinkingLevel": "minimal"}}),
    ("flash-latest budget=0", "gemini-flash-latest", {"thinkingConfig": {"thinkingBudget": 0}}),
    ("2.5-flash budget=0", "gemini-2.5-flash", {"thinkingConfig": {"thinkingBudget": 0}}),
    ("flash-lite-latest", "gemini-flash-lite-latest", {}),
]
for name, model, extra in variants:
    for rep in range(2):
        cfg = {"temperature": 0.2, "responseMimeType": "application/json", **extra}
        body = json.dumps({"contents": [{"parts": [{"inlineData": {"mimeType": "image/jpeg", "data": img}}, {"text": prompt}]}], "generationConfig": cfg}).encode()
        req = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", data=body, headers={"Content-Type": "application/json", "x-goog-api-key": key})
        t = time.time()
        try:
            d = json.load(urllib.request.urlopen(req, timeout=150)); dt = time.time() - t
            r = json.loads(d["candidates"][0]["content"]["parts"][0]["text"])
            print(f"::warning::SEB {name} #{rep+1}: {dt:.1f} mp | {r.get('magyarNev')} | {r.get('tudomanyosNev')} | {r.get('biztonsagSzazalek')}%")
        except urllib.error.HTTPError as e:
            msg = e.read().decode()[:160].replace("\n", " ")
            print(f"::warning::SEB {name} #{rep+1}: HTTP {e.code} {time.time()-t:.1f} mp {msg}")
        except Exception as e:
            print(f"::warning::SEB {name} #{rep+1}: hiba {str(e)[:150]}")
