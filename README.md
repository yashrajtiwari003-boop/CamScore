# CamScore by Fischer Labs
## Cross-Platform (Android & iOS) On-Device 10-Second Camera Hardware Diagnostic Benchmark & Global Leaderboard

---

### 🌐 Live Web App & Cloud Verification Portal (Zero Install Required)
👉 **Launch Live Website:** [https://yashrajtiwari003-boop.github.io/CamScore/](https://yashrajtiwari003-boop.github.io/CamScore/)  
👉 **Official Certificate Verification Portal:** [https://yashrajtiwari003-boop.github.io/CamScore/verify.html](https://yashrajtiwari003-boop.github.io/CamScore/verify.html)

- **Instant Browser Access**: Open directly in Chrome, Safari, Firefox, or Edge on any phone, tablet, or PC without installing anything.
- **Figure 3.1 Dual-Theme System**: Switch between **Deep Midnight Navy Dark Theme** (`#0A192F` / `#112240` / `#29B6F6`) and **Clean Clinical Daylight White Theme** (`#F4F7FB` / `#FFFFFF` / `#0D233A`) instantly.
- **Figure 3.1 6-Screen Architecture Showcase**: Interactive phone emulator + side-by-side architecture showcase view.
- **Live 10-Second Automated Camera Benchmark**:
  - **Phase 1: AF Speed (0.0s–3.5s)**: Laplacian variance ($\sigma^2$), milliseconds to 95% peak ($\Delta t_{AF}$).
  - **Phase 2: OIS Stabilization (3.5s–7.0s)**: 100Hz gyroscope ($\omega_{gyro}$) vs. 20-corner Lucas-Kanade optical flow ($d_{px}$) dampening ratio.
  - **Phase 3: Multi-Lens Switching (7.0s–10.0s)**: 1x to 0.6x (Ultrawide) and 3x/5x (Telephoto), CIE LAB color shift $\Delta E$ and switch latency.
- **Non-OIS Fair Mode**: Automatic hardware capability check redistributes weights ($60\%\text{ AF} + 40\%\text{ Lens Switch} = 100\%$) so healthy budget phones still achieve 95%–100% Mint Health!
- **Digital Health Certificate**: View tamper-proof hardware certificates, scan anti-fraud QR codes, and export as PNG or vector PDF.
- **Monthly Global Mobile Ranking Leaderboard**: Rolling monthly season rankings ($YYYY-MM$) with filters for "All Phones" vs. "Non-OIS / Budget Phones".
- **Offline Test History & Trends**: Persistent local history with canvas health retention sparkline chart.

---

### 1. Executive Summary
**CamScore** is the world's first cross-platform (Android & iOS), on-device, zero-lab-equipment camera hardware diagnostic benchmark and anti-fraud certification system. Built to solve three major market gaps:
1. **No Consumer Benchmark or Leaderboard**: DXOMARK only tests factory loaners in $50,000 labs; CamScore gives camera enthusiasts an uncapped score to chase and compare.
2. **Degraded Hardware Over Time**: Motorcycle handlebar vibrations shatter OIS springs and drops misalign focus motors without OS error popups.
3. **Used Phone Fraud**: Second-hand buyers and sellers (OLX, WhatsApp, Cashify) can verify physical camera condition via tamper-proof QR certificates.

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
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/model/`: 7 domain data models (`DeviceInfo`, `AutofocusResult`, `OisResult`, `LensSwitchResult`, `Certificate`, `LeaderboardEntry`, `DiagnosticReport`)
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/scoring/`: `ScoringEngine.kt` (Mathematical continuous interpolation curves, custom pure-Kotlin SHA-256)
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/statemachine/`: `DiagnosticStateMachine.kt` (Strict 10.0s automated state timeline)
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/repository/`: `TestRepository.kt`, `LeaderboardRepository.kt`, `CertificateRepository.kt`
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/viewmodel/`: Reactive StateFlow ViewModels
  - `shared/src/commonMain/kotlin/com/fischerlabs/camscore/ui/`: Material 3 screens, dual theme, Figure 3.1 components
- **Android Target**:
  - Jetpack CameraX + Camera2 Interop (`CameraController.android.kt`)
  - SensorManager 100Hz Gyroscope & Accelerometer (`SensorController.android.kt`)
  - OpenCV Android SDK (`OpenCVProcessor.android.kt`)
  - Android Canvas / PDF renderer (`CertificateExporter.android.kt`)
  - Entry point: `androidApp/src/main/kotlin/.../MainActivity.kt`
- **iOS Target**:
  - Apple AVFoundation (`CameraController.ios.kt`)
  - CoreMotion CMMotionManager 100Hz (`SensorController.ios.kt`)
  - Classical CV Laplacian & Lucas-Kanade optical flow (`OpenCVProcessor.ios.kt`)
  - Native SQLite driver (`DatabaseDriverFactory.ios.kt`)
  - Entry point: `iosApp/iosApp/iOSApp.swift` & `MainViewController.kt`
- **Web & Cloud Companion**:
  - High-fidelity responsive web application (`web/index.html` & `web/app.html`)
  - Cloud QR Anti-Fraud Verification Portal (`web/verify.html`)
  - Automated continuous deployment via GitHub Pages (`.github/workflows/deploy.yml`)

---

### 3. The 10-Second Diagnostic Pipeline

| Phase | Time Range | Hardware Tested | Algorithm | Health Weight (w/ OIS) | Health Weight (Non-OIS) |
|---|---|---|---|---|---|
| **Phase 1: AF Speed** | 0.0s – 3.5s | VCM Focus Coil | Grayscale Laplacian variance $\sigma^2$ until 95% peak | **35%** | **60%** (or 100% single lens) |
| **Phase 2: OIS Stability** | 3.5s – 7.0s | OIS Gyro Motors | 20-corner Lucas-Kanade $d_{px}$ vs. 100Hz $\omega_{gyro}$ | **40%** | **0% (Excluded)** |
| **Phase 3: Lens Switch** | 7.0s – 10.0s | Secondary Lenses | CIE LAB color Euclidean distance $\Delta E$ + switch lag | **25%** | **40%** |

---

### 4. How Non-OIS Fair Mode Operates
1. **Pre-Test Automatic Detection**: At $T = 0.0s$, the OS is queried via `CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` (Android) or `activeFormat.isOpticalImageStabilizationSupported` (iOS).
2. **Scan HUD Banner**: During Phase 2, a non-OIS device displays:  
   `"ℹ️ Hardware Note: No physical OIS motor exists on this device model. Running Digital Sensor & Frame Stability Check (No OIS Penalty Applied)."`
3. **Redistributed Formula (Zero Penalty)**:
   - Phone with OIS: $\text{Health} = 0.35 \times H_{AF} + 0.40 \times H_{OIS} + 0.25 \times H_{Lens}$
   - Multi-lens without OIS: $\text{Health} = 0.60 \times H_{AF} + 0.40 \times H_{Lens}$ (100% total)
   - Single-lens without OIS: $\text{Health} = 1.00 \times H_{AF}$ (100% total)
4. **Certificate Badge**: `"✔ VERIFIED HEALTHY (NON-OIS MODEL) — Evaluated via Fair Non-OIS Diagnostic Protocol."`

---

### 5. Screens Implemented (Figure 3.1 Architecture)

1. **Screen 01 — Home & Ambient Light Guard**: Live camera preview, 3-column readiness badges (Lighting, Texture, Readiness), OIS capability pill, large Start button, low-light alert dialog, monthly leaderboard shortcut.
2. **Screen 02 — 10-Second Live Testing Scanner**: Animated circular countdown ring (0.0s–10.0s), 3-row diagnostic phase checklist with progress bars, live 100Hz sensor-fusion waveform graph, non-OIS banner.
3. **Screen 03 — Results Breakdown**: Animated Uncapped CamScore counter, circular 0%–100% health gauge (PASS / MINOR WEAR / FAIL), 3 horizontal sub-score cards, monthly rank comparison, one-tap export.
4. **Screen 04 — Hardware Health & Flex Certificate**: Royal blue header, device thumbnail, test ID, CamScore, 0%–100% health stamp, scan-ready anti-fraud QR code, PNG/PDF export buttons.
5. **Monthly Global Mobile Ranking Screen**: Monthly leaderboard with rollover ($YYYY-MM$), filters ("All Phones" vs. "Non-OIS / Budget"), rank cards with Gold/Silver/Bronze badges.
6. **Test History Screen**: Canvas-drawn long-term health degradation chart, offline SQLite test history logs, test deletion.
7. **Onboarding Tutorial**: 3-slide animated beginner walkthrough explaining hardware diagnostic, health percentage monitoring, and anti-fraud certificates.

---

### 6. Build & Deployment Instructions

#### Android Native Build
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

#### iOS Native Build
```bash
cd CamScore
# Build shared iOS framework
./gradlew :shared:assembleSharedXCFramework

# Open iOS project in Xcode
open iosApp/iosApp.xcodeproj
```

#### Web & Cloud Deployment
The repository includes a continuous deployment workflow in `.github/workflows/deploy.yml` that automatically packages and deploys `./web` to GitHub Pages upon pushing to `main`.
```bash
# Push updates to trigger automated GitHub Pages build
git add .
git commit -m "feat: complete Figure 3.1 6-screen architecture and live web diagnostic app"
git push origin main
```
Live URL: **https://yashrajtiwari003-boop.github.io/CamScore/**
