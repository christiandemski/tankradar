# Tankradar

Android-App für Spritpreise, auf dem Handy und in **Android Auto**. Hervorgegangen aus der Spritpreis-Kachel des Dashboards (`../dashboard`). Nur für den eigenen Gebrauch, wird nicht veröffentlicht.

- **Favoriten:** bis zu 10 gemerkte Tankstellen, die günstigste zuerst.
- **In der Nähe:** die günstigsten geöffneten Tankstellen um den aktuellen Standort (Umkreis und Anzahl einstellbar).
- Umschalten mit einem Tipp, auf dem Handy oben, in Android Auto oben rechts.
- Sorte E5, E10 oder Diesel. Die anderen Sorten stehen klein darunter (Handy) oder in der Detailansicht (Auto).
- Android Auto: Karte mit Liste, die günstigste Tankstelle hat eine grüne Markierung. Ein Tipp öffnet alle Preise, „Navigieren“ (startet Google Maps oder die eingestellte Navi-App) und „Als Favorit merken“.
- Handy: Tipp auf eine Tankstelle öffnet die Karten-App, der Stern merkt sie als Favorit.

Die Preise kommen direkt von Tankerkönig (Daten der Markttransparenzstelle, CC BY 4.0). Ein Server ist nicht nötig. Der Schlüssel ist derselbe wie im Dashboard. Die App fragt höchstens alle 5 Minuten neu ab; „Aktualisieren“ holt frühestens nach einer Minute neue Preise.

## Technik
Kotlin, Jetpack Compose (Handy) und die Car App Library (Android Auto, Kategorie POI, Vorlagen `PlaceListMapTemplate` und `PaneTemplate`). Einstellungen und Favoriten liegen in den SharedPreferences, die Handy und Auto gemeinsam nutzen. Den Standort liefert das Handy über den `LocationManager`, ohne Google-Play-Dienste. Ab Android 11.

```
app/src/main/java/de/codekorb/tankradar/
  Tanker.kt          Abfrage bei Tankerkönig, Zwischenspeicher, Favoriten oder in der Nähe laden
  Store.kt           Einstellungen und Favoriten
  Locator.kt         Standort
  Format.kt          Preise ("1,74⁹ €"), Entfernungen
  MainActivity.kt    Einstieg und Farben (wie im Dashboard)
  PricesScreen.kt    Startseite auf dem Handy
  SettingsScreen.kt  Schlüssel, Sorte, Umkreis, Favoriten suchen und verwalten
  car/               Android Auto: Dienst, Liste mit Karte, Detailansicht
```

## Bauen
```
bash build-app.sh
```
Braucht nur Docker. Es entsteht `build/tankradar.apk`. Beim ersten Lauf baut das Skript das Image `tankradar-build` (JDK 17, Android-SDK, Gradle, siehe `docker/Dockerfile`) und erzeugt den Signaturschlüssel.

- Der Signaturschlüssel liegt in `deploy/tankradar.keystore`, das Passwort daneben in `deploy/tankradar.keystore.txt`. Beides steht nicht in Git und gehört gesichert: Ohne den Schlüssel lässt sich die installierte App nicht aktualisieren, nur deinstallieren und neu einrichten.
- Für ein Update vorher `versionCode` in `app/build.gradle.kts` um eins erhöhen.
- Das Projekt lässt sich auch in Android Studio öffnen (Gradle-Wrapper liegt bei).

## Installieren
1. `build/tankradar.apk` aufs Handy kopieren und dort öffnen. Android fragt einmal, ob Apps aus dieser Quelle installiert werden dürfen. Oder per USB: `adb install -r build/tankradar.apk`.
2. Tankradar öffnen, den Tankerkönig-Schlüssel eintragen (derselbe wie im Dashboard), Sorte wählen und Favoriten suchen: nach Ort oder Adresse oder mit „Hier“ um den aktuellen Standort.
3. Für „In der Nähe“ den Standort erlauben.

## Android Auto einrichten (einmalig)
Android Auto zeigt Apps, die nicht aus dem Play Store kommen, erst nach Freigabe in den Entwickleroptionen:

1. Auf dem Handy die Einstellungen von Android Auto öffnen (Einstellungen → Verbundene Geräte → Verbindungseinstellungen → Android Auto, je nach Hersteller etwas anders).
2. Ganz unten zehnmal auf **Version** tippen und bestätigen. Damit sind die Entwickleroptionen eingeschaltet.
3. Oben rechts ⋮ → **Entwicklereinstellungen** → **Unbekannte Quellen** einschalten.
4. Handy mit dem Auto verbinden. Tankradar erscheint in der App-Übersicht von Android Auto. Falls nicht: in den Android-Auto-Einstellungen unter „Launcher anpassen“ einblenden.

## Stand
**13.11.2026:** Erste Fassung. Baut fehlerfrei und ohne Warnungen, die APK ist signiert. Antwortformat von Tankerkönig mit dem Demo-Schlüssel geprüft. Noch nicht auf dem Handy und nicht in Android Auto ausprobiert.

Offen:
- Auf dem Handy und im Auto testen (Liste, Karte, Umschalter, Navigieren, Standortfreigabe aus dem Auto).
- Wunschtankstellen mit echtem Schlüssel (der Demo-Schlüssel liefert keine Preise für bestimmte Tankstellen).
