# Privacy Policy for Wake Me There

**Last Updated:** November 6, 2026

**Wake Me There** ("we", "our", or "the App") is developed with a strict **Privacy-First and Offline-First** philosophy. We strongly believe that your personal data and physical location should belong exclusively to you. 

This Privacy Policy explains how Wake Me There handles information on your device.

---

### 1. Zero Personal Data Collection & Zero Third-Party Tracking

* **No Accounts Required**: You do not need to register an account, sign in, or provide your name, email address, or phone number to use Wake Me There.
* **No Telemetry or Analytics**: The App does not contain third-party analytics SDKs, advertising trackers, or tracking cookies.
* **No External Servers**: Your alarms, destinations, coordinates, and preferences are stored exclusively in a local Room database on your device and are never sent to external servers or cloud services.
* **We Never Sell or Monetize Your Data**: We do not sell, rent, monetize, or share your data with advertisers, data brokers, or third parties under any circumstances.

---

### 2. Location Data Usage

Wake Me There is a location-based transit and travel alarm application. In order to function, the App requires access to your device's location.

#### A. Foreground Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
* **Purpose**: Used when you open the map to view your current position, search for destinations, and configure alarm trigger zones.
* **Handling**: Coordinates are processed in real-time on your device to display distances and are not uploaded anywhere.

#### B. Background Location (`ACCESS_BACKGROUND_LOCATION`) & Foreground Services (`FOREGROUND_SERVICE_LOCATION`)
* **Purpose**: To calculate your proximity to your chosen destination and ring your alarm on time when:
  * You lock your device and put it in your pocket.
  * You switch to other apps while commuting on a bus, train, or car.
  * The device screen is asleep.
* **Battery-Optimized Operation**:
  * **Zero Polling When Inactive**: Location tracking runs **only** when at least one alarm is actively armed. If all alarms are turned off or deleted, GPS hardware is completely powered down.
  * **Adaptive Proximity Tiers**: Polling frequency automatically slows down when you are far away (>10 km) to conserve up to 85% battery, and switches to high precision only when entering your arrival zone (<1.5 km).

---

### 3. Audio & Voice Announcements

* **Local Text-to-Speech (TTS)**: Voice arrival announcements use your Android device's built-in local Text-to-Speech engine. No voice audio is recorded, stored, or transmitted over the internet.
* **Alarm Sounds**: Ringtone audio is played directly from your device's built-in alarm library using standard Android media frameworks.

---

### 4. Data Storage and Deletion

* All alarms, saved presets, and custom destination settings are stored locally on your device using Android's encrypted private application storage.
* You can modify or permanently delete any alarm at any time within the App.
* Uninstalling the App or clearing App Data in your Android Device Settings instantly and permanently deletes all stored data.

---

### 5. Third-Party Services

* **Google Play Services (Google Maps)**: The App utilizes the official Google Play Services Maps SDK to render map tiles. Map tile requests are handled directly by Google Play Services in accordance with Google's standard Privacy Policy (https://policies.google.com/privacy).

---

### 6. Children's Privacy

Wake Me There does not collect personal information from anyone, including children under the age of 13.

---

### 7. Changes to This Privacy Policy

If we update this Privacy Policy, the updated version will be posted here with a revised "Last Updated" date.

---

### 8. Contact Us

If you have any questions or feedback regarding this Privacy Policy, please contact:
* **Developer**: Niraj Choudhary
* **Email**: niraj.choudhary1995@gmail.com
* **Project Repository**: https://github.com/nkchoudhary2000/wake-me-there

