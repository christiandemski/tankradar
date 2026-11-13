package de.codekorb.tankradar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// Spritpreise von Tankerkönig (Daten der Markttransparenzstelle, CC BY 4.0). Braucht einen kostenlosen API-Schlüssel:
// https://creativecommons.tankerkoenig.de  Der Dienst bittet darum, automatisch höchstens alle 5 Minuten abzufragen.

data class Station(val id: String, val name: String, val street: String, val place: String, val lat: Double, val lng: Double)

// Preise je Sorte (e5, e10, diesel); fehlt eine Sorte, gibt es sie dort nicht oder gerade keinen Preis
data class Quote(val station: Station, val open: Boolean, val prices: Map<String, Double>, val dist: Double?) {
    fun price(type: String) = if (open) prices[type] else null
}

class TankerException(message: String) : Exception(message)
class NoLocation(message: String) : Exception(message)

object Tanker {
    private const val BASE = "https://creativecommons.tankerkoenig.de/json"
    private const val MAX_AGE = 5 * 60_000L // so lange gelten Preise
    private const val MIN_AGE = 60_000L     // „Aktualisieren“ fragt frühestens nach einer Minute neu
    private val cache = HashMap<String, Pair<Long, Any>>()

    private fun get(endpoint: String, params: Map<String, Any>): JSONObject {
        val query = params.entries.joinToString("&") { it.key + "=" + URLEncoder.encode(it.value.toString(), "UTF-8") }
        try {
            val c = URL("$BASE/$endpoint?$query").openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 15_000
            try {
                val body = (if (c.responseCode < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val d = try { JSONObject(body) } catch (e: JSONException) { throw TankerException("Tankerkönig antwortet nicht (HTTP ${c.responseCode})") }
                if (!d.optBoolean("ok")) throw TankerException("Tankerkönig: " + d.optString("message", "Abruf fehlgeschlagen"))
                return d
            } finally { c.disconnect() }
        } catch (e: IOException) {
            throw TankerException("Keine Verbindung zu Tankerkönig")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: String, force: Boolean, load: () -> T): T = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[key] }?.let { (t, v) -> if (now - t < if (force) MIN_AGE else MAX_AGE) return@withContext v as T }
        load().also { synchronized(cache) { cache[key] = now to it } }
    }

    // Die Quelle schreibt vieles in Großbuchstaben ("OSTFELDSTR.", "ESSEN")
    private fun tidy(s: String): String {
        val t = s.trim()
        if (t.length <= 4 || t != t.uppercase()) return t
        return Regex("(^|[\\s\\-./(])(\\p{L})").replace(t.lowercase()) { it.groupValues[1] + it.groupValues[2].uppercase() }
    }

    private fun station(o: JSONObject) = Station(
        id = o.getString("id"),
        name = o.optString("brand").trim().ifEmpty { tidy(o.optString("name")) },
        street = (tidy(o.optString("street")) + " " + o.optString("houseNumber").trim()).trim(),
        place = tidy(o.optString("place")),
        lat = o.optDouble("lat"), lng = o.optDouble("lng"))

    private fun prices(o: JSONObject) =
        FUELS.keys.mapNotNull { k -> (o.opt(k) as? Number)?.toDouble()?.takeIf { it > 0 }?.let { k to it } }.toMap()

    // Alle Tankstellen im Umkreis, nach Entfernung, mit den Preisen aller Sorten
    suspend fun around(key: String, lat: Double, lng: Double, radius: Double, force: Boolean = false): List<Quote> =
        cached(String.format(Locale.US, "list:%.3f,%.3f,%.1f", lat, lng, radius), force) {
            val d = get("list.php", mapOf("lat" to lat, "lng" to lng, "rad" to radius.coerceIn(1.0, 25.0), "type" to "all", "sort" to "dist", "apikey" to key))
            val a = d.optJSONArray("stations") ?: JSONArray()
            List(a.length()) { a.getJSONObject(it) }.map { o ->
                Quote(station(o), o.optBoolean("isOpen"), prices(o), o.optDouble("dist").takeIf { !it.isNaN() })
            }
        }

    // Preise für bestimmte Tankstellen (höchstens 10 je Abfrage): id -> geöffnet und Preise
    suspend fun prices(key: String, ids: List<String>, force: Boolean = false): Map<String, Pair<Boolean, Map<String, Double>>> =
        cached("prices:" + ids.sorted().joinToString(","), force) {
            val p = get("prices.php", mapOf("ids" to ids.joinToString(","), "apikey" to key)).optJSONObject("prices") ?: JSONObject()
            ids.associateWith { id -> p.optJSONObject(id)?.let { (it.optString("status") == "open") to prices(it) } ?: (false to emptyMap()) }
        }
}

object Prices {
    // Favoriten (alle, günstigste zuerst) oder die günstigsten geöffneten Tankstellen in der Nähe
    suspend fun load(ctx: Context, store: Store, nearby: Boolean, force: Boolean = false): List<Quote> {
        val key = store.apiKey
        if (key.isBlank()) throw TankerException("Noch kein Tankerkönig-Schlüssel eingetragen")
        val type = store.type
        val here = Locator.current(ctx, fresh = nearby)
        val list = if (nearby) {
            if (here == null) throw NoLocation(
                if (Locator.allowed(ctx)) "Der Standort ist gerade nicht verfügbar." else "Für Tankstellen in der Nähe braucht die App den Standort.")
            Tanker.around(key, here.latitude, here.longitude, store.radius.toDouble(), force).filter { it.price(type) != null }
        } else {
            val favs = store.favorites
            if (favs.isEmpty()) return emptyList()
            val p = Tanker.prices(key, favs.map { it.id }, force)
            favs.map { s -> Quote(s, p[s.id]?.first ?: false, p[s.id]?.second.orEmpty(), here?.let { Locator.km(it, s) }) }
        }
        val sorted = list.sortedWith(compareBy(nullsLast()) { it.price(type) })
        return if (nearby) sorted.take(store.limit) else sorted
    }
}
