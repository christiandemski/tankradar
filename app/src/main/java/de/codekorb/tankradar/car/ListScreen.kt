package de.codekorb.tankradar.car

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.codekorb.tankradar.FUELS
import de.codekorb.tankradar.Locator
import de.codekorb.tankradar.NoLocation
import de.codekorb.tankradar.Prices
import de.codekorb.tankradar.Quote
import de.codekorb.tankradar.R
import de.codekorb.tankradar.Store
import de.codekorb.tankradar.priceText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Karte mit Liste: Favoriten oder die günstigsten Tankstellen in der Nähe. Oben rechts umschalten und aktualisieren.
class ListScreen(ctx: CarContext) : Screen(ctx) {
    private val store = Store(ctx)
    private var quotes: List<Quote>? = null
    private var error: Exception? = null
    private var job: Job? = null

    init {
        // Bei jeder Rückkehr auf diese Seite neu laden (aus dem Zwischenspeicher, falls noch frisch)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = load(force = false)
        })
    }

    private fun load(force: Boolean) {
        job?.cancel()
        job = lifecycleScope.launch {
            try {
                quotes = Prices.load(carContext, store, store.nearby, force); error = null
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                quotes = emptyList(); error = e
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        if (store.apiKey.isBlank()) return message("Bitte zuerst in Tankradar auf dem Handy den Tankerkönig-Schlüssel eintragen.")
        if (error is NoLocation && !Locator.allowed(carContext)) {
            return message(error!!.message!!, Action.Builder().setTitle("Standort erlauben").setOnClickListener {
                carContext.requestPermissions(Locator.PERMISSIONS.toList()) { _, _ -> load(force = false) }
            }.build())
        }
        val type = store.type
        val b = PlaceListMapTemplate.Builder()
            .setTitle((if (store.nearby) "In der Nähe" else "Favoriten") + " · " + FUELS[type])
            .setHeaderAction(Action.APP_ICON)
            .setCurrentLocationEnabled(Locator.allowed(carContext))
            .setActionStrip(ActionStrip.Builder()
                .addAction(Action.Builder().setTitle(if (store.nearby) "Favoriten" else "In der Nähe").setOnClickListener {
                    store.nearby = !store.nearby; quotes = null; error = null; invalidate(); load(force = false)
                }.build())
                .addAction(Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_refresh)).build())
                    .setOnClickListener { load(force = true) }.build())
                .build())
        val list = quotes ?: return b.setLoading(true).build()

        val limit = carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
        val best = list.firstOrNull()?.price(type).takeIf { list.size > 1 }
        val items = ItemList.Builder()
        items.setNoItemsMessage(error?.message ?: if (store.nearby) "Im Umkreis hat gerade keine Tankstelle geöffnet."
            else "Noch keine Favoriten. Sie lassen sich in Tankradar auf dem Handy auswählen.")
        list.take(limit).forEach { q -> items.addItem(row(q, type, best)) }
        return b.setItemList(items.build()).build()
    }

    private fun row(q: Quote, type: String, best: Double?): Row {
        val s = q.station
        val p = q.price(type)
        val r = Row.Builder()
            .setTitle(if (p != null) priceText(p) + "  " + s.name else s.name)
            .setOnClickListener { screenManager.push(DetailScreen(carContext, q)) }
        // Entfernung als DistanceSpan, damit Android Auto sie in der passenden Einheit zeigt
        val address = listOf(s.street, s.place).filter { it.isNotBlank() }.joinToString(", ")
        if (q.dist != null) {
            val text = SpannableString("  · $address")
            text.setSpan(DistanceSpan.create(Distance.create(q.dist, Distance.UNIT_KILOMETERS)), 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            r.addText(text)
        } else {
            // Ohne Standort keine Entfernung. Die Kartenvorlage lässt Zeilen ohne DistanceSpan nur zu, wenn sie
            // „browsable“ sind (sonst stürzt die App ab). Das passt: ein Tipp öffnet ohnehin die Detailseite.
            r.addText(address).setBrowsable(true)
        }
        if (p == null) r.addText(if (q.open) "kein Preis" else "geschlossen")

        val marker = PlaceMarker.Builder()
        if (p != null && p == best) marker.setColor(CarColor.GREEN)
        r.setMetadata(Metadata.Builder().setPlace(Place.Builder(CarLocation.create(s.lat, s.lng)).setMarker(marker.build()).build()).build())
        return r.build()
    }

    private fun message(text: String, action: Action? = null): Template {
        val b = MessageTemplate.Builder(text).setTitle("Tankradar").setHeaderAction(Action.APP_ICON)
        if (action != null) b.addAction(action)
        return b.build()
    }
}
