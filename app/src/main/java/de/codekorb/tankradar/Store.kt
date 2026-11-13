package de.codekorb.tankradar

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

// Einstellungen und Favoriten, gemeinsam für die App auf dem Handy und Android Auto
class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("tankradar", Context.MODE_PRIVATE)

    var apiKey: String
        get() = p.getString("api_key", "").orEmpty()
        set(v) = p.edit { putString("api_key", v.trim()) }
    var type: String
        get() = p.getString("type", "e10") ?: "e10"
        set(v) = p.edit { putString("type", v) }
    var radius: Float // km, für „in der Nähe“ und die Suche
        get() = p.getFloat("radius", 5f)
        set(v) = p.edit { putFloat("radius", v) }
    var limit: Int // so viele Tankstellen zeigt „in der Nähe“
        get() = p.getInt("limit", 6)
        set(v) = p.edit { putInt("limit", v) }
    var nearby: Boolean // Umschalter: false = Favoriten
        get() = p.getBoolean("nearby", false)
        set(v) = p.edit { putBoolean("nearby", v) }

    var favorites: List<Station>
        get() {
            val a = JSONArray(p.getString("favorites", "[]"))
            return List(a.length()) { a.getJSONObject(it) }.map {
                Station(it.getString("id"), it.getString("name"), it.optString("street"), it.optString("place"), it.getDouble("lat"), it.getDouble("lng"))
            }
        }
        set(v) = p.edit {
            putString("favorites", JSONArray(v.map {
                JSONObject().put("id", it.id).put("name", it.name).put("street", it.street).put("place", it.place).put("lat", it.lat).put("lng", it.lng)
            }).toString())
        }

    fun isFavorite(id: String) = favorites.any { it.id == id }

    // false, wenn schon MAX_FAVORITES gemerkt sind (Tankerkönig liefert höchstens 10 Preise je Abfrage)
    fun addFavorite(s: Station): Boolean {
        val f = favorites
        if (f.any { it.id == s.id }) return true
        if (f.size >= MAX_FAVORITES) return false
        favorites = f + s
        return true
    }

    fun removeFavorite(id: String) { favorites = favorites.filter { it.id != id } }

    companion object { const val MAX_FAVORITES = 10 }
}
