# Akvarisztika zsebkönyv – fajadatbázis

Ez a mappa az alkalmazás felhős fajadatbázisa. Az adatok a GitHub Pages-en érhetők el:

- összesítő: `https://csoti35.github.io/halfelismero/data/species.json`
- egy faj: `https://csoti35.github.io/halfelismero/data/species/<id>.json`

## Forrás és ellenőrzés

Az adatlapokat mesterséges intelligencia (Google Gemini, tartalékként Groq) írta saját szöveggel, a `scripts/fajlista.txt` alapján.
Más weboldalak (pl. diszhal.info, FishBase) szövegét **nem** tartalmazza.
Minden adatlap `ellenorzott: false` állapotban indul; akvarista átnézése után `true`-ra kell állítani.

## Egy adatlap mezői

| mező | típus | jelentés |
|---|---|---|
| `id` | szöveg | a latin névből képzett azonosító (pl. `betta-splendens`) |
| `tipus` | `hal` / `noveny` / `gerinctelen` | |
| `latin` | szöveg | érvényes tudományos név |
| `regi_latin_nevek` | lista | szinonimák, régi nevek (pl. Cynolebias) |
| `magyar_nev` | szöveg | a magyar akvarisztikában használt név |
| `egyeb_nevek` | lista | további magyar/bolti nevek |
| `magyar_nev_bizonytalan` | igen/nem | ha nincs bevett magyar név |
| `csalad` | szöveg | család (latinul) |
| `csoport` | szöveg | pl. elevenszülő, pontylazac, fogasponty (killi), sügér, harcsa, szárnövény |
| `szarmazas` | szöveg | élőhely |
| `max_meret_cm` | szám | halnál testhossz, növénynél magasság |
| `elettartam_ev` | szöveg | |
| `nehezseg` | 1–5 | tartás nehézsége (1 = kezdő) |
| `kezdoknek` | igen/nem | |
| `termeszet` | `bekes` / `felagressziv` / `agressziv` | halaknál, gerincteleneknél |
| `termeszet_leiras` | szöveg | |
| `rajhal`, `min_csoport` | igen/nem, szám | |
| `uszasi_zona` | `felso` / `kozepso` / `also` / `mindenhol` | |
| `min_akvarium_l` | szám | |
| `homerseklet_min`, `homerseklet_max` | °C | |
| `ph_min`, `ph_max` | szám | |
| `gh_min`, `gh_max` | °dGH | |
| `taplalek` | szöveg | |
| `tarsithato`, `kerulendo` | lista | csoportok vagy fajok, rövid indokkal |
| `szaporodas_mod` | szöveg | pl. elevenszülő, ikrázó, szájköltő, egynyári (tőzegben fejlődő ikra) |
| `szaporitas_nehezseg` | 1–5 | |
| `szaporitas_lepesek` | lista | |
| `ivari_kulonbseg` | szöveg | |
| `felismeres_jegyek` | lista | a fotós felismeréshez döntő látható bélyegek |
| `hasonlo_fajok` | lista `{latin, magyar_nev, kulonbseg}` | összetéveszthető fajok |
| `gyakori_betegsegek` | lista | a fajra jellemző betegségek, érzékenységek |
| `fenyigeny`, `co2_igeny`, `novekedes`, `elhelyezes`, `szaporitas_noveny` | szöveg | csak növényeknél |
| `beszerezhetoseg` | `gyakori` / `kozepes` / `ritka` | Magyarországon |
| `ar_ft_min`, `ar_ft_max` | szám | becsült bolti ár / db |
| `tippek` | lista | tartási tanácsok |
| `forras` | szöveg | melyik modell írta |
| `ellenorzott` | igen/nem | akvarista átnézte-e |
| `frissitve` | dátum | |
