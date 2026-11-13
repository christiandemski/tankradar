package de.codekorb.tankradar.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import de.codekorb.tankradar.FUELS
import de.codekorb.tankradar.Quote
import de.codekorb.tankradar.Store
import de.codekorb.tankradar.priceText

// Eine Tankstelle: Preise aller Sorten, Navigation starten, als Favorit merken
class DetailScreen(ctx: CarContext, private val q: Quote) : Screen(ctx) {
    private val store = Store(ctx)

    override fun onGetTemplate(): Template {
        val s = q.station
        val rows = FUELS.map { (k, name) ->
            Row.Builder().setTitle(name).addText(q.price(k)?.let(::priceText) ?: if (q.open) "kein Preis" else "geschlossen").build()
        } + Row.Builder().setTitle(listOf(s.street, s.place).filter { it.isNotBlank() }.joinToString(", ").ifEmpty { s.name }).build()
        val limit = carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE)

        val pane = Pane.Builder()
        rows.take(limit).forEach(pane::addRow)
        pane.addAction(Action.Builder().setTitle("Navigieren").setBackgroundColor(CarColor.BLUE).setOnClickListener {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:${s.lat},${s.lng}")))
        }.build())
        val fav = store.isFavorite(s.id)
        pane.addAction(Action.Builder().setTitle(if (fav) "Favorit entfernen" else "Als Favorit merken").setOnClickListener {
            if (fav) store.removeFavorite(s.id)
            else if (!store.addFavorite(s)) CarToast.makeText(carContext, "Mehr als ${Store.MAX_FAVORITES} Favoriten gehen nicht", CarToast.LENGTH_LONG).show()
            invalidate()
        }.build())

        return PaneTemplate.Builder(pane.build())
            .setTitle(if (q.open) s.name else "${s.name} (geschlossen)")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
