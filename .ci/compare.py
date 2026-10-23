import base64, json, re, textwrap, time, urllib.request, urllib.error
src = open("app/src/main/java/com/example/data/remote/GeminiFishIdentifier.kt", encoding="utf8").read()
prompt = textwrap.dedent(re.search(r'private val fishPrompt = """(.*?)"""\.trimIndent\(\)', src, re.S).group(1)).strip()
ks = open("app/src/main/java/com/example/data/local/ApiKeyStore.kt").read()
qk = "".join(chr(int(x) ^ 0x3C) for x in re.search(r"groqCode = intArrayOf\(([^)]*)\)", ks).group(1).split(","))
print("::add-mask::" + qk)
H = {"Content-Type": "application/json", "Authorization": "Bearer " + qk, "User-Agent": "ci"}
def call(body, timeout=150):
    r = urllib.request.Request("https://api.groq.com/openai/v1/chat/completions", data=json.dumps(body).encode(), headers=H)
    return json.load(urllib.request.urlopen(r, timeout=timeout))["choices"][0]["message"]["content"]
def js(t):
    t = re.sub(r"(?s)<think>.*?</think>", "", t); return json.loads(t[t.find("{"):t.rfind("}")+1])
DESC = """You are an expert ichthyologist. Describe ONLY what is visible about the most prominent fish in this aquarium photo, in precise anatomical detail, in English:
body shape and depth (elongated/cylindrical, laterally compressed, deep-bodied), estimated length relative to plants/gravel, exact colours and pattern,
dorsal fin (position, size, shape), anal fin (length, where it starts), pelvic fins (small or long thread-like), caudal fin shape (rounded, forked, lyre, veil/fan, long flowing),
mouth position (upturned/terminal), any barbels or adipose fin, where in the water column the fish is (surface/mid/bottom), and how many similar fish are visible.
Then list the 5 most likely aquarium species (scientific names) with a one-line reason each. Be concrete."""
DECIDE = """You are a world-class aquarium fish taxonomist. Based on this careful visual description of a fish photographed in a home aquarium, determine the most likely species.
Weigh the diagnostic features (fin positions, pelvic fin type, caudal shape, body shape, colour) heavily; consider common aquarium species first but do not ignore rarer hobby fish such as killifish.
Description:
---
%s
---
Answer with ONLY a JSON object: {"tudomanyosNev": "...", "magyarNev": "<Hungarian aquarium name>", "biztonsagSzazalek": <0-100>, "alternativFajok": ["...", "..."]}"""
def img(name): return "data:image/jpeg;base64," + base64.b64encode(open(f".ci/{name}.jpg", "rb").read()).decode()
for name in ["betta", "killi", "sarga"]:
    # A) qwen közvetlenül, gondolkodással
    for label, extra in [("qwen reasoning", {"reasoning_effort": "default"})]:
        t = time.time()
        try:
            j = js(call({"model": "qwen/qwen3.8-27b", "temperature": 0.2, "max_completion_tokens": 6000, **extra,
                         "messages": [{"role": "user", "content": [{"type": "text", "text": prompt + "\n\nCsak a JSON objektumot írd ki."}, {"type": "image_url", "image_url": {"url": img(name)}}]}]}))
            print(f"::warning::CMP {name} | {label}: {time.time()-t:.1f}s | {j.get('magyarNev')} | {j.get('tudomanyosNev')} | {j.get('biztonsagSzazalek')}")
        except urllib.error.HTTPError as e:
            print(f"::warning::CMP {name} | {label}: HTTP {e.code} {e.read().decode()[:160]}")
        except Exception as e:
            print(f"::warning::CMP {name} | {label}: hiba {str(e)[:150]}")
        time.sleep(62)
    # B) kétlépcsős: qwen leírja, gpt-oss-120b dönt
    t = time.time()
    try:
        d = call({"model": "qwen/qwen3.8-27b", "temperature": 0.1, "max_completion_tokens": 1200,
                  "messages": [{"role": "user", "content": [{"type": "text", "text": DESC}, {"type": "image_url", "image_url": {"url": img(name)}}]}]})
        d = re.sub(r"(?s)<think>.*?</think>", "", d)
        j = js(call({"model": "openai/gpt-oss-120b", "temperature": 0.2, "max_completion_tokens": 3000, "reasoning_effort": "medium",
                     "messages": [{"role": "user", "content": DECIDE % d}]}))
        print(f"::warning::CMP {name} | 2-lépcsős: {time.time()-t:.1f}s | {j.get('magyarNev')} | {j.get('tudomanyosNev')} | {j.get('biztonsagSzazalek')} | leírás: {d[:250].replace(chr(10),' ')}")
    except urllib.error.HTTPError as e:
        print(f"::warning::CMP {name} | 2-lépcsős: HTTP {e.code} {e.read().decode()[:160]}")
    except Exception as e:
        print(f"::warning::CMP {name} | 2-lépcsős: hiba {str(e)[:150]}")
    time.sleep(62)
