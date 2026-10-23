package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.local.ApiKeyStore
import com.example.data.model.FishRecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiFishIdentifier(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val supportedModels = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest"
    )

    suspend fun identifyFish(bitmap: Bitmap): Result<FishRecognitionResult> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.effectiveKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                MissingApiKeyException(
                    "A Gemini API-kulcs nincs beállítva. Add meg az ingyenes kulcsodat a Beállítások fülön (kulcs: aistudio.google.com/apikey), vagy használd a minta felismerés funkciót!"
                )
            )
        }

        val base64Image = bitmapToBase64(bitmap)
        val jsonPayload = buildRequestBody(base64Image)
        val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        var lastError: Exception? = null
        var busy = false

        // Végigpróbálja a modelleket; ha egy túlterhelt (503/429/500), a következőre vált.
        // Ha mind foglalt, rövid várakozás után még kétszer újrapróbálja.
        attempts@ for (attempt in 0 until 2) {
            if (attempt > 0) kotlinx.coroutines.delay(3000L)
            busy = false
            for (modelName in supportedModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
                    val request = Request.Builder()
                        .url(url)
                        .header("x-goog-api-key", apiKey)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseString = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val parsedResult = parseGeminiResponse(responseString)
                        return@withContext Result.success(parsedResult)
                    }
                    Log.w("GeminiFishIdentifier", "Model $modelName returned ${response.code}: $responseString")
                    val code = response.code
                    val modelMissing = code == 404 ||
                        (code == 400 && (responseString.contains("not found", true) || responseString.contains("not supported", true)))
                    val quotaGone = code == 429 && responseString.contains("quota", true)
                    if (quotaGone) {
                        lastError = Exception("Elfogyott a mai ingyenes Gemini-keret ennél a kulcsnál. Holnap újra működik, vagy adj meg saját ingyenes kulcsot a Beállításokban (aistudio.google.com/apikey).")
                        break@attempts
                    }
                    val overloaded = code == 429 || code == 500 || code == 503 || code == 504
                    if (overloaded) busy = true
                    lastError = when {
                        code == 401 || code == 403 || (code == 400 && responseString.contains("API key", true)) ->
                            Exception("A Gemini kulcs nem érvényes vagy le van tiltva. Adj meg új kulcsot a Beállításokban.")
                        overloaded ->
                            Exception("A Google szerverei most túlterheltek. Próbáld újra 1-2 perc múlva.")
                        else -> Exception("Gemini hiba ($code). Próbáld újra.")
                    }
                    if (!modelMissing && !overloaded) break@attempts
                } catch (e: Exception) {
                    Log.e("GeminiFishIdentifier", "Network error calling $modelName", e)
                    lastError = Exception("Nincs internetkapcsolat, vagy a Google nem válaszolt időben. Ellenőrizd a netet, és próbáld újra.")
                    busy = true
                }
            }
            if (!busy) break
        }

        // Ingyenes tartalék: Groq (nagyobb napi keret), ha a Gemini nem sikerült
        val groqKey = ApiKeyStore.groqKey()
        if (groqKey.isNotBlank()) {
            val g = groqIdentify(groqKey, base64Image)
            if (g.isSuccess) return@withContext g
            lastError = Exception(
                (lastError?.message ?: "A Gemini nem válaszolt.") +
                    "\nA tartalék szolgáltatás (Groq) sem sikerült: " + (g.exceptionOrNull()?.message ?: "ismeretlen hiba")
            )
        }

        Result.failure(
            lastError ?: Exception("Nem sikerült elérni a Gemini AI modellt. Ellenőrizd az internetkapcsolatot és az API kulcsot.")
        )
    }

    private val groqModels = listOf(
        "qwen/qwen3.6-27b",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "meta-llama/llama-4-maverick-17b-128e-instruct"
    )

    private fun groqIdentify(key: String, base64Image: String): Result<FishRecognitionResult> {
        var err: Exception? = null
        for (model in groqModels) {
            try {
                val content = JSONArray()
                    .put(JSONObject().put("type", "text").put("text", fishPrompt + "\n\nCsak a JSON objektumot írd ki, semmi mást."))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64," + base64Image)))
                val body = JSONObject()
                    .put("model", model)
                    .put("temperature", 0.2)
                    .put("max_completion_tokens", 3000)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + key)
                    .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                val responseString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val msg = JSONObject(responseString).optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("message")?.optString("content").orEmpty()
                    return Result.success(parseResultText(msg))
                }
                Log.w("GeminiFishIdentifier", "Groq $model returned ${response.code}: $responseString")
                val code = response.code
                err = when {
                    code == 401 || code == 403 -> Exception("a Groq kulcs nem érvényes.")
                    code == 429 -> Exception("a Groq napi ingyenes kerete is elfogyott, holnap újra működik.")
                    else -> Exception("Groq hiba ($code).")
                }
                val tryNext = code == 404 || code == 429 || code == 500 || code == 503 ||
                    (code == 400 && (responseString.contains("model", true) || responseString.contains("image", true)))
                if (!tryNext) break
            } catch (e: Exception) {
                Log.e("GeminiFishIdentifier", "Groq hiba: $model", e)
                err = Exception("a Groq nem válaszolt (internet?).")
            }
        }
        return Result.failure(err ?: Exception("a Groq nem érhető el."))
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1280
        val ratio = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height,
            1.0f
        )
        val targetWidth = (bitmap.width * ratio).toInt()
        val targetHeight = (bitmap.height * ratio).toInt()
        val scaled = if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private val fishPrompt = """
            Te egy világszínvonalú akvarisztikai szakértő és halhatározó vagy. Feladat: a képen látható akváriumi hal fajának pontos meghatározása.

            Így dolgozz (ezt magadban végezd el, a válaszba csak a JSON kerül):
            1. Nézd meg a látható bélyegeket: testforma (megnyúlt/hengeres, oldalról lapított, magas hátú), a száj állása, a HÁTÚSZÓ HELYE és mérete, a FAROK ALATTI ÚSZÓ hossza és helye, a HASÚSZÓ (kicsi vagy hosszú fonalszerű tapogató), a farokúszó formája, a mintázat (pöttyök, sávok, csíkok, foltok) és a színek.
            2. Gondold végig a 3-4 legvalószínűbb fajt, és döntsd el, melyik bélyeg választja el őket.
            3. Ne a legismertebb hasonló fajt válaszd automatikusan: a hobbisták ritkább fajokat is tartanak (killik/fogaspontyok: Austrolebias – régi nevén Cynolebias, Nothobranchius, Aphyosemion, Fundulopanchax, Simpsonichthys; szivárványhalak; Viktória-tavi és malawi sügérek; gébek; törpesügérek).

            FONTOS összetévesztések:
            - GYÖNGYÖS KILLI (Austrolebias nigripinnis, régi nevén Cynolebias nigripinnis) vs. GYÖNGYGURÁMI (Trichopodus leeri): mindkettő gyöngypöttyös! A killi teste megnyúlt, hengeres, kicsi (max. 4-5 cm); a hátúszó és a farok alatti úszó HÁTUL, egymással SZEMBEN ül, nagyjából egyforma, lekerekített; a hasúszó KICSI; a hím sötét kékesszürke vagy fekete, világoskék/zöldes pöttyökkel, a farok alatti úszó alja gyakran narancs/vörös szegélyű; a nőstény szürkésbarna. A gyöngygurámi teste magas, oldalról erősen lapított, nagyobb (10-12 cm); a hasúszói HOSSZÚ FONALSZERŰ TAPOGATÓK; a farok alatti úszó a has közepétől a farokig húzódik; a test közepén sötét hosszanti vonal fut a szemtől a farokig.
            - Killi-nőstény vs. harcoshal-nőstény: a harcoshalnak kicsi a hátúszója, hosszú a farok alatti úszója, és fonalas a hasúszója.
            - Aranyos zebrasügér (Astatotilapia latifasciata): széles függőleges fekete sávok sárga-narancs alapon (fiatalon foltokra szakadhatnak); a szabálytalan fekete pöttyös-foltos narancs hal inkább "OB" színváltozatú malawi sügér.

            A "biztonsagSzazalek" legyen őszinte: 85 fölé csak akkor menj, ha a döntő bélyegek jól látszanak. Az "alternativFajok" listában add meg a 2-3 legvalószínűbb MÁSIK fajt, mindegyiknél röviden, miben különbözik (pl. "Gyöngygurámi – magas test, fonalas hasúszó").
            Válaszolj KIZÁRÓLAG érvényes JSON formátumban az alábbi mezőkkel, magyar nyelven:
            {
              "magyarNev": "A hal közismert magyar neve",
              "tudomanyosNev": "A faj tudományos latin neve",
              "csalad": "Rendszertani család (pl. Characidae)",
              "biztonsagSzazalek": 90,
              "bizonytalansagiJelzes": "Ha a kép homályos, gyenge minőségű vagy a faj könnyen összetéveszthető, részletezd a bizonytalanságot. Ha biztos a felismerés, legyen üres string.",
              "alternativFajok": ["Hasonló faj 1", "Hasonló faj 2"],
              "meret": "Felnőtt méret (pl. 3-4 cm)",
              "elohely": "Eredeti élőhely (pl. Dél-Amerika, Amazonas-medence)",
              "homerseklet": "Ideális vízhőmérséklet (pl. 22-26 °C)",
              "ph": "Ideális pH tartomány (pl. 6.0 - 7.0)",
              "vizkemenyseg": "Ideális vízkeménység (pl. 2 - 10 dGH)",
              "taplalkozas": "Táplálkozási igények (pl. Mindenevő, finom lemezes és fagyasztott plankton)",
              "tarsithatosag": "Társíthatóság és viselkedés (pl. Békés rajhal, min. 8-10 egyed ajánlott)",
              "szaporitas": "Szaporodási mód (pl. Szabadon ikrázó, lágy víz szükséges)",
              "leiras": "Részletes magyar nyelvű ismertető a hal megjelenéséről, színeiről és viselkedéséről",
              "gondozasiTippek": "Hasznos akvarisztikai jótanácsok a tartáshoz és növényzethez"
            }
            Ha a képen egyértelműen NEM hal látható, a magyarNev mező értéke legyen 'Nem felismerhető akváriumi hal' és a bizonytalansagiJelzes részletezze a problémát.
        """.trimIndent()

    private fun buildRequestBody(base64Data: String): String {
        val prompt = fishPrompt

        val root = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()

        val imagePart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Data)
        imagePart.put("inlineData", inlineData)
        parts.put(imagePart)

        val textPart = JSONObject()
        textPart.put("text", prompt)
        parts.put(textPart)

        content.put("parts", parts)
        contents.put(content)
        root.put("contents", contents)

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.2)
        // Kevesebb "gondolkodás" = gyorsabb válasz (a felismeréshez elég)
        genConfig.put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
        genConfig.put("responseMimeType", "application/json")
        root.put("generationConfig", genConfig)

        return root.toString()
    }

    private fun parseGeminiResponse(rawResponse: String): FishRecognitionResult {
        val rootObj = JSONObject(rawResponse)
        val candidates = rootObj.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
        return parseResultText(text)
    }

    private fun parseResultText(rawText: String): FishRecognitionResult {
        // Néhány modell "gondolkodó" blokkot is ír a válasz elé; ezt kiszedjük.
        val text = rawText.replace(Regex("(?s)<think>.*?</think>"), "")

        // Robust JSON block extraction
        val jsonStartIndex = text.indexOf('{')
        val jsonEndIndex = text.lastIndexOf('}')
        val cleanedText = if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
            text.substring(jsonStartIndex, jsonEndIndex + 1)
        } else {
            text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        val json = JSONObject(cleanedText)

        val altFajok = mutableListOf<String>()
        val altArray = json.optJSONArray("alternativFajok")
        if (altArray != null) {
            for (i in 0 until altArray.length()) {
                val item = altArray.optString(i)
                if (item.isNotBlank()) altFajok.add(item)
            }
        }

        return FishRecognitionResult(
            hungarianName = json.optString("magyarNev", "Ismeretlen halfaj"),
            scientificName = json.optString("tudomanyosNev", ""),
            family = json.optString("csalad", ""),
            confidencePercent = json.optInt("biztonsagSzazalek", 85),
            uncertaintyWarning = json.optString("bizonytalansagiJelzes", ""),
            alternativeSpecies = altFajok,
            size = json.optString("meret", ""),
            habitat = json.optString("elohely", ""),
            temperature = json.optString("homerseklet", ""),
            ph = json.optString("ph", ""),
            hardness = json.optString("vizkemenyseg", ""),
            diet = json.optString("taplalkozas", ""),
            compatibility = json.optString("tarsithatosag", ""),
            breeding = json.optString("szaporitas", ""),
            description = json.optString("leiras", ""),
            careTips = json.optString("gondozasiTippek", "")
        )
    }
}

class MissingApiKeyException(message: String) : Exception(message)
