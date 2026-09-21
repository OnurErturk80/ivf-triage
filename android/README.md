# IVF Triage — Android app

A native Android wrapper around the triage calculator at the repository root. The
page is copied into the APK at build time, so the app and the web version are
always the same file.

## Design notes

**No `INTERNET` permission.** The calculator loads nothing from the network and
the page tells the reader that nothing is stored and nothing is transmitted.
Leaving the permission out makes that enforceable by Android rather than a claim
in prose — the app cannot open a socket even if a future edit tried to.

**The page is served over `https://`, not `file://`.** `WebViewAssetLoader` maps
the APK's assets onto `https://appassets.androidplatform.net/assets/`, which gives
the page an ordinary secure origin. Under `file://` a WebView applies a stricter,
quirkier set of rules, and the page would not behave the way it does in a browser.

**`sw.js` is deliberately not bundled.** Inside the APK the assets are already
local, so the service worker buys nothing — and its cache could serve a stale page
after an app update. The page's registration call fails harmlessly (it is already
wrapped in `.catch()`).

**Insets are handled natively.** The activity runs edge-to-edge, and the root view
is padded by the status bar, navigation bar, display cutout and IME insets. What
gets padded away shows the dark `--ink` colour, so the system bars read as chrome
around the light page.

**Single source of truth.** `copyWebAssets` in `app/build.gradle.kts` copies
`index.html`, `manifest.json` and the icons from the repository root into the
generated assets directory. Edit the page at the root; never edit a copy here.

## Building

Requires JDK 17 and the Android SDK (Android Studio installs both).

```sh
cd android
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is signed with the local debug key and installs directly:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the `android/` directory in Android Studio and press Run.

### Without a local Android SDK

The `Android` GitHub Actions workflow builds both APKs on every push and uploads
them as run artifacts — download `ivf-triage-debug-apk` from the run summary and
install it on the phone. The workflow also verifies that the page inside the APK
is byte-for-byte identical to `index.html` at the root.

### Release builds

`./gradlew assembleRelease` produces an **unsigned** APK. To install or publish it
you need your own keystore:

```sh
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias ivf-triage
```

Then add a `signingConfigs` block to `app/build.gradle.kts` and point the release
build type at it, keeping the keystore and its passwords out of version control.

## Configuration

| | |
|---|---|
| Application id | `io.github.onurerturk80.ivftriage` |
| `minSdk` | 24 (Android 7.0) |
| `compileSdk` / `targetSdk` | 35 (Android 15) |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.11.1 |
| Language | Java 17 |

Change the application id in `app/build.gradle.kts` (`namespace` and
`applicationId`) before publishing under a different account.
