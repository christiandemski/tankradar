package de.codekorb.tankradar

import android.content.Intent
import android.net.Uri
import java.util.Locale

val FUELS = linkedMapOf("e5" to "Super E5", "e10" to "Super E10", "diesel" to "Diesel")

private const val SUPERSCRIPT = "⁰¹²³⁴⁵⁶⁷⁸⁹"

// 1.749 -> "1,74" und "9" (die dritte Stelle steht an der Zapfsäule hochgestellt)
fun priceParts(p: Double): Pair<String, String> {
    val s = String.format(Locale.US, "%.3f", p)
    return s.substring(0, 4).replace('.', ',') to s.substring(4)
}

// Als einfacher Text (Android Auto): "1,74⁹ €"
fun priceText(p: Double): String {
    val (main, last) = priceParts(p)
    return main + last.map { SUPERSCRIPT[it - '0'] }.joinToString("") + " €"
}

fun kmText(km: Double): String = String.format(Locale.GERMANY, if (km < 10) "%.1f km" else "%.0f km", km)

// Tankstelle in der Karten-App öffnen
fun mapIntent(s: Station) = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${s.lat},${s.lng}(${Uri.encode(s.name)})"))
