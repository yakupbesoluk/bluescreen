# 📱 BlueScreen – Android Mavi Işık Filtresi

Ekran üstü overlay ile çalışan, zamanlayıcılı, QS tile destekli mavi ışık filtresi uygulaması.

---

## ✨ Özellikler

| Özellik | Açıklama |
|---|---|
| **Overlay Filtresi** | `SYSTEM_ALERT_WINDOW` ile tam ekran amber katmanı |
| **Sıcaklık Ayarı** | 1000K (sıcak amber) – 6500K (saydam) arası |
| **Yoğunluk Ayarı** | %0 – %100 arası |
| **QS Tile** | Hızlı ayar panelinden tek dokunuşla aç/kapat |
| **Zamanlayıcı** | Her gün belirtilen saatte otomatik aç/kapat |
| **Boot Autostart** | Telefon açıldığında filtreyi otomatik başlat |
| **Foreground Service** | Uygulama kapalıyken bile çalışır |

---

## 🚀 Geliştirme Ortamı (GitHub Codespaces)

### Codespaces ile başlat:
1. Bu repoyu GitHub'a push et
2. **Code → Codespaces → Create codespace on main** tıkla
3. Container açıldığında `.devcontainer/setup.sh` otomatik çalışır
4. Setup tamamlandıktan sonra APK derle:

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔨 GitHub Actions ile Build

Repository'ye push ettiğinde otomatik olarak APK derlenir.

**Manuel build:**
1. GitHub → Actions → "📱 Build APK"
2. **Run workflow** → build tipini seç (debug / release / both)
3. Tamamlandıktan sonra **Artifacts** bölümünden APK'yı indir

---

## 📂 Proje Yapısı

```
BlueScreen/
├── .devcontainer/
│   ├── devcontainer.json       # Codespaces konfigürasyonu
│   └── setup.sh                # Android SDK + Gradle kurulum scripti
├── .github/
│   └── workflows/
│       └── build.yml           # CI/CD – APK otomatik derleme
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/yakup/bluescreen/
│           ├── MainActivity.kt         # Ana ekran UI
│           ├── FilterService.kt        # Foreground overlay servisi
│           ├── FilterTileService.kt    # Quick Settings tile
│           ├── BootReceiver.kt         # Boot'ta otomatik başlatma
│           ├── ScheduleReceiver.kt     # Zamanlayıcı alarm alıcısı
│           ├── ScheduleManager.kt      # AlarmManager yönetimi
│           └── PreferencesManager.kt   # SharedPreferences wrapper
└── ...
```

---

## 🔐 Gerekli İzinler

| İzin | Neden |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Ekran üstü overlay çizmek için |
| `FOREGROUND_SERVICE` | Arka planda çalışmak için |
| `RECEIVE_BOOT_COMPLETED` | Boot'ta otomatik başlatmak için |
| `SCHEDULE_EXACT_ALARM` | Zamanlayıcı için tam zamanlı alarm |

> ⚠️ `SYSTEM_ALERT_WINDOW` izni uygulama ayarlarından **manuel olarak** verilmesi gerekir.
> Uygulama ilk açıldığında sizi ayarlara yönlendirir.

---

## 🎨 Renk Sıcaklığı Algoritması

```
t = (Kelvin - 1000) / (6500 - 1000)   // 0=sıcak, 1=soğuk
R = 255
G = 55 + t × 120
B = 0  + t × 60
Alpha = (yoğunluk × 220/100) × (1 - t)
```

Soğuk sıcaklıklarda (6500K'ya yakın) alpha değeri sıfıra yaklaşır → filtre görünmez olur.

---

## 📦 Minimum Gereksinimler

- Android **8.0 (API 26)** ve üzeri
- RAM: ~10MB
- Depolama: ~3MB

---

## 🛠 Geliştirme Gereksinimleri

- JDK 17
- Android SDK 34
- Gradle 8.2
- Kotlin 1.9.22
