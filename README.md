# Blacklane Auto-Accept Bot

Android bot that automatically accepts Blacklane Chauffeur ride offers based on location and pickup time filters.

## Features

- Monitors Blacklane push notifications in the background
- Filters offers to Canada-only (excludes USA rides)
- Accepts rides only within a configurable pickup time window (default: 1–2 hours from now)
- No root required — uses Android Accessibility Service + Notification Listener
- Survives phone reboots
- Simple setup UI with live permission status

## How It Works

1. Blacklane sends a "New offer" notification
2. Bot opens the Blacklane Chauffeur app automatically
3. Accessibility service reads the offers list on screen
4. For each offer it checks:
   - Is the pickup location in Canada? (matches province codes, major airports, city names)
   - Is the pickup time within the configured window?
5. If both match, the bot taps the accept button

## Setup

### Requirements

- Android phone (Android 8.0+)
- Blacklane Chauffeur app installed and logged in
- Android Studio to build the APK

### Build

1. Clone this repo
2. Open in Android Studio
3. Confirm the Blacklane package name:
   ```
   adb shell pm list packages | grep black
   ```
   Update `BotConfig.BLACKLANE_PACKAGE` if needed
4. Build → Generate Signed APK (or run directly via USB for testing)

### First-Time Setup on Phone

After installing the APK:

1. Open the **Blacklane Bot** app
2. Tap **Enable Notification Access** → find Blacklane Bot → toggle on
3. Tap **Enable Accessibility Service** → find Blacklane Bot → toggle on
4. Both indicators turn green → bot is active

### Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Min hours | 1.0 | Minimum hours before pickup to accept |
| Max hours | 2.0 | Maximum hours before pickup to accept |

## Project Structure

```
app/src/main/java/com/blacklanebot/
├── MainActivity.kt                  # Setup UI and status display
├── BotConfig.kt                     # Canada/USA keyword lists and settings
├── OfferParser.kt                   # Time and location parsing logic
├── BlacklaneNotificationListener.kt # Catches push notifications
├── BlacklaneAccessibilityService.kt # Reads screen and taps accept button
└── BootReceiver.kt                  # Restarts services after reboot
```

## Notes

- Tested against Blacklane Chauffeur app UI as of June 2026
- If Blacklane updates their app UI, the accessibility node detection may need re-tuning
- The bot does not store or transmit any account data


