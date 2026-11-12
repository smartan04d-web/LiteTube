# LiteTube — Android 10+ WebView project

A minimal Android wrapper for the official mobile YouTube website. This is an independent sample, not an official YouTube application.

## Requirements
- Android Studio with Android SDK Platform 35
- JDK 17
- Gradle 8.9 / Android Gradle Plugin 8.7.3
- Android 10 (API 29) or newer device

## Build with Android Studio (easiest)
1. Extract this ZIP.
2. Open Android Studio > Open > select the `LiteTube` folder.
3. Allow Gradle sync and install SDK Platform 35 when prompted.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Find the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer it to your phone and open it. Allow installation from that source when Android prompts you.

## Build from command line
The ZIP contains Gradle build scripts and wrapper properties but no generated Gradle wrapper JAR or executable. With Gradle 8.9 installed, run:

```bash
gradle assembleDebug
```

Alternatively, run `gradle wrapper --gradle-version 8.9` first to generate the wrapper scripts and JAR, then use `./gradlew assembleDebug` (Linux/macOS) or `gradlew.bat assembleDebug` (Windows). Set `ANDROID_HOME` to your installed SDK directory when using CLI tools.

## Release build
Use Android Studio > Build > Generate Signed Bundle / APK to sign a release APK. Do not distribute a debug build as a production release.

## Power-saving choices
- No background service, wake lock, push messaging, or background scheduler.
- Requires a tap before media playback starts.
- Pauses WebView activity when backgrounded; site media behavior may vary.
- Uses standard Android hardware-accelerated WebView (device-dependent).
- No added analytics or advertising libraries.
- Choose 360p or 480p in YouTube's own player settings; this app does not override YouTube's quality controls.
- Keep screen brightness low for greater battery savings.

## Limitations
- Video playback, login, and fullscreen depend on YouTube's current site and Android System WebView implementation; Google may restrict sign-in or certain functions in embedded browsers.
- This app does not bypass ads, DRM, access restrictions, subscriptions, or YouTube policies.
- Playback may still consume substantial battery; lower consumption compared with the official app is not guaranteed.
- The APK is not precompiled in this archive.
