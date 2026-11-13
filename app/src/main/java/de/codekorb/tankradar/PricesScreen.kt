package de.codekorb.tankradar

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

// Startseite: Umschalter Favoriten / in der Nähe, darunter die Tankstellen, günstigste zuerst
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(store: Store, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var nearby by remember { mutableStateOf(store.nearby) }
    var reload by remember { mutableIntStateOf(0) } // > 0: vom Knopf „Aktualisieren“ oder nach einer Freigabe
    var result by remember { mutableStateOf<Result<List<Quote>>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var favIds by remember { mutableStateOf(store.favorites.map { it.id }.toSet()) }
    val type = store.type

    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload++ }

    LaunchedEffect(nearby, reload) {
        if (nearby && !Locator.allowed(ctx) && reload == 0) askLocation.launch(Locator.PERMISSIONS)
        loading = true
        result = runCatching { Prices.load(ctx, store, nearby, force = reload > 0) }
        loading = false
    }

    fun toggleFavorite(s: Station) {
        if (s.id in favIds) store.removeFavorite(s.id)
        else if (!store.addFavorite(s)) Toast.makeText(ctx, "Mehr als ${Store.MAX_FAVORITES} Favoriten gehen nicht.", Toast.LENGTH_SHORT).show()
        favIds = store.favorites.map { it.id }.toSet()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(FUELS[type] ?: "Tankradar") }, actions = {
            IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, "Aktualisieren") }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Einstellungen") }
        })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                listOf(false to "Favoriten", true to "In der Nähe").forEachIndexed { i, (value, label) ->
                    SegmentedButton(selected = nearby == value, shape = SegmentedButtonDefaults.itemShape(i, 2),
                        onClick = { nearby = value; store.nearby = value; result = null }) { Text(label) }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Spacer(Modifier.height(4.dp))

            val r = result
            val quotes = r?.getOrNull()
            when {
                r == null -> {}
                r.exceptionOrNull() is NoLocation && !Locator.allowed(ctx) ->
                    Message(r.exceptionOrNull()!!.message!!, "Standort erlauben") { askLocation.launch(Locator.PERMISSIONS) }
                r.isFailure -> Message(r.exceptionOrNull()?.message ?: "Fehler", "Erneut versuchen") { reload++ }
                quotes!!.isEmpty() && !nearby ->
                    Message("Noch keine Favoriten. In den Einstellungen Tankstellen suchen oder hier unter „In der Nähe“ mit dem Stern merken.",
                        "Tankstellen suchen", onSettings)
                quotes.isEmpty() -> Message("Im Umkreis von ${kmText(store.radius.toDouble())} hat gerade keine Tankstelle mit ${FUELS[type]} geöffnet.")
                else -> {
                    val best = quotes.first().price(type).takeIf { quotes.size > 1 }
                    LazyColumn {
                        items(quotes, key = { it.station.id }) { q ->
                            QuoteRow(q, type, best = best != null && q.price(type) == best, favorite = q.station.id in favIds,
                                onFavorite = { toggleFavorite(q.station) }, onClick = { ctx.startActivity(mapIntent(q.station)) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(q: Quote, type: String, best: Boolean, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    val s = q.station
    val others = FUELS.filterKeys { it != type }.mapNotNull { (k, name) -> q.price(k)?.let { name.removePrefix("Super ") + " " + priceText(it) } }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            IconButton(onClick = onFavorite) {
                Icon(Icons.Filled.Star, if (favorite) "Favorit entfernen" else "Als Favorit merken",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        },
        headlineContent = { Text(s.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(listOfNotNull(s.street.ifBlank { null }, s.place.ifBlank { null }, q.dist?.let(::kmText)).joinToString(" · "))
                if (others.isNotEmpty()) Text(others.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = { Price(q.price(type), q.open, best) })
}

@Composable
fun Price(price: Double?, open: Boolean, best: Boolean = false) {
    if (price == null) {
        Text(if (open) "kein Preis" else "geschlossen", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val (main, last) = priceParts(price)
    Text(buildAnnotatedString {
        append(main)
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.6.em)) { append(last) }
    }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
        color = if (best) goodColor() else MaterialTheme.colorScheme.onSurface)
}

@Composable
fun Message(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}
