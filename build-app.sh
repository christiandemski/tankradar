#!/usr/bin/env bash
# Baut Tankradar als signierte APK und als App Bundle für Google Play in Docker (kein Android Studio nötig).
#
#   bash build-app.sh      erzeugt build/tankradar.apk und build/tankradar.aab
#
# Beim ersten Lauf entsteht das Build-Image (JDK, Android-SDK, Gradle; docker/Dockerfile) und der Signaturschlüssel
# deploy/tankradar.keystore mit dem Passwort in deploy/tankradar.keystore.txt. Beides steht nicht in Git und gehört gesichert:
# Ohne den Schlüssel lässt sich die installierte App nicht aktualisieren, nur deinstallieren und neu einrichten.
# Vor jedem neuen Paket versionCode in app/build.gradle.kts um eins erhöhen, sonst nimmt Android es nicht als Update an.
set -euo pipefail
cd "$(dirname "$0")"

export MSYS_NO_PATHCONV=1   # Git Bash soll die Pfade im Container nicht umschreiben
DIR="$(pwd -W 2>/dev/null || pwd)"
IMG=tankradar-build
docker build -q -t $IMG docker >/dev/null   # beim ersten Lauf und nach Änderungen am Dockerfile dauert das etwas
# Gradle-Downloads liegen in einem eigenen Docker-Volume, damit der zweite Lauf schnell geht
RUN=(docker run --rm -v "$DIR:/app" -v tankradar-gradle:/cache/gradle -w /app $IMG)

if [ ! -f deploy/tankradar.keystore ]; then
  mkdir -p deploy
  PW="$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9')"
  "${RUN[@]}" keytool -genkeypair -keystore deploy/tankradar.keystore -alias tankradar -keyalg RSA -keysize 2048 -validity 36500 \
    -storepass "$PW" -keypass "$PW" -dname "CN=Tankradar" >/dev/null
  printf 'Signaturschlüssel für Tankradar (Alias tankradar)\nPasswort: %s\n' "$PW" > deploy/tankradar.keystore.txt
  echo "Neuer Signaturschlüssel: deploy/tankradar.keystore, Passwort in deploy/tankradar.keystore.txt. Bitte sichern."
fi

"${RUN[@]}" gradle --no-daemon -q assembleRelease bundleRelease
mkdir -p build
cp app/build/outputs/apk/release/app-release.apk build/tankradar.apk
cp app/build/outputs/bundle/release/app-release.aab build/tankradar.aab
echo "Fertig: build/tankradar.apk (Handy), build/tankradar.aab (Google Play)"
