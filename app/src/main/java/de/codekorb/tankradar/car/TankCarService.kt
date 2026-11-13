package de.codekorb.tankradar.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

// Einstieg für Android Auto
class TankCarService : CarAppService() {
    // Nur Android Auto und die anderen bekannten Hosts von Google dürfen sich verbinden (Liste aus der Car App Library).
    // Beim Testen mit der Desktop Head Unit (Debug-Build) jeder Host.
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        else HostValidator.Builder(applicationContext).addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample).build()

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = ListScreen(carContext)
    }
}
