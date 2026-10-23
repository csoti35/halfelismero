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
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val supportedModels = listOf(
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-2.0-flash"
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

        // Try candidate models in order of capability
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
                } else {
                    Log.w("GeminiFishIdentifier", "Model $modelName returned ${response.code}: $responseString")
                    lastError = Exception("Gemini API hiba ($modelName - ${response.code}): $responseString")
                    // If model not found (404), continue to next model in list
                    val modelUnavailable = response.code == 404 ||
                        (response.code == 400 && (responseString.contains("not found", true) || responseString.contains("not supported", true)))
                    if (!modelUnavailable) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiFishIdentifier", "Network error calling $modelName", e)
                lastError = e
            }
        }

        Result.failure(
            lastError ?: Exception("Nem sikerült elérni a Gemini AI modellt. Ellenőrizd az internetkapcsolatot és az API kulcsot.")
        )
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
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun buildRequestBody(base64Data: String): String {
        val prompt = """
            Te egy világszínvonalú akvarisztikai szakértő és halbiológus vagy.
            Elemezd a képen látható halat és válaszolj KIZÁRÓLAG érvényes JSON formátumban az alábbi mezőkkel magyar nyelven:
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

        val root = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()

        val textPart = JSONObject()
        textPart.put("text", prompt)
        parts.put(textPart)

        val imagePart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Data)
        imagePart.put("inlineData", inlineData)
        parts.put(imagePart)

        content.put("parts", parts)
        contents.put(content)
        root.put("contents", contents)

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.2)
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
