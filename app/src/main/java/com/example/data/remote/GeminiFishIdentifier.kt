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
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val supportedModels = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest"
    )

    suspend fun identifyFish(bitmap: Bitmap): Result<FishRecognitionResult> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.effectiveKey()
        val groqKey = ApiKeyStore.groqKey()

        if ((apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") && groqKey.isBlank()) {
            return@withContext Result.failure(
                MissingApiKeyException(
                    "Nincs beállítva kulcs a felismeréshez. Add meg az ingyenes kulcsodat a Beállítások fülön (aistudio.google.com/apikey), vagy használd a minta felismerés funkciót!"
                )
            )
        }

        val base64Image = bitmapToBase64(bitmap)

        var lastError: Exception? = null
        val geminiOk = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        // 1. Google Gemini: a legpontosabb halhatározó (napi ingyenes kerettel)
        if (geminiOk) {
        val jsonPayload = buildRequestBody(base64Image)
        val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
        var busy = false

        // Végigpróbálja a modelleket; ha egy túlterhelt (503/429/500), a következőre vált.
        // Ha mind foglalt, rövid várakozás után még kétszer újrapróbálja.
        attempts@ for (attempt in 0 until 1) {
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

        }

        // 2. Tartalék: Groq (gyors, nagyobb keret, de fajszinten kevésbé pontos) – jelölve
        val groqKey2 = ApiKeyStore.groqKey()
        if (groqKey2.isNotBlank()) {
            val g = groqIdentify(groqKey2, base64Image)
            val r = g.getOrNull()
            if (r != null) {
                val note = "Tartalék felismerés (a Gemini napi kerete most nem elérhető): ez csak tájékoztató tipp, " +
                    "a pontos fajt ellenőrizd (pl. a diszhal.info gombbal), vagy próbáld újra később."
                return@withContext Result.success(
                    r.copy(
                        confidencePercent = minOf(r.confidencePercent, 60),
                        uncertaintyWarning = if (r.uncertaintyWarning.isBlank()) note else note + " " + r.uncertaintyWarning
                    )
                )
            }
            lastError = Exception(
                "A felismerés most nem sikerült.\nGemini: " + (lastError?.message ?: "nincs kulcs.") +
                    "\nTartalék (Groq): " + (g.exceptionOrNull()?.message ?: "nem válaszolt.") + "\nPróbáld újra 1-2 perc múlva."
            )
        }

        Result.failure(
            lastError ?: Exception("Nem sikerült elérni a Gemini AI modellt. Ellenőrizd az internetkapcsolatot és az API kulcsot.")
        )
    }

    private val groqFallbackModels = listOf(
        "qwen/qwen3.8-27b",
        "qwen/qwen3.6-27b",
        "meta-llama/llama-4-scout-17b-16e-instruct"
    )
    private var groqModelsCache: List<String>? = null

    /** Lekéri, milyen modellek érhetők el most a Groqnál (a nevek időnként változnak). */
    private fun groqModels(key: String): List<String> {
        groqModelsCache?.let { return it }
        val found = mutableListOf<String>()
        try {
            val req = Request.Builder().url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer " + key).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (resp.isSuccessful) {
                val data = JSONObject(body).optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val id = data.optJSONObject(i)?.optString("id").orEmpty()
                        val lower = id.lowercase()
                        val vision = (lower.contains("qwen") && !lower.contains("guard")) ||
                            lower.contains("llama-4") || lower.contains("vision")
                        if (vision) found.add(id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GeminiFishIdentifier", "Groq modellista nem érhető el", e)
        }
        // A legújabb verzió előre
        val ordered = found.sortedDescending() + groqFallbackModels.filter { it !in found }
        groqModelsCache = ordered
        return ordered
    }

    private fun groqIdentify(key: String, base64Image: String): Result<FishRecognitionResult> {
        var err: Exception? = null
        for (model in groqModels(key).take(4)) {
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
                    code == 429 -> Exception("a Groq percenkénti vagy napi kerete épp betelt.")
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
        val maxDimension = 1024
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
            Te tapasztalt akvarisztikai halhatározó szakértő vagy. Feladat: a MELLÉKELT KÉPEN látható akváriumi hal fajának meghatározása.
            Ha több hal látszik, a legnagyobb, legélesebben látható halat (vagy a képen többségben lévő fajt) határozd meg.

            Módszer:
            1. Először írd le pontosan, mit látsz a képen ("lathatoBelyegek" mező): testforma és méret a növényekhez/kavicshoz képest, színek, mintázat (pöttyök, sávok, csíkok, foltok), a hátúszó helye és mérete, a farok alatti úszó hossza, a hasúszó (kicsi vagy hosszú, fonalszerű), a farokúszó formája és színe, a száj állása.
            2. CSAK ezek alapján gondold végig a 3-4 szóba jöhető fajt, és válaszd azt, amelyikre a látott bélyegek a legjobban illenek. Ne találgass olyan fajt, amelynek jellegzetes bélyegei nem látszanak a képen.
            3. Gondolj a gyakori díszhalakra (elevenszülők: guppi, endler, platti, molli, kardfarkú; pontylazacok; razbórák, dániók, márnák; harcsák; labirintushalak; sügérek) és a ritkábbakra is (fogaspontyok/killik, szivárványhalak, gébek, tavi sügérek).
            Hasznos megkülönböztetések: a fogaspontyoknál (killik) a hát- és a farok alatti úszó hátul, egymással szemben ül; a gurámiknál és harcoshalaknál a hasúszó hosszú, fonalszerű; az elevenszülő hímeknél a farok alatti úszó pálcaszerű párzószervvé (gonopódium) alakult; a pontylazacoknak gyakran van kis zsírúszójuk.

            Elnevezés: a "magyarNev" a magyar akvarisztikában használt név legyen. Fogaspontyoknál (killiknél) a magyar fogasponty nevet írd, mellette zárójelben a killi megnevezést.
            A "biztonsagSzazalek" legyen őszinte: 85 fölé csak akkor menj, ha a döntő bélyegek jól látszanak; homályos, apró vagy távoli halnál legyen alacsonyabb.
            Az "alternativFajok" listában add meg a 2-3 legvalószínűbb MÁSIK fajt, mindegyiknél röviden, miben különbözik a képen látott haltól.

            Válaszolj KIZÁRÓLAG érvényes JSON formátumban, magyar nyelven, PONTOSAN ezekkel a mezőkkel (a <...> helyére a saját válaszodat írd):
            {
              "lathatoBelyegek": "<amit a képen ténylegesen látsz, 1-3 mondat>",
              "magyarNev": "<magyar név>",
              "tudomanyosNev": "<latin név>",
              "csalad": "<család latinul>",
              "biztonsagSzazalek": <0-100 egész szám>,
              "bizonytalansagiJelzes": "<ha bizonytalan vagy a kép rossz, miért; különben üres string>",
              "alternativFajok": ["<másik faj – miben különbözik>", "<másik faj – miben különbözik>"],
              "meret": "<felnőttkori méret cm-ben>",
              "elohely": "<eredeti élőhely>",
              "homerseklet": "<ideális vízhőmérséklet °C>",
              "ph": "<ideális pH-tartomány>",
              "vizkemenyseg": "<ideális keménység dGH>",
              "taplalkozas": "<táplálkozás>",
              "tarsithatosag": "<természet (békés/félagresszív/agresszív) és társíthatóság>",
              "szaporitas": "<szaporodási mód és a tenyésztés lényege>",
              "leiras": "<a hal megjelenése, színei, viselkedése>",
              "gondozasiTippek": "<tartási tanácsok>"
            }
            Ha a képen egyértelműen NEM hal látható, a magyarNev legyen 'Nem felismerhető akváriumi hal', és a bizonytalansagiJelzes írja le a problémát.
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
