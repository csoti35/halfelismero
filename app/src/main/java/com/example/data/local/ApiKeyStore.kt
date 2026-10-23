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
    private const val KEY = "gemini_api_key"
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

    /** A mentett kulcs, vagy ha nincs, a fordításkor megadott (ha valódi). */
    fun effectiveKey(): String {
        val k = saved()
        if (k.isNotBlank()) return k
        val built = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        return if (built.isBlank() || built == "MY_GEMINI_API_KEY") "" else built
    }
}
