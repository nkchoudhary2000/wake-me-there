# Wake Me There — Location-Based Android Alarm Clock

Wake Me There is a **100% free, ad-free, offline-resilient Location Alarm Clock** for Android built with Kotlin and Jetpack Compose. It triggers loud, wakeful alarms when your device enters a user-defined geographical radius (geofence), ensuring you never miss your train stop, highway exit, or destination even when sleeping.

---

## Architecture Overview

```
                               ┌─────────────────────────────────────────────────────┐
                               │                    UI Layer                         │
                               │   (Jetpack Compose, Material 3, Single-Activity)    │
                               │  - HomeScreen (Alarm list, toggles, test trigger)   │
                               │  - MapPickerScreen (Google Maps + Offline Radar)    │
                               │  - AlarmConfigScreen (Ringtone picker, radius)      │
                               │  - ActiveAlarmActivity (Lockscreen Override UI)     │
                               └─────────────────────────┬───────────────────────────┘
                                                         │
                                                         ▼
                               ┌─────────────────────────────────────────────────────┐
                               │                 Presentation Layer                  │
                               │     (HomeViewModel, MapPickerViewModel, etc.)       │
                               └─────────────────────────┬───────────────────────────┘
                                                         │
                                                         ▼
                               ┌─────────────────────────────────────────────────────┐
                               │                  Repository Layer                   │
                               │        (AlarmRepository / AlarmRepositoryImpl)      │
                               └──────────────┬─────────────────────────┬────────────┘
                                              │                         │
                                              ▼                         ▼
                    ┌───────────────────────────────────┐    ┌───────────────────────────────────┐
                    │            Data Layer             │    │          Location Layer           │
                    │      Room Database (SQLite)       │    │   Dual-Layer Offline Tracking     │
                    │  - AlarmEntity / AlarmDao         │    │  - Google Play GeofencingClient   │
                    │  - Local coordinate storage       │    │  - Foreground GPS LocationService │
                    └───────────────────────────────────┘    └─────────────────┬─────────────────┘
                                                                               │
                                                                               ▼
                                                             ┌───────────────────────────────────┐
                                                             │            Audio & Wake           │
                                                             │   - AlarmPlayerService            │
                                                             │   - MediaPlayer (USAGE_ALARM)     │
                                                             │   - WakeLock & FullScreenIntent   │
                                                             └───────────────────────────────────┘
```

---

## Key Features & Production Mechanisms

### 1. Bulletproof Dual-Layer Background Tracking
Aggressive OEM battery savers and Android Doze mode frequently throttle or kill background geofences. Wake Me There employs a **dual-redundancy mechanism**:
1. **Google Play Services Geofencing API (`GeofencingClient`)**: OS-level, battery-optimized geofence monitoring.
2. **Foreground Proximity Service (`LocationTrackerService`)**: Bound to a persistent notification (`FOREGROUND_SERVICE_LOCATION`), requesting raw GPS updates via `FusedLocationProviderClient(Priority.PRIORITY_HIGH_ACCURACY)`. It actively computes geodesic distance (`Location.distanceBetween`) against all active destinations.
3. If **either** layer detects entry within the target radius, the alarm triggers instantly!

### 2. 100% Offline Resiliency
- GPS satellite signals function globally without internet connection or cellular signal.
- If Google Maps tiles fail to load offline, the app features an **Interactive Offline Radar/Compass view (`OfflineMapFallback.kt`)** with live distance calculation and direct manual coordinate input.
- All alarms, coordinates, and configurations are stored offline in a local **Room SQLite Database**.

### 3. Lockscreen Override & Screen Wake (`ActiveAlarmActivity`)
When triggered while the device is sleeping or locked:
- Acquires `SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP` via `WakeLockHelper`.
- Configures `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and calls `keyguardManager.requestDismissKeyguard()`.
- Dispatches a high-priority notification with `setFullScreenIntent()` to turn on the display over the keyguard.
- Features a **Massive "Slide to Turn Off Alarm" (`SlideToDismissBar`)** component with haptic feedback to prevent accidental pocket dismissals.

### 4. High-Priority Alarm Audio (`USAGE_ALARM`)
- `MediaPlayer` plays over `AudioAttributes.USAGE_ALARM` and `AudioAttributes.CONTENT_TYPE_SONIFICATION`.
- Rings at full alarm volume even if the phone is set to Silent or Do Not Disturb (DND).
- Requests `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` to pause other music or podcasts.
- Synchronized multi-stage vibration waveform pattern.

### 5. Battery Optimization Opt-Out
- Prompts user to exempt the app using `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Prevents Android Doze mode from freezing the GPS radio while the phone is in your pocket.

---

## Project Structure

```
wake-me-there/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/wakemethere/app/
        │   ├── WakeMeThereApp.kt
        │   ├── data/
        │   │   ├── local/ (AlarmEntity, AlarmDao, AlarmDatabase)
        │   │   ├── model/ (LocationAlarm)
        │   │   └── repository/ (AlarmRepository, AlarmRepositoryImpl)
        │   ├── util/
        │   │   ├── BatteryOptimizationHelper.kt
        │   │   ├── DistanceCalculator.kt
        │   │   ├── NotificationHelper.kt
        │   │   └── PermissionUtils.kt
        │   ├── location/
        │   │   ├── GeofenceManager.kt
        │   │   ├── GeofenceBroadcastReceiver.kt
        │   │   └── LocationTrackerService.kt
        │   ├── alarm/
        │   │   ├── WakeLockHelper.kt
        │   │   ├── AlarmPlayerService.kt
        │   │   └── AlarmActionReceiver.kt
        │   ├── receiver/
        │   │   └── BootReceiver.kt
        │   └── ui/
        │       ├── MainActivity.kt
        │       ├── ActiveAlarmActivity.kt
        │       ├── navigation/ (Screen)
        │       ├── theme/ (Color, Theme, Type)
        │       ├── components/ (SlideToDismissBar, PermissionBanner, BatteryOptimizationBanner)
        │       ├── home/ (HomeScreen, HomeViewModel, AlarmItemCard)
        │       ├── map/ (MapPickerScreen, MapPickerViewModel, OfflineMapFallback)
        │       ├── config/ (AlarmConfigScreen, AlarmConfigViewModel)
        │       └── active/ (ActiveAlarmScreen)
        └── res/
            ├── drawable/ (ic_launcher_foreground.xml)
            ├── mipmap-anydpi-v26/ (ic_launcher.xml, ic_launcher_round.xml)
            ├── values/ (colors.xml, strings.xml, themes.xml)
            └── xml/ (backup_rules.xml, data_extraction_rules.xml)
```

---

## Setup & Running

1. **Clone or Open Project**:
   Open this folder in **Android Studio Hedgehog / Iguana / Jellyfish (or newer)**.
2. **Google Maps API Key**:
   Set your Google Maps API Key in `local.properties`:
   ```properties
   MAPS_API_KEY=AIzaSy...YourKey...
   ```
   *(Note: The app will run and function 100% offline using the built-in Offline Radar even if no API key is provided).*
3. **Build & Run**:
   Run the app on any device running Android 8.0 (API 26) through Android 14+ (API 34).

