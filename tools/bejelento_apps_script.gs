// AquaFaj hibabejelentő – Google Apps Script (a saját Gmail-fiókodból küldi a levelet a freemailre)
const CIM = 'aquafajpro@freemail.hu';

function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents);
    const szoveg = String(d.text || '').slice(0, 3000);
    if (!szoveg) return valasz({ ok: false, hiba: 'üres' });
    const opciok = {
      name: 'AquaFaj bejelentő',
      htmlBody: '<h2>' + esc(d.kind || 'Bejelentés') + '</h2>' +
        '<p style="font-size:16px">' + esc(szoveg).replace(/\n/g, '<br>') + '</p>' +
        '<p><b>Felhasználó:</b> ' + esc(d.email || 'nincs belépve') + '</p>' +
        '<pre style="background:#f3f4f6;padding:10px;border-radius:8px">' + esc(d.meta || '') + '</pre>'
    };
    if (d.photo) opciok.attachments = [Utilities.newBlob(Utilities.base64Decode(d.photo), 'image/jpeg', 'aquafaj_foto.jpg')];
    MailApp.sendEmail(CIM, 'AquaFaj – ' + (d.kind || 'Bejelentés') + (d.photo ? ' (fotóval)' : ''), szoveg, opciok);
    return valasz({ ok: true });
  } catch (err) {
    return valasz({ ok: false, hiba: String(err) });
  }
}
function doGet() { return valasz({ ok: true, info: 'AquaFaj bejelentő működik' }); }
function valasz(o) { return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON); }
function esc(s) { return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])); }

// Ezt egyszer futtasd le a szerkesztőben (Futtatás), hogy engedélyt adj a levélküldésre.
function proba() { MailApp.sendEmail(CIM, 'AquaFaj – próbalevél', 'Ha ezt látod, a bejelentő e-mail működik.'); }
