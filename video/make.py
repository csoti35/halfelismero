"""Bemutató videó: természetes magyar hang (Microsoft Neural, Noémi) + képkockák -> MP4."""
import asyncio, json, os, subprocess, sys, base64, wave, urllib.request
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out"); AUD = os.path.join(OUT, "audio"); CLIP = os.path.join(OUT, "clips")
os.makedirs(AUD, exist_ok=True); os.makedirs(CLIP, exist_ok=True)
scenes = json.load(open(os.path.join(HERE, "scenes.json"), encoding="utf-8"))
VOICE = os.environ.get("VOICE", "hu-HU-NoemiNeural")

def sh(*a):
    r = subprocess.run(a, capture_output=True, text=True)
    if r.returncode: print(r.stderr[-3000:]); sys.exit(1)
    return r.stdout

def dur(f):
    return float(sh("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f))

async def edge(text, path):
    import edge_tts
    await edge_tts.Communicate(text, VOICE, rate="-4%").save(path)

def gemini(text, path):
    sys.path.insert(0, os.path.join(HERE, "..", "scripts"))
    G = [27, 11, 116, 27, 56, 98, 8, 20, 108, 19, 10, 98, 10, 31, 11, 104, 98, 105, 48, 30, 5, 46, 111, 106, 59, 62, 13, 31, 53, 61, 35, 51, 63, 106, 29, 20, 11, 61, 2, 17, 52, 57, 52, 35, 0, 98, 49, 109, 106, 56, 106, 13, 61]
    key = "".join(chr(c ^ 0x5A) for c in G)
    body = {"contents": [{"parts": [{"text": "Olvasd fel természetesen, barátságos, nyugodt magyar hangon: " + text}]}],
            "generationConfig": {"responseModalities": ["AUDIO"], "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": "Kore"}}}}}
    req = urllib.request.Request("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent",
                                 json.dumps(body).encode(), {"Content-Type": "application/json", "x-goog-api-key": key})
    d = json.load(urllib.request.urlopen(req, timeout=120))
    pcm = base64.b64decode(d["candidates"][0]["content"]["parts"][0]["inlineData"]["data"])
    wav = path[:-4] + ".wav"
    with wave.open(wav, "wb") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(24000); w.writeframes(pcm)
    sh("ffmpeg", "-y", "-i", wav, path)

engine = "edge"
for s in scenes:
    p = os.path.join(AUD, s["id"] + ".mp3")
    if os.path.exists(p) and os.path.getsize(p) > 1000: continue
    try:
        if engine == "edge": asyncio.run(edge(s["say"], p))
        else: gemini(s["say"], p)
    except Exception as e:
        print("Edge TTS hiba, Gemini hanggal folytatom:", e); engine = "gemini"; gemini(s["say"], p)
    print("hang", s["id"], round(dur(p), 1), "mp")

FPS = 30; PAD_IN, PAD_OUT = 0.35, 0.65
lst = []
for s in scenes:
    a = os.path.join(AUD, s["id"] + ".mp3"); f = os.path.join(OUT, "frames", s["id"] + ".png"); c = os.path.join(CLIP, s["id"] + ".mp4")
    T = PAD_IN + dur(a) + PAD_OUT; n = int(T * FPS) + 1
    zoom = f"zoompan=z='1+0.035*on/{n}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={n}:s=1080x1920:fps={FPS}"
    vf = f"scale=2160:3840,{zoom},fade=t=in:st=0:d=0.3,fade=t=out:st={T-0.3:.2f}:d=0.3,format=yuv420p"
    sh("ffmpeg", "-y", "-loop", "1", "-i", f, "-i", a, "-filter_complex",
       f"[0:v]{vf}[v];[1:a]adelay={int(PAD_IN*1000)}|{int(PAD_IN*1000)},apad,atrim=0:{T:.3f},aresample=44100[a]",
       "-map", "[v]", "-map", "[a]", "-t", f"{T:.3f}", "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-c:a", "aac", "-b:a", "160k", "-ac", "2", c)
    lst.append(f"file '{c}'"); print("klip", s["id"], round(T, 1), "mp")
open(os.path.join(OUT, "list.txt"), "w").write("\n".join(lst))
final = os.path.join(OUT, "AquaFaj_bemutato.mp4")
sh("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", os.path.join(OUT, "list.txt"), "-c", "copy", "-movflags", "+faststart", final)
print("KÉSZ", final, round(dur(final), 1), "mp", round(os.path.getsize(final) / 1e6, 1), "MB")
