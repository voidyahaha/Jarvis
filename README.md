# Aether Assistant — Android AI / Automation App

A **real, local-first Android application** built from the specification you gave: a futuristic, Jarvis-inspired assistant that can talk, see, read files, remember, automate apps, generate code, and prepare supported online actions — while keeping privacy, zero cost, offline operation and explicit consent as hard requirements.

> This is a source project, not a mockup. Every screen is wired to a real class in `core/`. Where a capability cannot honestly be done 100% on-device or without user consent, the app says so instead of faking it.

---

## 1. What the app actually does

### 🧠 Core assistant
- **Real-time voice conversation** using Android `SpeechRecognizer` + `TextToSpeech`.
  - Interrupt support (`interrupt()` cancels TTS and listening).
  - Hands-free toggle.
  - Prefers on-device recognition (`EXTRA_PREFER_OFFLINE`); no paid voice API.
- **Two AI modes, always visible** in the top bar:
  - 🟢 **Offline AI** — on-device intent engine + optional local model server at `127.0.0.1:8080`.
  - 🔵 **Online AI** — any user-configured OpenAI-compatible endpoint. Nothing is uploaded until you switch modes.
- **Conversation screen** with local chat history and spoken replies.

### 👁️ Screen intelligence
- Optional `AccessibilityService` reads the current UI tree (text, content descriptions, resource ids).
- Agent can **click by text/id, type into focused fields, scroll, go back/home**.
- Optional `MediaProjection` foreground service for screen capture + OCR.
- Requires explicit user enablement in Android Accessibility settings.

### 📷 Camera vision
- CameraX preview + on-device ML Kit:
  - Text recognition (OCR)
  - Barcode / QR scanning
  - Image labeling
- Visible **CAMERA ACTIVE** indicator. No frame is stored or uploaded automatically.
- UPI QR codes are parsed locally and shown to you — **no payment is made by the app**.

### 🤖 Autonomous agent
Strict architecture, visible in the UI as a task progress overlay:

```
Planner → Executor → Verifier → Error Handler
```

- Plans multi-step tasks from one sentence.
- Verifies each step.
- Retries / falls back (e.g., Online AI failure → Offline engine).
- **Confirmation dialog** for sensitive, financial or state-changing actions.
- Reports every step and the final result.

### 📱 Android automation
- Open installed apps by name (Intents).
- Search/tap/type/scroll through Accessibility.
- Alarms/timers via official `AlarmClock` intents.
- Settings, web links, media-style actions where Android permits.
- **No root, no exploits, no permission bypass.**

### 🌐 Web
- Free DuckDuckGo HTML search (no paid search API).
- Read/summarize arbitrary http(s) pages through AI.
- Clear online labeling when you trigger web actions.

### 📁 File analysis
Handles PDFs, images, audio/video metadata, ZIP listings, CSV and source code.
- Local OCR for images and PDFs.
- Progress indicators for large files.
- Private files are never uploaded unless you explicitly allow online analysis.

### 🧠 Memory
- Room database, local only.
- Sensitive entries encrypted with AES-256-GCM using a key in the **Android Keystore**.
- View, search, reveal, delete individual, and **Delete All Memory**.

### 🔐 Privacy & permission dashboards
- Live status for Microphone, Camera, Screen capture, Accessibility, Notifications, Files, Network.
- Toggles for screen intelligence, notifications, online vision/files, confirmation, TTS.
- No telemetry, ads, tracking SDKs, hidden analytics, or secret recording.

### 📱 Apps + 💻 Code Lab (the “floating cards” UI)
- **App Vault**: floating-style list of installed apps with real launch buttons.
- **Code Lab**: generate original project skeletons and source files from plain language (static web, React, Flask, Android Compose). Files are written to the app's local `generated/` directory.

### 🎬 YouTube upload flow
- Generate title/description from your instruction.
- Uses the **official YouTube Data API** with a user-supplied OAuth token.
- Supports `publishAt` scheduling.
- No credentials are bundled; you paste a token in Settings and approve the upload.

### 💸 Payment flow boundary
- Opens PhonePe and attempts to tap the scanner via Accessibility.
- Reads UPI QR locally (`upi://pay?...`).
- Shows payee / UPI id / amount.
- **Stops there.** You confirm and enter your own UPI PIN inside PhonePe. Aether never stores UPI PINs and never authorizes a payment.

---

## 2. Honest limitations (so nothing is fake)

| Requested idea | What is implemented | Why / boundary |
|---|---|---|
| Fully offline LLM chat | Offline intent engine + optional local HTTP model endpoint | Bundling a multi-GB model is unrealistic on arbitrary devices; the app recommends/configures a smaller local model instead of pretending |
| Offline video/audio transcription | Metadata + “requires local speech model” message | Whisper-size models are too large for many phones |
| Clone any website/app “exactly” | Original re-implementation from scratch | Copyright/trademark/ToS limits; no proprietary assets or impersonation |
| Auto-pay in PhonePe | Open scanner + parse QR + prepare details | Financial actions require your explicit confirmation and UPI PIN |
| Auto-follow on Instagram | Planner step with confirmation, Accessibility click | Third-party UI changes and platform ToS make unattended automation unreliable |
| Upload to YouTube | Official API + OAuth token + scheduling | Token is user-provided; API quota/ToS apply |
| “Do anything” | Official APIs + permitted Accessibility actions | Android security model forbids unrestricted automation |

---

## 3. Project structure

```
AetherAssistant/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── xml/accessibility_service_config.xml
        │   ├── xml/backup_rules.xml
        │   ├── xml/data_extraction_rules.xml
        │   ├── xml/file_paths.xml
        │   ├── values/strings.xml, styles.xml, ic_launcher_background.xml
        │   └── drawable/mipmap launch icons
        └── java/com/aether/assistant/
            ├── AetherApplication.kt          # container + app init
            ├── MainActivity.kt                # Compose nav + status + confirmation
            ├── core/
            │   ├── ai/                        # AiEngine, LocalEngine, OnlineEngine, AiManager
            │   ├── agent/                     # Actions, Planner, Executor, Verifier, ErrorHandler, Agent
            │   ├── automation/                # Accessibility, ScreenCapture, Notifications, Automation, PaymentFlow
            │   ├── voice/                     # STT/TTS controller
            │   ├── vision/                    # CameraX + ML Kit
            │   ├── files/                     # local file analysis
            │   ├── memory/                    # Room + Keystore CryptoBox
            │   ├── settings/                  # DataStore + encrypted tokens
            │   ├── permissions/               # permission dashboard state
            │   ├── web/                       # free search + page read
            │   ├── code/                      # project generator
            │   └── youtube/                   # upload + scheduling
            └── ui/
                ├── theme/                     # HUD colors/typography
                ├── components/                # visualizer, status, progress, confirmation
                └── screens/                   # home, chat, vision, files, code, apps, memory, privacy, settings
```

---

## 4. Build & run

1. Open Android Studio (Ladybug or newer) and **Open** the `AetherAssistant` folder.
2. Let Gradle sync (Kotlin 1.9.24, Compose BOM 2024.10, compileSdk 35).
3. Run on a device or emulator with **API 26+**.
4. Grant permissions from the in-app Privacy dashboard / first-use prompts.

No API keys are required. To use Online AI or YouTube, supply your own endpoint/token in Settings.

---

## 5. Priority order used

1. 🔐 Privacy & security
2. 💰 Zero cost
3. 📴 Offline functionality
4. 🌐 Optional online AI
5. 🤖 Real automation
6. ⚡ Reliability
7. 🎨 Professional UI

---

## 6. Key design decisions

- **Manual DI container** (`AppContainer`) keeps everything inspectable and local.
- **Local-first storage**: DataStore for settings, Room for memories, Keystore AES-GCM for sensitive fields.
- **No hard-coded secrets**: endpoint token and YouTube token are user-provided and encrypted/session-scoped.
- **Every action is observable**: task plan, per-step status, errors and confirmations are StateFlows rendered by Compose.
- **Fail loudly, never freeze**: network/model/permission failures produce clear messages and safe fallbacks.
