# Build the APK yourself (works on any x86_64 Windows/Linux/macOS PC)

## Option A — Android Studio (easiest)
1. Open this folder in Android Studio.
2. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. APK appears at: `app/build/outputs/apk/debug/app-debug.apk`.

## Option B — Command line
Requirements: JDK 17, Android SDK platform 35, build-tools 35.0.0, Gradle 8.9.

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
gradle assembleDebug --no-daemon
```

## Upload the APK to GoFile manually
```bash
curl -X POST https://upload.gofile.io/uploadfile \
  -F "file=@app/build/outputs/apk/debug/app-debug.apk"
```
The JSON response contains `data.downloadPage` — that is your share link.

## Automatic build + GoFile upload
Push this project to a GitHub repository and run the workflow:
`.github/workflows/build-and-upload.yml`

It builds on GitHub's free runner, uploads to GoFile, and prints the link in the workflow summary.
