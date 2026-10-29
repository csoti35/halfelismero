// Képkockák készítése a bemutató videóhoz (1080x1920, álló).
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
const ROOT = path.resolve(__dirname, '..');
const OUT = path.join(__dirname, 'out'); fs.mkdirSync(path.join(OUT, 'shots'), { recursive: true }); fs.mkdirSync(path.join(OUT, 'frames'), { recursive: true });
const scenes = JSON.parse(fs.readFileSync(path.join(__dirname, 'scenes.json'), 'utf8'));
const { spawn } = require('child_process');
const srv = spawn('python3', ['-m', 'http.server', '8099', '--bind', '127.0.0.1'], { cwd: ROOT, stdio: 'ignore' });
process.on('exit', () => srv.kill());
process.on('unhandledRejection', e => { console.log('::error::' + String(e && e.stack || e).slice(0, 600).replace(/\n/g, ' | ')); process.exit(1); });
const APP = 'http://127.0.0.1:8099/zsebkonyv/index.html';
const ICON = 'data:image/png;base64,' + fs.readFileSync(path.join(ROOT, 'icons/icon-512.png')).toString('base64');
const URL_TXT = 'csoti35.github.io/halfelismero/zsebkonyv';

const FISH_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
<defs><linearGradient id="w" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0b4f6c"/><stop offset="1" stop-color="#05202e"/></linearGradient>
<linearGradient id="b" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#c9d6e0"/><stop offset="1" stop-color="#eef3f6"/></linearGradient></defs>
<rect width="1200" height="900" fill="url(#w)"/>
${Array.from({length:14},(_,i)=>`<path d="M${60+i*85} 900 C ${40+i*85} 760, ${90+i*85} 680, ${70+i*85} ${560+(i%4)*40}" stroke="#1f7a4d" stroke-width="16" fill="none" opacity=".75"/>`).join('')}
<g transform="translate(600 430)">
<path d="M-330 0 C -250 -120, 150 -130, 280 -20 C 300 0, 300 10, 280 25 C 150 130, -250 120, -330 0 Z" fill="url(#b)"/>
<path d="M-300 -10 C -150 -60, 120 -60, 250 -20 L 250 0 C 120 -35, -150 -35, -300 10 Z" fill="#22d3ee"/>
<path d="M-120 20 C -40 30, 120 30, 230 10 C 220 50, 120 85, -60 80 C -110 70, -130 45, -120 20 Z" fill="#ef4444"/>
<path d="M-330 0 L -470 -110 L -440 0 L -470 110 Z" fill="#dbe4ea" opacity=".8"/>
<circle cx="215" cy="-10" r="22" fill="#111"/><circle cx="222" cy="-16" r="7" fill="#fff"/>
</g></svg>`;

const MOCK = { tipus: 'hal', magyar_nev: 'Neonhal', latin: 'Paracheirodon innesi', biztossag: 93,
  lathato: 'Karcsú, kb. 3 cm-es hal, a testén világító kékeszöld csík, a hátsó alsó részén piros sáv.',
  alternativak: [{ magyar_nev: 'Vörös neonhal (kardinálishal)', latin: 'Paracheirodon axelrodi', kulonbseg: 'Ott a piros sáv a teljes has alján végigfut.' }],
  leiras: 'Békés, csapatban élő dél-amerikai pontylazac. Legalább 8–10 egyedet tarts belőle, sűrűn növényesített, enyhén savas vízben.',
  max_meret_cm: 4, termeszet: 'bekes', ar_ft_min: 250, ar_ft_max: 450,
  adatlap: { szerzo: 'Myers, 1936', csoport: 'Pontylazacok', szarmazas: 'Dél-Amerika, az Amazonas-medence', elohely: 'Lassú folyású, lágy, savas, sötét vizű erdei patakok', testhossz: '3–4 cm', viselkedes: 'Békés rajhal, hasonló méretű békés halakkal társítható.', uszasi_zona: 'kozepso', taplalkozas: 'Apró szemű lemezes táp, fagyasztott és élő apró eleség.', medence: 'min. 54 liter', allomany: 'legalább 10 fős raj', berendezes: 'Sűrű növényzet, gyökerek, tompa fény', homerseklet: '20–26 °C', ph: '5,5–7,5', keménység: '1–10 NK°', eletkor: '5–8 év', megjegyzes: 'Hosszúkás, karcsú testén fénylő kékeszöld csík fut a szemtől a zsírúszóig, alatta élénkpiros sáv a test hátsó felén. A nőstény teltebb. Hirtelen vízváltozásra érzékeny.' },
  szaporodas: { mod: 'szabadívó, az ikrát nem gondozza', nehezseg: 'nehez', ivarok: 'A nőstény teltebb, a kék csíkja enyhén hajlott.', ivarerettseg: 'kb. 6 hónaposan', tenyeszvíz: '24–25 °C, pH 5,5–6,0, nagyon lágy víz', elokeszites: 'Kis, sötét ívató akvárium, ívórács vagy moha, tompa fény.', lefolyas: 'Hajnalban ívnak a növények közé.', utodok: '60–130 ikra, 24 óra múlva kel.', ivadekneveles: 'Infuzórium, majd artémia lárva.', szulok: 'Megeszik az ikrát – ívás után ki kell őket emelni.', tippek: 'Az ikra fényérzékeny, tartsd sötétben.' } };

const PLANT = { tipus: 'noveny', magyar_nev: 'Törpe anubiasz', latin: 'Anubias barteri var. nana', biztossag: 91,
  lathato: 'Sötétzöld, kerekded, vastag levelek rövid száron, kúszó gyökértörzs.', alternativak: [],
  leiras: 'Szinte elpusztíthatatlan, lassú növésű növény. Kőre vagy gyökérre kötve a legszebb, a talajba ne temesd a gyökértörzsét.',
  feny: 'alacsony', co2: 'nem_kell', novekedes: 'lassu', elhelyezes: 'dekoracio_ko_fa', max_magassag_cm: 10, szaporitas: 'A gyökértörzs (rizóma) kettévágásával.', allapot: null, ar_ft_min: 1500, ar_ft_max: 2800 };
const SICK = { tipus: 'betegseg', magyar_nev: 'Pontbetegség (ichthyophthiriasis)', latin: 'Ichthyophthirius multifiliis', biztossag: 88, surgosseg: 'azonnal',
  lathato: 'A leírás alapján: apró fehér pöttyök a testen, étvágytalanság, dörzsölődés.',
  alternativak: [{ magyar_nev: 'Bársonybetegség (oodinium)', latin: 'Piscinoodinium', kulonbseg: 'Finom, aranyporszerű bevonat, nem különálló pöttyök.' }],
  leiras: 'Egysejtű élősködő okozza, nagyon ragályos, ezért az egész akváriumot kezelni kell. Időben kezelve jól gyógyul.',
  teendok: ['Emeld a hőmérsékletet lassan, napi 1 fokkal 27–28 °C-ra', 'Cserélj 30% vizet, és szívd ki a talajt', 'Vedd ki az aktív szenet a szűrőből, erősítsd a levegőztetést', 'Mérd meg az ammóniát és a nitritet'],
  gyogyszerek: [{ hatoanyag: 'Malachitzöld + formalin', termek: 'JBL Punktol Plus, Sera costapur', megjegyzes: 'Garnélára és csigára veszélyes, a kezelést a tünetek eltűnése után is fejezd be.' }],
  megelozes: 'Az új halakat 2 hétig tartsd karanténban, és kerüld a hirtelen hőmérséklet-változást.', allatorvos: 'Ha 3–4 nap kezelés után sem javul, vagy egyre több hal beteg.' };
const PLANT_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900"><rect width="1200" height="900" fill="#0d3b2e"/>
<ellipse cx="600" cy="760" rx="420" ry="90" fill="#5b4636"/><path d="M380 720 C 500 700, 700 700, 820 720" stroke="#3f2f22" stroke-width="40" fill="none" stroke-linecap="round"/>
${[[-250,-260,-30],[-120,-330,-10],[20,-350,8],[150,-310,22],[260,-240,40],[-60,-210,-20],[90,-200,15]].map(([x,y,r])=>`<g transform="translate(${600+x*0.7} ${720+y}) rotate(${r})"><ellipse rx="70" ry="120" fill="#1f6b3a"/><path d="M0 -110 L0 110" stroke="#2f8a4f" stroke-width="5"/></g><path d="M${600+x*0.7} ${720+y+110} L${600+x*0.3} 715" stroke="#2a5c34" stroke-width="10"/>`).join('')}</svg>`;

async function wireCss(page) {
  if (!process.env.LOCAL_CSS) return;
  const css = fs.readFileSync(process.env.LOCAL_CSS, 'utf8');
  await page.route(/tailwindcss/, r => r.fulfill({ contentType: 'text/javascript', body: `window.tailwind={};document.addEventListener('DOMContentLoaded',()=>{const s=document.createElement('style');s.textContent=${JSON.stringify(css)};document.head.appendChild(s)})` }));
  await page.route(/gstatic|jsdelivr|cdnjs/, r => r.abort());
}

(async () => {
  const browser = await chromium.launch();
  // Hal "fotó"
  const fp = await browser.newPage({ viewport: { width: 1200, height: 900 } });
  await fp.setContent(`<body style="margin:0">${FISH_SVG}</body>`);
  const fishPng = path.join(OUT, 'fish.png'); await fp.screenshot({ path: fishPng });
  await fp.setContent(`<body style="margin:0">${PLANT_SVG}</body>`); const plantPng = path.join(OUT, 'plant.png'); await fp.screenshot({ path: plantPng }); await fp.close();

  // App képernyőképek telefonméretben
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2.5, isMobile: true, hasTouch: true, serviceWorkers: 'block' });
  ctx.on('page', () => {}); 
  const page = await ctx.newPage(); await wireCss(page);
  await page.route(/generativelanguage/, r => { const pr = r.request().postData() || ''; const ans = /halorvos/.test(pr) ? SICK : /növényhatározó/.test(pr) ? PLANT : MOCK;
    setTimeout(() => r.fulfill({ contentType: 'application/json', body: JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify(ans) }] } }] }) }), 300); });
  await page.route(/firebasejs/, r => r.abort());
  await page.addInitScript(() => {
    try { localStorage.setItem('aquaFajProSeenGuide', '1'); } catch (e) {}
  });
  page.on('framenavigated', f => { if (f === page.mainFrame()) console.log('::notice::NAV ' + f.url().slice(0, 160)); });
  await page.goto(APP); await page.waitForTimeout(2500);
  const hl = async sel => page.evaluate(s => { document.querySelectorAll('.demo-hl').forEach(e => { e.classList.remove('demo-hl'); e.style.outline = ''; e.style.boxShadow = ''; }); const e = s && document.querySelector(s); if (e) { e.classList.add('demo-hl'); e.style.outline = '3px solid #fbbf24'; e.style.outlineOffset = '3px'; e.style.boxShadow = '0 0 0 9px rgba(251,191,36,.25)'; e.style.borderRadius = '12px'; } }, sel);
  const prep = async () => { await page.evaluate(() => { document.querySelectorAll('[id$="Modal"]').forEach(m => m.classList.add('hidden')); }); await page.waitForTimeout(400); };
  const tab = async t => { await page.evaluate(t => switchTab(t), t); await page.waitForTimeout(900); await prep(); };
  const to = async sel => { await page.evaluate(s => { const e = document.querySelector(s); if (e) { const y = e.getBoundingClientRect().top + scrollY - 150; scrollTo(0, Math.max(0, y)); } }, sel); await page.waitForTimeout(500); };
  const shot = async n => page.screenshot({ path: path.join(OUT, 'shots', n + '.png') });

  // Mintaállomány: kerüljön bele egy skalár is, hogy látsszon a figyelmeztetés
  await page.evaluate(() => { appState.stock.push({ id: 9, name: 'Skalár', latin: 'Pterophyllum scalare', type: 'hal', count: 2, size: 15, aggro: 'teruletvedo' }); saveToLocalStorage && saveToLocalStorage(true); renderAll(); });
  await prep();
  await tab('tab5'); await page.evaluate(() => scrollTo(0, 0)); await page.evaluate(() => document.getElementById('scanModes').scrollIntoView({ block: 'start' })); await page.evaluate(() => scrollBy(0, -80)); await hl('#scanModes'); await shot('scan'); await hl(null);
  const inputs = await page.$$('input[type=file][accept^="image"]');
  await inputs[inputs.length - 1].setInputFiles(fishPng);
  await page.waitForTimeout(2500); await prep();
  await page.evaluate(() => { const e = document.getElementById('scanPreview'); e.scrollIntoView({ block: 'start' }); scrollBy(0, -130); }); await page.waitForTimeout(400);
  await page.evaluate(() => document.getElementById('uploadPrompt').classList.add('hidden'));
  await hl(null); await shot('result');
  await page.waitForTimeout(2500);
  await page.evaluate(() => { const e = document.getElementById('speciesPhotos'); (e && e.innerHTML ? e : document.getElementById('scanResultContent')).scrollIntoView({ block: 'start' }); scrollBy(0, -60); }); await page.waitForTimeout(800);
  await hl('#speciesPhotos'); await shot('gallery'); await hl(null);
  await page.evaluate(() => { const e = [...document.querySelectorAll('#scanResultContent summary')].find(x => /Adatlap/.test(x.textContent)); e.scrollIntoView({ block: 'start' }); scrollBy(0, -20); }); await page.waitForTimeout(400); await shot('factsheet');
  await page.evaluate(() => { const e = [...document.querySelectorAll('#scanResultContent summary')].find(x => /Szaporodás/.test(x.textContent)); e.scrollIntoView({ block: 'start' }); scrollBy(0, -20); }); await page.waitForTimeout(400); await shot('breeding');
  await page.evaluate(() => { const e = [...document.querySelectorAll('#scanResultContent div')].find(d => /Ár és beszerzés/.test(d.textContent) && d.className.includes('rounded-xl')); e.scrollIntoView({ block: 'center' }); }); await page.waitForTimeout(400);
  await page.evaluate(() => { const e = [...document.querySelectorAll('#scanResultContent div')].find(d => /Ár és beszerzés/.test(d.textContent) && d.className.includes('rounded-xl')); e.classList.add('demo-target'); });
  await hl('.demo-target'); await shot('price');
  await hl(null);
  await page.evaluate(() => { document.getElementById('fixBox').scrollIntoView({ block: 'center' }); }); await page.evaluate(() => openFix()); await page.fill('#fixInput', 'Paracheirodon innesi (Neonhal)'); await page.waitForTimeout(300);
  await hl('#fixBox'); await page.evaluate(() => document.getElementById('fixBox').scrollIntoView({ block: 'center' })); await page.waitForTimeout(300); await shot('fix'); await hl(null);
  // Növény
  await page.evaluate(() => { setScanMode('noveny'); document.getElementById('uploadPrompt').classList.remove('hidden'); });
  { const ins = await page.$$('input[type=file][accept^="image"]'); await ins[ins.length - 1].setInputFiles(plantPng); }
  await page.waitForTimeout(2500); await prep();
  await page.evaluate(() => { document.getElementById('uploadPrompt').classList.add('hidden'); document.getElementById('scanResultContent').scrollIntoView({ block: 'start' }); scrollBy(0, -150); }); await page.waitForTimeout(400);
  await shot('plant');
  // Beteg hal: tünetgombok
  await page.evaluate(() => { setScanMode('beteg'); document.getElementById('scanPreview').classList.add('hidden'); document.getElementById('uploadPrompt').classList.remove('hidden'); document.getElementById('scanResultContent').innerHTML = ''; document.getElementById('scanAccuracy').classList.add('hidden'); document.getElementById('scanActions').classList.add('hidden'); });
  for (const t of ['fehér pöttyök', 'dörzsölődik a tárgyakon', 'nem eszik', 'új halat tettem be nemrég']) await page.evaluate(t => addSymptom(t), t);
  await page.evaluate(() => { const n = document.getElementById('scanNote'); n.value += ', guppi, 2 napja kezdődött'; document.getElementById('scanModes').scrollIntoView({ block: 'start' }); scrollBy(0, -20); });
  await page.waitForTimeout(400); await hl('#scanNote'); await shot('symptoms'); await hl(null);
  await page.evaluate(() => analyzeTextOnly()); await page.waitForTimeout(2500); await prep();
  await page.evaluate(() => { const e = [...document.querySelectorAll('#scanResultContent div')].find(d => /Mit tegyél most/.test(d.textContent) && d.className.includes('rounded-xl')); e.scrollIntoView({ block: 'start' }); scrollBy(0, -260); }); await page.waitForTimeout(400);
  await shot('disease');
  await page.evaluate(() => { setScanMode('hal'); document.getElementById('scanNote').value = ''; document.getElementById('scanResultContent').innerHTML = ''; });
  await tab('tab1'); await page.evaluate(() => scrollTo(0, 0)); await shot('tab1');
  await tab('tab2'); await to('#stockCheckBox'); await hl('#stockCheckBox'); await shot('tab2'); await hl(null);
  await page.evaluate(() => { const b = [...document.querySelectorAll('#stockCheckBox button')].find(x => /Nálam működik/.test(x.textContent)); b.classList.add('demo-ok'); b.scrollIntoView({ block: 'center' }); }); await hl('.demo-ok'); await page.waitForTimeout(300); await shot('okpair'); await hl(null);
  await tab('tab3'); await to('#waterChart'); await page.waitForTimeout(800); await shot('tab3');
  await tab('tab4'); await page.evaluate(() => scrollTo(0, 0)); await shot('tab4');
  await tab('tab5'); await page.evaluate(() => scrollTo(0, 0));
  await page.evaluate(() => { document.getElementById('cloudBtn').classList.remove('hidden'); document.getElementById('cloudBtnText').innerText = 'Belépés'; showToast('Adatok elmentve a felhőbe.'); });
  await hl('#cloudBtn'); await page.waitForTimeout(300); await shot('cloud'); await hl(null);
  await page.evaluate(() => openBugReport({ kind: 'Rossz felismerés', text: 'Ez nem sárga sügér, hanem Lombard sügér.' })); await page.waitForTimeout(500); await shot('bug');
  await page.evaluate(() => closeModal('bugModal'));
  await ctx.close();

  // Kompozíció: 1080x1920 keret, felirat + telefon
  const comp = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
  const img = f => 'data:image/png;base64,' + fs.readFileSync(f).toString('base64');
  const BASE = `<style>*{box-sizing:border-box}body{margin:0;width:1080px;height:1920px;overflow:hidden;font-family:'Inter','Segoe UI',system-ui,sans-serif;color:#e2e8f0;
    background:radial-gradient(1200px 900px at 20% 0%,#0f766e 0%,transparent 60%),radial-gradient(1000px 800px at 100% 100%,#155e75 0%,transparent 55%),#0b1220}
    .cap{position:absolute;top:110px;left:0;right:0;text-align:center;font-size:66px;font-weight:800;letter-spacing:-.5px;padding:0 60px;text-shadow:0 4px 24px rgba(0,0,0,.5)}
    .brand{position:absolute;bottom:60px;left:0;right:0;text-align:center;font-size:34px;color:#5eead4;font-weight:700;opacity:.9}
    .phone{position:absolute;left:50%;top:290px;transform:translateX(-50%);width:760px;height:1500px;border-radius:84px;background:#020617;padding:22px;box-shadow:0 40px 120px rgba(0,0,0,.6),0 0 0 3px #334155}
    .screen{width:100%;height:100%;border-radius:64px;overflow:hidden;background:#0f172a;position:relative}
    .screen img{width:100%;display:block}
    .notch{position:absolute;top:18px;left:50%;transform:translateX(-50%);width:160px;height:40px;border-radius:20px;background:#020617;z-index:3}</style>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap" rel="stylesheet">`;
  const phone = inner => `<div class="phone"><div class="screen"><div class="notch"></div>${inner}</div></div>`;
  const chromeBarUnused = `<div style="position:absolute;bottom:0;left:0;right:0;height:150px;background:#1e293b;display:flex;align-items:center;gap:22px;padding:0 30px;font-size:34px">
     <span style="font-size:44px">⌂</span><div style="flex:1;background:#334155;border-radius:40px;padding:22px 30px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:30px">🔒 ${URL_TXT}</div><span style="font-size:46px;font-weight:900">⋮</span></div>`;
  const appBg = `<img src="${img(path.join(OUT, 'shots/scan.png'))}" style="filter:brightness(.55)">`;
  for (const s of scenes) {
    let body;
    if (s.kind === 'title') {
      body = `<div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:50px;text-align:center;padding:80px">
        <img src="${ICON}" style="width:330px;height:330px;border-radius:80px;box-shadow:0 30px 90px rgba(20,184,166,.45)">
        <div style="font-size:130px;font-weight:800;letter-spacing:-2px">${s.title}</div>
        <div style="font-size:52px;color:#99f6e4;font-weight:600;max-width:900px;line-height:1.25">${s.sub}</div></div>`;
    } else if (s.kind === 'app') {
      body = `<div class="cap">${s.caption}</div>` + phone(`<img src="${img(path.join(OUT, 'shots', s.shot + '.png'))}">`) + `<div class="brand">AquaFaj · ingyenes</div>`;
    } else if (s.kind === 'install1') {
      body = `<div class="cap">${s.caption}</div>` + phone(`<div style="position:absolute;inset:0;background:#0b141a"></div>
          <div style="position:absolute;top:70px;left:0;right:0;height:120px;background:#1f2c34;display:flex;align-items:center;padding:0 40px;font-size:38px;font-weight:700;color:#e9edef">← 💬 Üzenet</div>
          <div style="position:absolute;top:300px;left:40px;width:560px;background:#1f2c34;border-radius:0 34px 34px 34px;padding:34px;font-size:36px;color:#e9edef;line-height:1.35">Szia! Próbáld ki ezt az akvarisztikai appot 🐠</div>
          <div style="position:absolute;top:520px;left:40px;width:600px;background:#1f2c34;border-radius:0 34px 34px 34px;overflow:hidden;box-shadow:0 0 0 6px #fbbf24">
            <div style="height:220px;background:linear-gradient(135deg,#0f766e,#155e75);display:flex;align-items:center;justify-content:center"><img src="${ICON}" style="width:150px;height:150px;border-radius:40px"></div>
            <div style="padding:28px 34px;font-size:36px;font-weight:800;color:#e9edef">AquaFaj – akvarisztikai zsebkönyv</div>
            <div style="padding:0 34px 30px;font-size:32px;color:#53bdeb;text-decoration:underline">🔗 Koppints ide</div></div>
          <div style="position:absolute;top:900px;left:330px;font-size:150px;transform:rotate(-15deg)">👆</div>
          <div style="position:absolute;bottom:90px;left:40px;right:40px;text-align:center;font-size:34px;color:#99f6e4">Nem Chrome-ban nyílt meg?<br><b>⋮ → Megnyitás Chrome-ban</b></div>`) + `<div class="brand">Koppints a kapott linkre</div>`;
    } else if (s.kind === 'install2') {
      const items = ['Új lap', 'Könyvjelzők', 'Előzmények', 'Letöltések', '<b>📲 Alkalmazás telepítése</b>', 'Hozzáadás a kezdőképernyőhöz', 'Beállítások'];
      body = `<div class="cap">${s.caption}</div>` + phone(`${appBg}
          <div style="position:absolute;top:70px;right:24px;width:560px;background:#1e293b;border-radius:30px;box-shadow:0 20px 60px rgba(0,0,0,.6);padding:20px 0;font-size:36px">
          ${items.map((t, i) => `<div style="padding:26px 40px;${i === 4 ? 'background:rgba(251,191,36,.18);outline:4px solid #fbbf24;outline-offset:-4px;color:#fde68a' : ''}">${t}</div>`).join('')}</div>
          <div style="position:absolute;top:22px;right:40px;font-size:60px;font-weight:900;color:#fbbf24">⋮</div>`) + `<div class="brand">Chrome menü → Alkalmazás telepítése</div>`;
    } else if (s.kind === 'install3') {
      const apps = ['📞', '💬', '📷', '🗺️', '🎵', '📧', '☀️', '📅'];
      body = `<div class="cap">${s.caption}</div>` + phone(`<div style="position:absolute;inset:0;background:linear-gradient(160deg,#134e4a,#1e3a8a 70%,#0f172a)"></div>
          <div style="position:absolute;top:160px;left:0;right:0;text-align:center;font-size:110px;font-weight:300;color:#fff">21:37</div>
          <div style="position:absolute;top:520px;left:50px;right:50px;display:grid;grid-template-columns:repeat(4,1fr);gap:60px 30px;text-align:center">
          ${apps.map(a => `<div><div style="width:130px;height:130px;margin:auto;border-radius:36px;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;font-size:66px">${a}</div></div>`).join('')}
          <div><img src="${ICON}" style="width:130px;height:130px;border-radius:36px;outline:5px solid #fbbf24;outline-offset:6px"><div style="font-size:28px;margin-top:14px;color:#fff;font-weight:700">AquaFaj</div></div></div>`) + `<div class="brand">iPhone: Safari → Megosztás → Főképernyőhöz adás</div>`;
    }
    await comp.setContent(`<html><head><meta charset="utf-8">${BASE}</head><body>${body}</body></html>`);
    await comp.waitForTimeout(process.env.LOCAL_CSS ? 200 : 1200);
    await comp.screenshot({ path: path.join(OUT, 'frames', s.id + '.png') });
  }
  await browser.close();
  console.log('frames ok'); srv.kill(); process.exit(0);
})();
