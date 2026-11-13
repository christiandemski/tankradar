package de.codekorb.tankradar

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

// Einstellungen: Schlüssel, Sorte, Umkreis, Favoriten verwalten und suchen. Jede Änderung wird sofort gespeichert.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(store.apiKey) }
    var type by remember { mutableStateOf(store.type) }
    var radius by remember { mutableStateOf(store.radius) }
    var limit by remember { mutableStateOf(store.limit.toFloat()) }
    var favorites by remember { mutableStateOf(store.favorites) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<Result<List<Quote>>?>(null) }
    val canClose = key.isNotBlank()

    BackHandler(enabled = canClose, onBack = onClose)

    fun search(here: Boolean) = scope.launch {
        searching = true
        found = runCatching {
            if (store.apiKey.isBlank()) throw TankerException("Zuerst den Tankerkönig-Schlüssel eintragen")
            val (lat, lng) = if (here) {
                Locator.current(ctx)?.let { it.latitude to it.longitude } ?: throw NoLocation("Der Standort ist gerade nicht verfügbar.")
            } else withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val a = Geocoder(ctx, Locale.GERMANY).getFromLocationName(query, 1)?.firstOrNull()
                    ?: throw TankerException("„$query“ nicht gefunden")
                a.latitude to a.longitude
            }
            Tanker.around(store.apiKey, lat, lng, radius.toDouble())
        }
        searching = false
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Locator.allowed(ctx)) search(here = true)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Einstellungen") }, navigationIcon = {
            if (canClose) IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
        })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            item {
                Section("Tankerkönig")
                Text("Die Preise kommen von Tankerkönig (Daten der Markttransparenzstelle, CC BY 4.0). Dafür braucht es einen kostenlosen Schlüssel.",
                    Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://creativecommons.tankerkoenig.de/"))) },
                    Modifier.padding(horizontal = 4.dp)) { Text("Schlüssel beantragen") }
                OutlinedTextField(key, { key = it; store.apiKey = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text("Tankerkönig-Schlüssel") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false))
            }
            item {
                Section("Sorte")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    FUELS.entries.forEachIndexed { i, (k, name) ->
                        SegmentedButton(selected = type == k, shape = SegmentedButtonDefaults.itemShape(i, FUELS.size),
                            onClick = { type = k; store.type = k }) { Text(name.removePrefix("Super ")) }
                    }
                }
            }
            item {
                Section("In der Nähe")
                Text("Umkreis: ${kmText(radius.toDouble())}", Modifier.padding(horizontal = 16.dp))
                Slider(radius, { radius = it.roundToInt().toFloat() }, Modifier.padding(horizontal = 16.dp),
                    valueRange = 1f..25f, steps = 23, onValueChangeFinished = { store.radius = radius })
                Text("Anzahl: ${limit.roundToInt()} Tankstellen", Modifier.padding(horizontal = 16.dp))
                Slider(limit, { limit = it }, Modifier.padding(horizontal = 16.dp),
                    valueRange = 1f..10f, steps = 8, onValueChangeFinished = { store.limit = limit.roundToInt() })
            }
            item { Section("Favoriten (${favorites.size} von ${Store.MAX_FAVORITES})") }
            if (favorites.isEmpty()) item {
                Text("Noch keine. Unten Tankstellen suchen und mit + merken.", Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(favorites, key = { "fav" + it.id }) { s ->
                ListItem(headlineContent = { Text(s.name) }, supportingContent = { Text(listOf(s.street, s.place).filter { it.isNotBlank() }.joinToString(", ")) },
                    trailingContent = {
                        IconButton(onClick = { store.removeFavorite(s.id); favorites = store.favorites }) { Icon(Icons.Filled.Delete, "Entfernen") }
                    })
            }
            item {
                Section("Tankstellen suchen")
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text("Ort oder Adresse") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) search(here = false) }))
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { search(here = false) }, enabled = query.isNotBlank() && !searching) {
                        Icon(Icons.Filled.Search, null); Text("  Suchen")
                    }
                    OutlinedButton(onClick = { if (Locator.allowed(ctx)) search(here = true) else askLocation.launch(Locator.PERMISSIONS) },
                        enabled = !searching) {
                        Icon(Icons.Filled.LocationOn, null); Text("  Hier")
                    }
                }
                if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                found?.exceptionOrNull()?.let { Text(it.message ?: "Fehler", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
                if (found?.getOrNull()?.isEmpty() == true) Text("Keine Tankstellen im Umkreis.", Modifier.padding(horizontal = 16.dp))
            }
            items(found?.getOrNull().orEmpty(), key = { "found" + it.station.id }) { q ->
                val s = q.station
                val fav = favorites.any { it.id == s.id }
                ListItem(headlineContent = { Text(s.name) },
                    supportingContent = { Text(listOfNotNull(s.street, s.place, q.dist?.let(::kmText)).filter { it.isNotBlank() }.joinToString(" · ")) },
                    trailingContent = {
                        IconButton(onClick = {
                            if (fav) store.removeFavorite(s.id)
                            else if (!store.addFavorite(s)) Toast.makeText(ctx, "Mehr als ${Store.MAX_FAVORITES} Favoriten gehen nicht.", Toast.LENGTH_SHORT).show()
                            favorites = store.favorites
                        }) {
                            if (fav) Icon(Icons.Filled.Check, "Favorit entfernen", tint = MaterialTheme.colorScheme.primary)
                            else Icon(Icons.Filled.Add, "Als Favorit merken")
                        }
                    })
            }
        }
    }
}

@Composable
private fun Section(title: String) =
    Text(title, Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
