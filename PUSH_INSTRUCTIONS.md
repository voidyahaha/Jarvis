# Push to GitHub and get the GoFile APK link

Target repository: **https://github.com/voidyahaha/Jarvis.git**
Default branch: **main** (empty repo).

The workflow `.github/workflows/build-and-upload.yml` already triggers on a push to `main`, so pushing is enough.

## On a PC with Git installed

```bash
cd /path/to/AetherAssistant
git init
git remote add origin https://github.com/voidyahaha/Jarvis.git
git add .
git commit -m "Aether Assistant source"
git branch -M main
git push -u origin main
```

If GitHub asks for authentication, use a **Personal Access Token** as the password:

```bash
git remote set-url origin https://YOUR_TOKEN@github.com/voidyahaha/Jarvis.git
git push -u origin main
git remote set-url origin https://github.com/voidyahaha/Jarvis.git
```

> Never commit the token into a file.

## After pushing

1. Open https://github.com/voidyahaha/Jarvis/actions
2. Wait for **“Build Aether APK and upload to GoFile”** to finish.
3. Open that run → the **Summary** tab shows the GoFile download link.
4. The APK is also attached as an artifact named `aether-debug-apk`.

If the workflow does not start automatically, click **Run workflow** manually.

## Note

This chat environment has no `git`, no GitHub credentials, and cannot run Android build tools, so the push/build must be done from a machine that has Git or from GitHub Actions after the push.
