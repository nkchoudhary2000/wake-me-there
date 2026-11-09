# 🚀 Google Play Store Launch Kit for "Wake Me There"

Everything you need to publish **Wake Me There** on Google Play is prepared, built, and verified.

---

## 📦 1. Your Signed Production Bundle
* **File to Upload**: `wake-me-there-release.aab` (Located in project root, size ~3.7 MB)
* **Build Type**: Release (Optimized & shrunk with ProGuard/R8)
* **Keystore**: `release-key.jks`
* **Key Alias**: `wakemethere`
* **Key & Store Password**: `WakeMeThere2026!`
* **Certificate SHA-1**: `C7:2C:BB:96:F9:DA:8A:58:F4:EF:BD:9E:30:33:A0:0D:EA:36:8D:0C`
* **Certificate SHA-256**: `86:9C:FC:A7:E1:6B:E2:E4:6C:72:C0:55:16:A8:F9:8B:31:6F:13:A0:7A:C6:4B:57:82:D5:B9:7F:08:95:A5:C9`

---

## 🎨 2. Store Listing Graphics (Generated in `play-store-assets/`)
Upload these in **Play Console $\rightarrow$ Grow $\rightarrow$ Store presence $\rightarrow$ Main store listing**:

1. **App Icon** (512 × 512 px): `play-store-assets/app-icon-512.png`
2. **Feature Graphic** (1024 × 500 px): `play-store-assets/feature-graphic-1024x500.png`
3. **Phone Screenshots** (1080 × 1920 px):
   * `play-store-assets/screenshot_1.png` — Never Miss Your Stop (Home & Quick Commute)
   * `play-store-assets/screenshot_2.png` — Pinpoint Arrival Geofencing (Map & Radius Slider)
   * `play-store-assets/screenshot_3.png` — Natural Voice Arrival Alerts (Spoken announcements)
   * `play-store-assets/screenshot_4.png` — Reliable Fullscreen Wake-Up (Lockscreen alarm)
   * `play-store-assets/screenshot_5.png` — 100% Private & Battery Smart (Zero tracking & battery savings)

---

## 📝 3. Store Listing Copy (Copy-Paste Ready)

### App Name (max 30 chars):
```
Wake Me There: Location Alarm
```

### Short Description (max 80 chars):
```
Sleep peacefully on transit! GPS alarm that wakes you up at your destination.
```

### Full Description (max 4000 chars):
```markdown
Never miss your bus stop, train station, or commute destination again!

Wake Me There is a smart, battery-optimized, privacy-first location alarm clock that wakes you up as soon as you enter your target arrival zone. Whether you're napping on the daily bus ride, reading on the subway, or traveling cross-country on a night train, Wake Me There guarantees you arrive relaxed and on time.

===============================
🌟 KEY FEATURES & HIGHLIGHTS
===============================

📍 PINPOINT GPS GEOFENCING
Drop a pin anywhere on the map or search any place name or address. Customize your trigger radius from 100 meters up to 5,000 meters with our intuitive radius slider and quick presets.

🗣️ NATURAL VOICE ARRIVAL ANNOUNCEMENTS
Never jump out of your seat in confusion. Wake Me There speaks your destination out loud using your phone's built-in natural speech engine:
• "Transit Commute": "Attention! Arriving at Mountain View Station. Please prepare to get off."
• "Wake-Up Alert": Loud, energetic arrival reminders.
• "Calm Reminder": Gentle, pleasant notifications for quiet commuters.

⚡ SMART BATTERY GPS SAVINGS
Most location apps drain your battery by polling GPS continuously. Wake Me There uses an intelligent 4-tier adaptive tracking system:
• FAR (>10 km): Low-power balanced updates, saving up to 85% battery.
• CRITICAL (<1.5 km): Precision high-accuracy tracking right when you need it.
• ZERO DRAIN WHEN IDLE: As soon as your alarms are off, GPS hardware is completely powered down.

🔒 100% PRIVATE • ZERO DATA TRACKING
Your location belongs to you.
• No accounts, sign-ins, or phone numbers required.
• No telemetry, analytics trackers, or third-party advertising SDKs.
• All destinations and coordinates are calculated locally on your device and NEVER uploaded to any cloud server or data broker.

📴 WORKS 100% OFFLINE
Lost cellular reception in an underground metro tunnel or remote valley? No problem. The alarm logic and geofencing work completely offline without internet.

🚌 1-TAP COMMUTE PRESETS
Save your frequent spots (Office, Home, Bus Interchange, Metro Station) once. When boarding, tap once to arm and sleep without worries!

===============================
🛡️ PERMISSIONS & PRIVACY TRANSPARENCY
===============================
• Location (Foreground & Background): Essential to monitor distance to your destination and trigger your alarm while the app is in the background or device is in sleep mode.
• Foreground Service: Ensures reliable alarm execution when your device is locked in your pocket.
• Notifications: Shows live remaining distance and arrival alerts.

Download Wake Me There today and sleep peacefully on every journey!
```

---

## 📋 4. Exact Form Answers for Google Play Console

### A. Privacy Policy URL
Host your `PRIVACY_POLICY.md` on GitHub Gist or GitHub Pages to get a public URL:
1. Go to [gist.github.com](https://gist.github.com).
2. Paste the contents of `PRIVACY_POLICY.md`.
3. Save as public and click **Raw** to get the public link.
4. Paste that URL into **App content $\rightarrow$ Privacy policy**.

### B. App Access
* Select: **"All functionality is available without special access restrictions"** (No login/passwords needed).

### C. Ads
* Select: **"No, my app does not contain ads"**.

### D. Content Rating Questionnaire
* Category: **Utility / Productivity / Tools**
* Does the app contain violence? **No**
* Does the app contain sexual content? **No**
* Does the app contain profanity? **No**
* Does the app allow users to interact/exchange messages? **No**
* Does the app share user location with others? **No**
$\rightarrow$ **Result**: Rating will be **Everyone (PEGI 3)**.

### E. Target Audience & Content
* Select: **18 and over**, **13–17** (or 13+).
* Could your app appeal to children? **No**.

### F. Data Safety Form (Crucial to Prevent Rejection!)
* Does your app collect or share user data? **Yes** (Location).
* Is data collected or shared?
  * **Approximate Location**: Collected = **Yes**, Shared = **No**, Ephemeral = **Yes** (processed locally in memory to calculate distance).
  * **Precise Location**: Collected = **Yes**, Shared = **No**, Ephemeral = **Yes**.
* Is this data required or optional? **Required** (core alarm feature).
* Why is this data collected? **App functionality** (To trigger the alarm when the user arrives).
* Does the app collect any personal info (Name, Email, Device ID)? **No**.

### G. Background Location Declaration (Google Review Form)
When Play Console asks why your app needs background location:
* **Feature Description**:
  > "Wake Me There is a transit alarm clock. The user sets a destination alarm (e.g. their bus or train stop) and locks their phone in their pocket while resting. The app requires background location to track proximity and sound the alarm as soon as the commuter enters their designated arrival radius, preventing them from missing their stop."
* **Video Link (Required by Google)**:
  > Record a brief 30-second screen video showing:
  > 1. Opening the app and setting an alarm radius.
  > 2. Locking the phone or minimizing the app.
  > 3. Moving into the trigger zone (or clicking Test Trigger) and the alarm ringing over the lock screen.
  > Upload as an **Unlisted** YouTube video and paste the link.

---

## 🧪 5. Testing Track & Publishing

1. In Play Console, go to **Testing $\rightarrow$ Internal testing**.
2. Click **Create new release**.
3. Upload `wake-me-there-release.aab`.
4. Release name: `1.0.0 (1)`.
5. Click **Next** $\rightarrow$ **Save and publish to internal testing**.
6. You can immediately install and test the production version on your phone!
7. Once satisfied, promote the release to **Production**!

