# 📵 Phone Blocker

A simple Android app to block unwanted calls using pattern matching with wildcards.

**🌐 [Live Preview](https://johnwmail.github.io/phone-blocker/preview.html)** | **📦 [Download APK](https://github.com/johnwmail/phone-blocker/releases/latest/download/PhoneBlocker-release.apk)**

## Features

- **Pattern Matching** - Flexible wildcards for matching phone numbers
  - `*` = match zero or more digits
  - `?` = match exactly one digit
  - `86*` blocks ALL numbers starting with 86
  - `138????????` blocks 11-digit numbers starting with 138
- **Four Actions:**
  - ✅ **Allow** - Whitelist (highest priority, checked first)
  - 🚫 **Block** - Reject the call completely
  - 🔇 **Silence** - Mute ringer, call still visible
  - 📞 **Voicemail** - Decline to voicemail
- **Priority System** - ALLOW rules are always checked first
- **Call Log** - View incoming calls with raw number format
- **Enable/Disable** - Toggle individual rules on/off
- **Simple UI** - Easy to add, manage, and delete rules

## Requirements

- Android 10+ (API 29+)
- Permissions: Phone, Call Logs

## Installation

### From GitHub Releases (Recommended)

1. Download the latest APK from [Releases](https://github.com/johnwmail/phone-blocker/releases/latest)
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

### Adding Rules

1. Tap the **+** button
2. Enter pattern using digits and wildcards (`*` `?`)
3. Select action: Allow, Block, Silence, or Voicemail
4. Tap **Add**

### Pattern Matching

The app strips all non-digit characters from incoming numbers before matching:
- `+8613812345678` becomes `8613812345678`
- `183+23456312` becomes `18323456312`
- `(021) 555-1234` becomes `0215551234`

### Wildcards

| Wildcard | Meaning | Example |
|----------|---------|---------|
| `*` | Zero or more digits | `86*` matches `86`, `861`, `8612345678` |
| `?` | Exactly one digit | `138????????` matches any 11-digit number starting with 138 |

### Pattern Examples

| Pattern | Matches | Use Case |
|---------|---------|----------|
| `86*` | All numbers starting with 86 | Block all China numbers |
| `865*` | Numbers starting with 865 | Allow specific prefix (use with ALLOW) |
| `138????????` | 11-digit numbers starting with 138 | Block specific carrier prefix |
| `1800*` | Numbers starting with 1800 | Block toll-free numbers |
| `????1234` | 8-digit numbers ending with 1234 | Block numbers with specific suffix |
| `*` | ALL numbers | Block everything (use ALLOW rules for exceptions) |

### Priority & Whitelist Example

To block all China numbers EXCEPT a specific prefix:

1. Add rule: `865*` → **Allow** (checked first)
2. Add rule: `86*` → **Block** (checked second)

Result: `8651234567` is allowed, `8612345678` is blocked.

### Call Log

Tap **📋 Log** to view incoming calls:
- Shows raw phone number format
- Shows matched pattern (if any)
- Status: ALLOW, BLOCK, SILENCE, VOICEMAIL, or NOT MATCH

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
git clone https://github.com/johnwmail/phone-blocker.git
cd phone-blocker
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Windows

1. Install [JDK 17](https://adoptium.net/temurin/releases/?version=17)
2. Install [Android Studio](https://developer.android.com/studio) (includes SDK)
3. Open project in Android Studio
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

### Run Tests

```bash
./gradlew testDebugUnitTest
```

## Project Structure

```
phone-blocker/
├── .github/workflows/
│   ├── test.yml                 # CI: test & lint on push/PR
│   └── release.yml              # CI: build & release on tag
├── docs/                        # GitHub Pages
│   ├── index.html               # Download page
│   └── preview.html             # Interactive UI preview
├── app/
│   ├── src/main/
│   │   ├── java/com/phoneblocker/
│   │   │   ├── MainActivity.kt       # Main UI
│   │   │   ├── BlockRule.kt          # Rule data model & matching
│   │   │   ├── RuleStorage.kt        # Save/load rules (with priority)
│   │   │   ├── RuleAdapter.kt        # RecyclerView adapter
│   │   │   ├── CallBlockerService.kt # CallScreeningService (Android 10+)
│   │   │   ├── CallReceiver.kt       # BroadcastReceiver fallback
│   │   │   ├── CallLogActivity.kt    # Call log viewer
│   │   │   └── CallLogStorage.kt     # Call log storage
│   │   ├── res/
│   │   │   ├── layout/               # UI layouts
│   │   │   ├── menu/                 # Menu resources
│   │   │   ├── values/               # Colors, strings, themes
│   │   │   └── drawable/             # Icons
│   │   └── AndroidManifest.xml
│   ├── src/test/
│   │   └── java/com/phoneblocker/
│   │       └── BlockRuleTest.kt      # Unit tests
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## How It Works

### Call Blocking Methods

The app uses two methods to block calls:

1. **CallScreeningService** (Android 10+)
   - Official Android API for call screening
   - Requires being set as default Call Screening app
   - May not work on all custom ROMs

2. **BroadcastReceiver Fallback**
   - Listens for `PHONE_STATE` broadcasts
   - Uses `TelecomManager.endCall()` to reject calls
   - Works on most phones including custom ROMs

### Action Behaviors

| Action | Behavior | Notification |
|--------|----------|--------------|
| **Block** | Reject immediately | No notification |
| **Silence** | Mute ringer, call visible | User can answer/decline |
| **Voicemail** | Decline (routes to voicemail) | Missed call notification |

### Rule Priority

1. **ALLOW** rules are always checked first
2. If an ALLOW rule matches → call is allowed
3. **BLOCK/SILENCE/VOICEMAIL** rules are checked next
4. If a blocking rule matches → action is taken
5. If no rule matches → call is allowed (logged as "NOT MATCH")

## Troubleshooting

### Calls not being blocked

1. **Check permissions:** Ensure Phone and Call Logs permissions are granted
2. **Check the pattern:** Use the Call Log to see the raw number format
3. **Check rule is enabled:** Toggle switch should be ON (blue)
4. **Try full number:** Test with the exact phone number (no wildcards) first

### Finding the right pattern

1. Let a call come through
2. Open **📋 Log** to see the raw number
3. Create a pattern based on that format

### App not appearing in Default Apps

Some phones hide the Call Screening option. The app's BroadcastReceiver fallback should still work.

### Permission denied errors

Some phones require additional permissions. Go to:
- Settings → Apps → Phone Blocker → Permissions → Grant all available permissions

For Chinese ROMs (Xiaomi, OPPO, Vivo):
- Enable "Autostart" permission
- Disable battery optimization for the app

## CI/CD

The project uses GitHub Actions for continuous integration:

- **test.yml** - Runs on push to `main`/`dev` and PRs
  - Runs unit tests
  - Runs Android lint
  - Builds APK

- **release.yml** - Runs on tag push (`v*`)
  - Builds debug and release APKs
  - Creates GitHub Release with APKs attached

To create a release:
```bash
git tag v0.0.3
git push origin v0.0.3
```

## License

MIT License - Feel free to use and modify.

## Contributing

Pull requests welcome! Please:
- Run tests before submitting: `./gradlew testDebugUnitTest`
- Test on multiple devices if possible
- Update documentation for new features

## Links

- **GitHub:** https://github.com/johnwmail/phone-blocker
- **Live Preview:** https://johnwmail.github.io/phone-blocker/preview.html
- **Download:** https://johnwmail.github.io/phone-blocker/
- **Releases:** https://github.com/johnwmail/phone-blocker/releases
