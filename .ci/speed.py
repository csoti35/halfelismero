import base64, json, re, textwrap, time, urllib.request, urllib.error
src = open("app/src/main/java/com/example/data/remote/GeminiFishIdentifier.kt", encoding="utf8").read()
prompt = textwrap.dedent(re.search(r'private val fishPrompt = """(.*?)"""\.trimIndent\(\)', src, re.S).group(1)).strip()
ks = open("app/src/main/java/com/example/data/local/ApiKeyStore.kt").read()
gk = "".join(chr(int(x) ^ 0x5A) for x in re.search(r"builtInCode = intArrayOf\(([^)]*)\)", ks).group(1).split(","))
qk = "".join(chr(int(x) ^ 0x3C) for x in re.search(r"groqCode = intArrayOf\(([^)]*)\)", ks).group(1).split(","))
print("::add-mask::" + gk); print("::add-mask::" + qk)
img = base64.b64encode(open(".ci/teszt-hal.jpg", "rb").read()).decode()
def gem(level, model="gemini-flash-latest"):
    cfg = {"temperature": 0.2, "responseMimeType": "application/json", "thinkingConfig": {"thinkingLevel": level}}
    body = json.dumps({"contents": [{"parts": [{"inlineData": {"mimeType": "image/jpeg", "data": img}}, {"text": prompt}]}], "generationConfig": cfg}).encode()
    r = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", data=body, headers={"Content-Type": "application/json", "x-goog-api-key": gk})
    d = json.load(urllib.request.urlopen(r, timeout=150)); return d["candidates"][0]["content"]["parts"][0]["text"]
def groq(extra):
    content = [{"type": "text", "text": prompt + "\n\nCsak a JSON objektumot írd ki, semmi mást."}, {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64," + img}}]
    body = json.dumps({"model": "qwen/qwen3.8-27b", "temperature": 0.2, "max_completion_tokens": 3000, "messages": [{"role": "user", "content": content}], **extra}).encode()
    r = urllib.request.Request("https://api.groq.com/openai/v1/chat/completions", data=body, headers={"Content-Type": "application/json", "Authorization": "Bearer " + qk, "User-Agent": "ci"})
    d = json.load(urllib.request.urlopen(r, timeout=150)); return d["choices"][0]["message"]["content"]
tests = [("Gemini minimal", lambda: gem("minimal")), ("Gemini low", lambda: gem("low")),
         ("Groq alap", lambda: groq({})), ("Groq reasoning=none", lambda: groq({"reasoning_effort": "none"})),
         ("Groq reasoning hidden", lambda: groq({"reasoning_format": "hidden"}))]
for name, fn in tests:
    t = time.time()
    try:
        txt = fn(); dt = time.time() - t
        txt = re.sub(r"(?s)<think>.*?</think>", "", txt); a, b = txt.find("{"), txt.rfind("}")
        j = json.loads(txt[a:b+1])
        print(f"::warning::SEB {name}: {dt:.1f} mp | {j.get('magyarNev')} | {j.get('tudomanyosNev')} | {j.get('biztonsagSzazalek')}%")
    except urllib.error.HTTPError as e:
        print(f"::warning::SEB {name}: HTTP {e.code} {time.time()-t:.1f} mp {e.read().decode()[:200]}")
    except Exception as e:
        print(f"::warning::SEB {name}: hiba {time.time()-t:.1f} mp {str(e)[:150]}")
