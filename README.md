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
Braucht nur Docker. Es entstehen `build/tankradar.aab` (für Google Play) und `build/tankradar.apk` (zum direkten Installieren, aber ohne Android Auto). Beim ersten Lauf baut das Skript das Image `tankradar-build` (JDK 17, Android-SDK, Gradle, siehe `docker/Dockerfile`) und erzeugt den Signaturschlüssel.

- Der Signaturschlüssel liegt in `deploy/tankradar.keystore`, das Passwort daneben in `deploy/tankradar.keystore.txt`. Beides steht nicht in Git und gehört gesichert. Für Google Play ist er der Upload-Schlüssel: Ohne ihn nimmt Play keine neuen Fassungen an (zurücksetzen geht nur über den Play-Support).
- Für ein Update vorher `versionCode` in `app/build.gradle.kts` um eins erhöhen. Play nimmt jede Nummer nur einmal an.
- Google Play verlangt ein aktuelles `targetSdk` (seit 31.08.2026 mindestens 36). Wird es jährlich höher, hier und in `docker/Dockerfile` (`platforms;android-…`) anheben.
- Das Projekt lässt sich auch in Android Studio öffnen (Gradle-Wrapper liegt bei).

## Warum über Google Play
Android Auto zeigt Apps aus der Car App Library nur, wenn Android sie als „aus Google Play installiert“ kennt. Das Einschalten von „Unbekannte Quellen“ in den Entwicklereinstellungen von Android Auto hilft dabei nicht. Auch `adb install -i com.android.vending` hilft nicht. Darum kommt die App über einen **internen Test** in Google Play aufs Handy. Den sieht nur, wer in der Testerliste steht. Prüfen lässt sich das ohne Auto: In der Android-Auto-App unter „Launcher anpassen“ muss Tankradar auftauchen.

## Google Play einrichten (einmalig)
1. Entwicklerkonto anlegen unter [play.google.com/console](https://play.google.com/console) (25 $ einmalig, persönliches Konto). Google prüft die Identität, das kann einige Tage dauern.
2. **App erstellen:** Name „Tankradar“, App, kostenlos. Der Paketname `de.codekorb.tankradar` steht ab dem ersten Upload fest.
3. **Testen → Interner Test → Tester:** eine E-Mail-Liste mit dem eigenen Google-Konto (dem auf dem Handy) anlegen und speichern.
4. **Interner Test → Neuen Release erstellen:** Bei der Frage nach der App-Signatur „Von Google verwaltet“ (Play App Signing) lassen. Dann `build/tankradar.aab` hochladen, Release prüfen und einführen. Fehlen noch Pflichtangaben unter „App-Inhalte“ (z. B. Datenschutzerklärung, Datensicherheit, Standort), zeigt die Console sie an. Sie müssen nur einmal ausgefüllt werden.
5. Auf der Seite „Tester“ den **Link zur Teilnahme** öffnen (mit dem Handy-Konto), Teilnahme annehmen und Tankradar dort über den Play Store installieren.
6. **Vorher die selbst installierte APK deinstallieren.** Play signiert mit einem eigenen Schlüssel, darüber lässt sich die alte Fassung nicht aktualisieren.
7. In der Android-Auto-App unter „Launcher anpassen“ nachsehen, ob Tankradar dort steht. Falls nicht: In der Play Console unter **Test und Release → Einstellungen → Erweiterte Einstellungen → Formfaktoren** „Android Auto“ hinzufügen.

Updates: `versionCode` erhöhen, `bash build-app.sh`, im internen Test einen neuen Release mit der neuen `.aab` erstellen. Der Play Store aktualisiert die App danach von selbst.

## Einrichten auf dem Handy
1. Tankradar öffnen, den Tankerkönig-Schlüssel eintragen (derselbe wie im Dashboard), Sorte wählen und Favoriten suchen: nach Ort oder Adresse oder mit „Hier“ um den aktuellen Standort.
2. Für „In der Nähe“ den Standort erlauben.

Nur für das Handy ohne Android Auto genügt auch die APK: `build/tankradar.apk` aufs Handy kopieren und öffnen oder `adb install -r build/tankradar.apk`.

## Stand
**14.11.2026:** Android Auto zeigte die App nicht, weil sie nicht aus Google Play kam. Jetzt mit targetSdk 36 und App Bundle für einen internen Test in Google Play. Baut fehlerfrei.

**13.11.2026:** Erste Fassung. Baut fehlerfrei und ohne Warnungen, die APK ist signiert. Antwortformat von Tankerkönig mit dem Demo-Schlüssel geprüft. Noch nicht auf dem Handy und nicht in Android Auto ausprobiert.

Offen:
- Auf dem Handy und im Auto testen (Liste, Karte, Umschalter, Navigieren, Standortfreigabe aus dem Auto).
- Wunschtankstellen mit echtem Schlüssel (der Demo-Schlüssel liefert keine Preise für bestimmte Tankstellen).
