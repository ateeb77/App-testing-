# JAGRUK
### "Know danger. Nearby. In time."

Jagruk is a citizen-powered, AI-assisted hyperlocal emergency intelligence and early-warning platform. It empowers citizens to report hazardous situations such as flash floods, fires, dangerous wildlife, and structural hazards. Incoming reports are geolocated, corroborated, evaluated by AI and rule-based verification, framed into dynamic danger zones, and immediately broadcast over the internet to residents inside the geofence perimeter.

---

## 🏛️ Public Safety & Government Integration Notice

> **"Jagruk is designed to complement official emergency-warning infrastructure such as India's SACHET and emergency response number 112. It focuses on community-level, hyperlocal hazard reporting and early awareness."**

Jagruk does **NOT** claim to replace official emergency services (112, NDMA, SACHET, Police, Fire, Ambulance). Instead, it serves as a community-level early warning and rapid reporting layer.

---

## 📱 Two-Phone Live Hackathon Demonstration

The core of Jagruk is a real-time internet link connecting two physical smartphones:

- **Phone A (Reporter)**:
  - Role: `REPORTER`
  - Location: Demo Location A (`21.1458, 79.0882`)
  - Submits emergency incident (e.g., Flash Flood, 1000m radius)
- **Phone B (Resident)**:
  - Role: `RESIDENT`
  - Location: Demo Location B (`21.1480, 79.0900`, ~420m away — **Inside Danger Zone**)
  - Receives real-time alert, full-screen Critical Alert display, 3-tone emergency chime, and haptic vibration.
- **Admin Command Center**:
  - Monitors live feed, verifies reports, modifies danger radius, escalates to 112, or resolves incidents.

Both phones communicate over mobile data or Wi-Fi through the real-time internet mesh (`ntfy.sh` WebSocket/HTTP sync broker).

---

## 🛠️ Technology Stack & Architecture

- **Platform**: Native Android (Kotlin & Jetpack Compose)
- **Design System**: Material Design 3 (Government/Enterprise grade, deep navy `#0B192C` and alert accents)
- **Geofencing**: Haversine distance engine, automated radius detection, dynamic zone circles
- **Real-Time Sync**: Internet WebSocket & HTTP mesh channel with fallback polling
- **Local Persistence**: Android Room Database (offline-first, cache, audit logs)
- **AI Verification**: Google Gemini 3.5 Flash (`gemini-3.5-flash`) REST integration via Retrofit + 100% resilient rule-based verification fallback
- **Alert Audio**: Pure PCM 3-tone emergency chime synthesized via `AudioTrack` (880Hz, 1174Hz, 1568Hz) + `Vibrator` pattern
- **GIS Canvas**: High-performance interactive vector radar map with pinch-to-zoom and pan gestures

---

## 🚀 Live Demo Scenario: "FLASH FLOOD — LIVE DEMO"

1. Open Jagruk on **Phone A** and select **Reporter** in the profile/demo bar.
2. Open Jagruk on **Phone B** and select **Resident** in the profile/demo bar.
3. Tap **"🌊 Trigger 'FLASH FLOOD — LIVE DEMO' Scenario"** on Phone A.
4. **Phone B immediately responds**:
   - High-contrast **Critical Alert Screen** pops up.
   - Distinctive **3-tone emergency alert chime** plays.
   - Device **vibrates** with emergency cadence.
   - Resident can tap **"VIEW LIVE MAP"** to inspect the 1000m danger zone or tap **"ACKNOWLEDGE"**.
5. Switch to **Admin Command Center**:
   - Verify report (`Confidence: VERIFIED`).
   - Adjust danger radius with the slider.
   - Tap **"Reset Demo"** to restore both phones to ready state.

---

## 🔑 Environment Variables & Secrets

Configure your Gemini API key in `.env`:
```env
GEMINI_API_KEY=your_gemini_api_key_here
```
If `GEMINI_API_KEY` is not supplied or network drops, Jagruk automatically switches to its deterministic rule-based verification engine without disruption.

---

## 🛡️ Accidental Report Protection

To protect against accidental taps, prank reports, and duplicate reports:
- All **HIGH** and **CRITICAL** severity hazards require a **1.5-second Press-and-Hold** confirmation button before broadcasting.
- Users can flag any report with **"I reported this by mistake"**, immediately placing the incident into **UNDER REVIEW** status for administrators.
