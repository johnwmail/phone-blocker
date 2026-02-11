# 📵 Phone Blocker

A simple Android app to block unwanted calls using pattern matching with wildcards.

## Features

- **Pattern Matching** - Use `*` as wildcard for any digit
  - `3456****` blocks all 8-digit numbers starting with 3456
  - `1800******` blocks 10-digit numbers starting with 1800
  - `**********` blocks all 10-digit numbers
- **Three Actions:**
  - 🚫 **Block** - Reject the call completely
  - 🔇 **Silence** - Silent rejection
  - 📞 **Voicemail** - Send to voicemail
- **Enable/Disable** - Toggle individual rules on/off
- **Simple UI** - Easy to add, manage, and delete rules

## Requirements

- Android 10+ (API 29+)
- Permissions: Phone, Call Logs

## Installation

### From APK

1. Download `PhoneBlocker.apk` to your Android phone
2. Enable "Install from unknown sources" in Settings
3. Open the APK file to install
4. Launch the app and grant permissions when prompted

### Setup Permissions

After installing, you need to grant permissions:

1. **Settings → Apps → Phone Blocker → Permissions**
   - Enable: **Phone** (电话)
   - Enable: **Call Logs** (通话记录)

2. **Set as Call Screening App** (if prompted):
   - Settings → Apps → Default Apps → Caller ID & Spam
   - Select: Phone Blocker

> **Note:** Some phones (especially Chinese ROMs like MIUI, ColorOS, etc.) may not show the Call Screening option. The app includes a fallback method that works on most devices.

## Usage

1. **Add a Rule:** Tap the **+** button
2. **Enter Pattern:** Use digits and `*` wildcards
   - Example: `3456****` matches 34560000 through 34569999
3. **Select Action:** Block, Silence, or Voicemail
4. **Tap Add**

### Pattern Examples

| Pattern | Matches |
|---------|--------|
| `3456****` | 8-digit numbers starting with 3456 |
| `1800******` | 10-digit numbers starting with 1800 |
| `****1234` | 8-digit numbers ending with 1234 |
| `98765*****` | 10-digit numbers starting with 98765 |

## Building from Source

### Prerequisites

- JDK 17
- Android SDK (API 34)
- Gradle 8.2+

### Linux

```bash
# Install JDK 17
sudo apt install openjdk-17-jdk

# Set up Android SDK
mkdir -p ~/Android/cmdline-tools
cd ~/Android/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# Set environment variables
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=~/Android
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Install SDK components
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Clone and build
git clone <repository-url>
cd phone-blocker
gradle assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Windows

1. Install [JDK 17](https://adoptium.net/temurin/releases/?version=17)
2. Install [Android Studio](https://developer.android.com/studio) (includes SDK)
3. Open project in Android Studio
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

## Project Structure

```
phone-blocker/
├── app/
│   ├── src/main/
│   │   ├── java/com/phoneblocker/
│   │   │   ├── MainActivity.kt      # Main UI
│   │   │   ├── BlockRule.kt         # Rule data model
│   │   │   ├── RuleStorage.kt       # Save/load rules
│   │   │   ├── RuleAdapter.kt       # RecyclerView adapter
│   │   │   ├── CallBlockerService.kt # CallScreeningService (Android 10+)
│   │   │   └── CallReceiver.kt      # BroadcastReceiver fallback
│   │   ├── res/
│   │   │   ├── layout/              # UI layouts
│   │   │   ├── values/              # Colors, strings, themes
│   │   │   └── drawable/            # Icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## How It Works

The app uses two methods to block calls:

1. **CallScreeningService** (Android 10+)
   - Official Android API for call screening
   - Requires being set as default Call Screening app
   - May not work on all custom ROMs

2. **BroadcastReceiver Fallback**
   - Listens for `PHONE_STATE` broadcasts
   - Uses `TelecomManager.endCall()` to reject calls
   - Works on most phones including custom ROMs

## Troubleshooting

### Calls not being blocked

1. **Check permissions:** Ensure Phone and Call Logs permissions are granted
2. **Check the pattern:** Make sure your pattern matches the incoming number format
3. **Check rule is enabled:** Toggle switch should be ON (blue)
4. **Try full number:** Test with the exact phone number (no wildcards) first

### App not appearing in Default Apps

Some phones hide the Call Screening option. The app's BroadcastReceiver fallback should still work.

### Permission denied errors

Some phones require additional permissions. Go to:
- Settings → Apps → Phone Blocker → Permissions → Grant all available permissions

## License

MIT License - Feel free to use and modify.

## Contributing

Pull requests welcome! Please test on multiple devices if possible.
