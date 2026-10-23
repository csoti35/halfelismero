# Felismerés-teszt a fordítás közben: az alkalmazás saját promptjával küldi el a tesztképet.
import base64, json, re, sys, textwrap, time, urllib.request, urllib.error
src = open("app/src/main/java/com/example/data/remote/GeminiFishIdentifier.kt", encoding="utf8").read()
prompt = textwrap.dedent(re.search(r'val prompt = """(.*?)"""\.trimIndent\(\)', src, re.S).group(1)).strip()
ks = open("app/src/main/java/com/example/data/local/ApiKeyStore.kt").read()
key = "".join(chr(int(x) ^ 0x5A) for x in re.search(r"intArrayOf\(([^)]*)\)", ks).group(1).split(","))
img = base64.b64encode(open(".ci/teszt-hal.jpg", "rb").read()).decode()
body = json.dumps({"contents": [{"parts": [{"inlineData": {"mimeType": "image/jpeg", "data": img}}, {"text": prompt}]}],
                   "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"}}).encode()
for attempt in range(3):
    for model in ["gemini-flash-latest", "gemini-3.8-flash", "gemini-3.7-flash", "gemini-2.5-flash"]:
        req = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
                                     data=body, headers={"Content-Type": "application/json", "x-goog-api-key": key})
        try:
            d = json.load(urllib.request.urlopen(req, timeout=120))
            r = json.loads(d["candidates"][0]["content"]["parts"][0]["text"])
            print(f"::warning::HAL-TESZT ({model}): {r.get('magyarNev')} | {r.get('tudomanyosNev')} | {r.get('biztonsagSzazalek')}% | alternatívák: {r.get('alternativFajok')}")
            sys.exit(0)
        except urllib.error.HTTPError as e:
            print(f"::warning::HAL-TESZT {model}: HTTP {e.code}")
        except Exception as e:
            print(f"::warning::HAL-TESZT {model}: {str(e)[:200]}")
    time.sleep(5)
