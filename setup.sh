#!/bin/bash
set -e

echo "🔧 BlueScreen geliştirme ortamı kuruluyor..."

# ─── Bağımlılıklar ───────────────────────────────────────────────────────────
sudo apt-get update -qq
sudo apt-get install -y wget unzip curl git > /dev/null

# ─── Gradle 8.2 ──────────────────────────────────────────────────────────────
GRADLE_VERSION="8.2"
GRADLE_DIR="/opt/gradle"
if [ ! -d "$GRADLE_DIR" ]; then
    echo "📦 Gradle $GRADLE_VERSION indiriliyor..."
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip
    sudo unzip -q /tmp/gradle.zip -d /opt
    sudo ln -sf "/opt/gradle-${GRADLE_VERSION}" "$GRADLE_DIR"
    rm /tmp/gradle.zip
    echo "✅ Gradle kuruldu"
fi
export PATH="$PATH:/opt/gradle/bin"

# ─── Android SDK ─────────────────────────────────────────────────────────────
ANDROID_HOME="/opt/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "📦 Android SDK komut satırı araçları indiriliyor..."
    sudo mkdir -p "$ANDROID_HOME/cmdline-tools"
    wget -q "$CMDLINE_TOOLS_URL" -O /tmp/cmdline-tools.zip
    sudo unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    sudo mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" 2>/dev/null || true
    sudo chown -R "$(whoami)" "$ANDROID_HOME"
    rm /tmp/cmdline-tools.zip
fi

export ANDROID_HOME="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# ─── SDK Bileşenleri ─────────────────────────────────────────────────────────
echo "📦 Android SDK bileşenleri kuruluyor (bu biraz sürebilir)..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "platform-tools" > /dev/null
echo "✅ Android SDK hazır"

# ─── Gradle Wrapper ──────────────────────────────────────────────────────────
cd /workspaces/BlueScreen 2>/dev/null || cd "$(dirname "$0")/.."
if [ ! -f "gradlew" ]; then
    echo "⚙️  Gradle wrapper oluşturuluyor..."
    gradle wrapper --gradle-version 8.2 --distribution-type bin
    chmod +x gradlew
    echo "✅ gradlew oluşturuldu"
fi

# ─── Özet ────────────────────────────────────────────────────────────────────
echo ""
echo "🎉 Kurulum tamamlandı!"
echo ""
echo "📱 APK derlemek için:"
echo "   ./gradlew assembleDebug"
echo ""
echo "📂 APK çıktısı:"
echo "   app/build/outputs/apk/debug/app-debug.apk"
