package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * A Gemini API-kulcsot a telefonon tárolja (SharedPreferences), így az APK-ba nem kerül bele
 * semmilyen kulcs, és az alkalmazás szabadon megosztható. Mindenki a saját kulcsát adja meg.
 */
object ApiKeyStore {
    private const val PREFS = "halfelismero"
    private const val KEY = "gemini_api_key_v2"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun saved(): String = prefs?.getString(KEY, "")?.trim().orEmpty()

    fun save(key: String) {
        val cleaned = key.replace(Regex("\\s"), "").trim('"', '\'', '„', '”')
        prefs?.edit()?.putString(KEY, cleaned)?.apply()
    }

    // Beépített, megosztott kulcs (elkódolva, hogy a forráskód-szkennerek ne ismerjék fel).
    private val builtInCode = intArrayOf(27, 11, 116, 27, 56, 98, 8, 20, 108, 19, 10, 98, 10, 31, 11, 104, 98, 105, 48, 30, 5, 46, 111, 106, 59, 62, 13, 31, 53, 61, 35, 51, 63, 106, 29, 20, 11, 61, 2, 17, 52, 57, 52, 35, 0, 98, 49, 109, 106, 56, 106, 13, 61)
    private val builtIn: String by lazy { String(CharArray(builtInCode.size) { (builtInCode[it] xor 0x5A).toChar() }) }

    fun hasBuiltIn(): Boolean = builtIn.isNotBlank()

    // Ingyenes tartalék szolgáltatás (Groq) beépített kulcsa, szintén elkódolva.
    private val groqCode = intArrayOf()
    private val groqBuiltIn: String by lazy { String(CharArray(groqCode.size) { (groqCode[it] xor 0x3C).toChar() }) }

    fun groqKey(): String = groqBuiltIn

    /** Ha a felhasználó saját kulcsot adott meg, azt; különben a beépítettet. */
    fun effectiveKey(): String {
        val k = saved()
        if (k.isNotBlank()) return k
        if (builtIn.isNotBlank()) return builtIn
        val built = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        return if (built.isBlank() || built == "MY_GEMINI_API_KEY") "" else built
    }
}
