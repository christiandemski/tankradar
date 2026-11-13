# Build-Umgebung für Tankradar: JDK 17, Android-SDK und Gradle. Wird von build-app.sh gebaut und genutzt.
FROM eclipse-temurin:17-jdk
ENV ANDROID_HOME=/opt/android-sdk GRADLE_USER_HOME=/cache/gradle
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl && rm -rf /var/lib/apt/lists/*
RUN mkdir -p $ANDROID_HOME/cmdline-tools \
 && curl -fsSL -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q /tmp/tools.zip -d $ANDROID_HOME/cmdline-tools && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && rm /tmp/tools.zip
# Das nimmt die Lizenzbedingungen des Android-SDK an
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null \
 && $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-35" "build-tools;34.0.0" "platform-tools" >/dev/null
RUN curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip \
 && unzip -q /tmp/gradle.zip -d /opt && ln -s /opt/gradle-8.11.1/bin/gradle /usr/local/bin/gradle && rm /tmp/gradle.zip
WORKDIR /app
