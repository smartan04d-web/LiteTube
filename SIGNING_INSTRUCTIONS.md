# Persistent APK signing (GitHub Actions)

The workflow always builds an APK. Without secrets it builds a debug-signed APK, whose signing key can change between runs. To install updates over an existing app, use the **same keystore and alias** for every build. You cannot update an app signed with a different certificate without uninstalling it (which can erase local app data).

On Linux, create a keystore once (keep it private):

```bash
keytool -genkeypair -v -keystore litetube-release.jks -alias litetube -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 litetube-release.jks > litetube-keystore-base64.txt
```

In GitHub repository **Settings → Secrets and variables → Actions**, add these four secrets:

- `LITETUBE_KEYSTORE_BASE64`: contents of `litetube-keystore-base64.txt`
- `LITETUBE_STORE_PASSWORD`: keystore password
- `LITETUBE_KEY_ALIAS`: `litetube`
- `LITETUBE_KEY_PASSWORD`: key password

Do **not** commit the `.jks` file or base64 text. Back up the keystore securely. Once secrets are configured, Actions creates a signed release APK. If an older installation was signed with another key, uninstall it before installing this one; future builds signed with this same keystore can update in place.

Note: The embedded YouTube WebView cannot guarantee playback of all videos, sign-in, or features that YouTube requires to run in an official client.
