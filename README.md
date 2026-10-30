# CamScore by Fischer Labs
## Cross-Platform (Android & iOS) On-Device 10-Second Camera Hardware Diagnostic Benchmark & Global Leaderboard

---

### 1. Executive Summary
**CamScore** is the world's first cross-platform, on-device, zero-lab-equipment camera hardware diagnostic benchmark. In a single **10-second hands-off test**, CamScore:
1. **Sweeps zoom 1x–10x** across physical lenses (Ultrawide, Main, Telephoto).
2. Computes an **Uncapped CamScore** for global monthly leaderboard bragging rights.
3. Calculates a **0%–100% Hardware Health Monitor** detecting motor wear and optical drop damage.
4. Generates an **Anti-Fraud QR Health Certificate** backed by Firebase Firestore.
5. Provides **Fair Non-OIS Redistribution** so budget phones can still achieve 100% Mint Health.

---

### 2. Architecture & Tech Stack

```
                          ┌───────────────────────────┐
                          │   Compose Multiplatform   │
                          │   (Shared UI / Theme M3)  │
                          └─────────────┬─────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
        ┌───────────────────────┐               ┌───────────────────────┐
        │  Kotlin Multiplatform │               │   Scoring & CV Math   │
        │      Shared Core      │               │   (OpenCV + 100Hz)    │
        └───────────┬───────────┘               └───────────┬───────────┘
                    │                                       │
        ┌───────────┴───────────┐               ┌───────────┴───────────┐
        ▼                       ▼               ▼                       ▼
┌───────────────┐       ┌───────────────┐┌───────────────┐     ┌───────────────┐
│ Android Main  │       │   iOS Main    ││ CameraX / C2  │     │ AVFoundation  │
│ SensorManager │       │  CoreMotion   ││ SQLite Room   │     │ Native SQLite │
└───────────────┘       └───────────────┘└───────────────┘     └───────────────┘
```

- **Shared Core (85% Kotlin Code)**:
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/model/`: 7 domain data models
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/scoring/`: `ScoringEngine.kt`
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/statemachine/`: `DiagnosticStateMachine.kt` (10.0s timeline)
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/repository/`: `TestRepository.kt`, `LeaderboardRepository.kt`, `CertificateRepository.kt`
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/viewmodel/`: 5 reactive StateFlow ViewModels
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/ui/`: Material 3 components, dual theme, 7 complete screens
- **Android Target**:
  - CameraX + Camera2 Interop (`CameraController.android.kt`)
  - SensorManager 100Hz (`SensorController.android.kt`)
  - OpenCV Android SDK (`OpenCVProcessor.android.kt`)
  - Android Canvas / PDF renderer (`CertificateExporter.android.kt`)
  - Entry point: `androidApp/src/main/kotlin/.../MainActivity.kt`
- **iOS Target**:
  - AVFoundation (`CameraController.ios.kt`)
  - CoreMotion CMMotionManager 100Hz (`SensorController.ios.kt`)
  - Classical CV Laplacian & PyrLK (`OpenCVProcessor.ios.kt`)
  - Native SQLite driver (`DatabaseDriverFactory.ios.kt`)
  - Entry point: `iosApp/iosApp/iOSApp.swift` & `MainViewController.kt`

---

### 3. The 10-Second Diagnostic Pipeline

| Phase | Time Range | Hardware Tested | Algorithm | Health Weight (w/ OIS) | Health Weight (Non-OIS) |
|---|---|---|---|---|---|
| **Phase 1: AF Speed** | 0.0s – 3.5s | VCM Focus Coil | Grayscale Laplacian variance $\sigma^2$ until 95% peak | **35%** | **60%** (or 100% single lens) |
| **Phase 2: OIS Stability** | 3.5s – 7.0s | OIS Gyro Motors | 20-corner Lucas-Kanade $d_{px}$ vs. 100Hz $\omega_{gyro}$ | **40%** | **0% (Excluded)** |
| **Phase 3: Lens Switch** | 7.0s – 10.0s | Secondary Lenses | CIE LAB color Euclidean distance $\Delta E$ + switch lag | **25%** | **40%** |

---

### 4. How Non-OIS Fair Mode Operates
1. **Pre-Test Automatic Detection**: At $T = 0.0s$, the OS is queried via `CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` (Android) or `activeFormat.isVideoStabilizationModeSupported` (iOS).
2. **Scan HUD Banner**: During Phase 2, a non-OIS device displays:  
   `"ℹ Hardware Note: No physical OIS motor exists on this device model. Running Digital Sensor & Frame Stability Check (No OIS Penalty Applied)."`
3. **Redistributed Formula**:
   - Phone with OIS: $\text{Health} = 0.35 \times H_{AF} + 0.40 \times H_{OIS} + 0.25 \times H_{Lens}$
   - Multi-lens without OIS: $\text{Health} = 0.60 \times H_{AF} + 0.40 \times H_{Lens}$ (100% total)
   - Single-lens without OIS: $\text{Health} = 1.00 \times H_{AF}$ (100% total)
4. **Certificate Badge**: `"✔ VERIFIED HEALTHY (NON-OIS MODEL) — Evaluated via Fair Non-OIS Diagnostic Protocol."`

---

### 5. Screens Implemented

1. **Screen 01 — Home & Ambient Light Guard**: Live camera preview, 3-column readiness badges (Lighting, Texture, Readiness), OIS capability pill, large Start button, low-light alert dialog, monthly leaderboard shortcut.
2. **Screen 02 — 10-Second Live Testing Scanner**: Animated circular countdown ring, 3-row diagnostic phase checklist with progress bars, live sensor-fusion waveform graph, non-OIS banner.
3. **Screen 03 — Results Breakdown**: Animated Uncapped CamScore counter, circular 0%–100% health gauge (PASS / MINOR WEAR / FAIL), 3 horizontal sub-score cards, monthly rank comparison, one-tap export.
4. **Screen 04 — Hardware Health & Flex Certificate**: Royal blue header, device thumbnail, test ID, CamScore, 0%–100% health stamp, scan-ready anti-fraud QR code, PNG/PDF export buttons.
5. **Monthly Global Mobile Ranking Screen**: Monthly leaderboard with rollover ($YYYY-MM$), filters ("All Phones", "OIS Phones", "Non-OIS / Budget"), rank cards with Gold/Silver/Bronze badges.
6. **Test History Screen**: Canvas-drawn long-term health degradation chart, offline SQLite test history logs, test deletion.
7. **Onboarding Tutorial**: 3-slide animated beginner walkthrough explaining hardware diagnostic, health percentage monitoring, and anti-fraud certificates.

---

### 6. Build & Deployment Instructions

#### Android
```bash
cd CamScore
# Assemble Debug APK
./gradlew :androidApp:assembleDebug

# Assemble Release AAB for Google Play Store
./gradlew :androidApp:bundleRelease
```
Output:
- Debug APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Release AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab`

#### iOS
```bash
cd CamScore
# Build shared iOS framework
./gradlew :shared:assembleSharedXCFramework

# Open iOS project in Xcode
open iosApp/iosApp.xcodeproj
```

#### Web QR Verification Page
Deploy `web/verify.html` to Firebase Hosting or GitHub Pages:
```bash
firebase deploy --only hosting
```
URL pattern: `https://camscore.fischerlabs.com/verify?id=<hash>`
