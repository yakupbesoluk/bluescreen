#!/bin/bash
set -euo pipefail

# ─── Çalışma dizinini proje köküne sabitle ────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"
echo "📁 Proje kökü: $PROJECT_ROOT"

# ─── Bağımlılıklar ────────────────────────────────────────────────────────────
echo "📦 Sistem paketleri güncelleniyor..."
sudo apt-get update -qq
sudo apt-get install -y wget unzip curl git > /dev/null
echo "✅ Sistem paketleri hazır"

# ─── Gradle 8.2 ──────────────────────────────────────────────────────────────
GRADLE_VERSION="8.2"
GRADLE_INSTALL_DIR="/opt/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_INSTALL_DIR/bin/gradle"

if [ ! -f "$GRADLE_BIN" ]; then
    echo "📦 Gradle $GRADLE_VERSION indiriliyor..."
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip
    sudo unzip -q /tmp/gradle.zip -d /opt
    rm /tmp/gradle.zip
    echo "✅ Gradle $GRADLE_VERSION kuruldu"
else
    echo "✅ Gradle zaten kurulu"
fi

export PATH="$PATH:$GRADLE_INSTALL_DIR/bin"

# ─── Android SDK ─────────────────────────────────────────────────────────────
ANDROID_SDK="/opt/android-sdk"
CMDLINE_LATEST="$ANDROID_SDK/cmdline-tools/latest"

if [ ! -d "$CMDLINE_LATEST" ]; then
    echo "📦 Android SDK komut satırı araçları indiriliyor..."
    sudo mkdir -p "$ANDROID_SDK/cmdline-tools"
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
         -O /tmp/cmdline-tools.zip
    sudo unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tmp
    # Google zip içinde "cmdline-tools" klasörü geliyor → "latest" olarak taşı
    if [ -d "/tmp/cmdline-tmp/cmdline-tools" ]; then
        sudo mv /tmp/cmdline-tmp/cmdline-tools "$CMDLINE_LATEST"
    else
        sudo mv /tmp/cmdline-tmp/* "$CMDLINE_LATEST"
    fi
    sudo rm -rf /tmp/cmdline-tmp /tmp/cmdline-tools.zip
    sudo chown -R "$(whoami):$(whoami)" "$ANDROID_SDK"
    echo "✅ SDK komut satırı araçları kuruldu"
else
    echo "✅ SDK komut satırı araçları zaten kurulu"
fi

export ANDROID_HOME="$ANDROID_SDK"
export PATH="$PATH:$ANDROID_SDK/cmdline-tools/latest/bin:$ANDROID_SDK/platform-tools"

# ─── SDK Lisansları ve Bileşenler ─────────────────────────────────────────────
echo "📦 Android SDK bileşenleri kontrol ediliyor..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

for pkg in "platforms;android-34" "build-tools;34.0.0" "platform-tools"; do
    if sdkmanager --list_installed 2>/dev/null | grep -q "$pkg"; then
        echo "  ✓ $pkg zaten kurulu"
    else
        echo "  ⬇  $pkg kuruluyor..."
        sdkmanager "$pkg" > /dev/null
    fi
done
echo "✅ Android SDK bileşenleri hazır"

# ─── Gradle Wrapper ──────────────────────────────────────────────────────────
if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
    echo "⚙️  Gradle wrapper oluşturuluyor..."
    gradle wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
    chmod +x gradlew
    echo "✅ gradlew oluşturuldu"
else
    chmod +x "$PROJECT_ROOT/gradlew"
    echo "✅ gradlew zaten mevcut"
fi

# ─── ~/.bashrc'e kalıcı PATH ekle ────────────────────────────────────────────
BASHRC="$HOME/.bashrc"
if ! grep -q "android-sdk" "$BASHRC" 2>/dev/null; then
    cat >> "$BASHRC" << 'EOF'

# BlueScreen – Android SDK & Gradle
export ANDROID_HOME=/opt/android-sdk
export PATH="$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/gradle-8.2/bin"
EOF
    echo "✅ PATH bilgileri ~/.bashrc'e eklendi"
fi

# ─── Özet ─────────────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║  🎉  BlueScreen geliştirme ortamı hazır!     ║"
echo "╠══════════════════════════════════════════════╣"
echo "║                                              ║"
echo "║  Debug APK derlemek için:                    ║"
echo "║    ./gradlew assembleDebug                   ║"
echo "║                                              ║"
echo "║  Çıktı:                                      ║"
echo "║    app/build/outputs/apk/debug/              ║"
echo "║                                              ║"
echo "╚══════════════════════════════════════════════╝"
