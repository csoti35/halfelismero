import base64, json, re, textwrap, time, urllib.request, urllib.error
src = open("app/src/main/java/com/example/data/remote/GeminiFishIdentifier.kt", encoding="utf8").read()
prompt = textwrap.dedent(re.search(r'private val fishPrompt = """(.*?)"""\.trimIndent\(\)', src, re.S).group(1)).strip()
ks = open("app/src/main/java/com/example/data/local/ApiKeyStore.kt").read()
qk = "".join(chr(int(x) ^ 0x3C) for x in re.search(r"groqCode = intArrayOf\(([^)]*)\)", ks).group(1).split(","))
print("::add-mask::" + qk)
for name in ["killi", "sarga"]:
    img = base64.b64encode(open(f".ci/{name}.jpg", "rb").read()).decode()
    content = [{"type": "text", "text": prompt + "\n\nCsak a JSON objektumot írd ki, semmi mást."}, {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64," + img}}]
    body = json.dumps({"model": "qwen/qwen3.8-27b", "temperature": 0.2, "max_completion_tokens": 3000, "messages": [{"role": "user", "content": content}]}).encode()
    t = time.time()
    try:
        r = urllib.request.Request("https://api.groq.com/openai/v1/chat/completions", data=body, headers={"Content-Type": "application/json", "Authorization": "Bearer " + qk, "User-Agent": "ci"})
        txt = json.load(urllib.request.urlopen(r, timeout=120))["choices"][0]["message"]["content"]
        txt = re.sub(r"(?s)<think>.*?</think>", "", txt); j = json.loads(txt[txt.find("{"):txt.rfind("}")+1])
        print(f"::warning::TESZT {name}: {time.time()-t:.1f} mp | {j.get('magyarNev')} | {j.get('tudomanyosNev')} | {j.get('biztonsagSzazalek')}% | látott: {str(j.get('lathatoBelyegek'))[:200]} | alt: {j.get('alternativFajok')}")
    except urllib.error.HTTPError as e:
        print(f"::warning::TESZT {name}: HTTP {e.code} {e.read().decode()[:200]}")
    except Exception as e:
        print(f"::warning::TESZT {name}: hiba {str(e)[:200]}")
    time.sleep(65)  # percenkénti tokenkeret miatt
