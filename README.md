# Telegram Monitor

An Android application that monitors when a user is viewing the "Meduza — LIVE" channel in Telegram and displays an overlay notification.

## Features

- Background monitoring of Telegram app content
- Detects when the "Meduza — LIVE" channel is open
- Displays an overlay popup notification
- Provides option to quickly minimize Telegram

## Requirements

- Android 8.0 (API level 26) or higher
- Telegram app installed on the device

## Installation

### Build from Source

1. Clone this repository
2. Open the project in Android Studio
3. Build and install using:

```bash
./gradlew installDebug
```

Alternatively, you can build an APK:

```bash
./gradlew assembleDebug
```

The APK will be available at `app/build/outputs/apk/debug/app-debug.apk`

## Setup and Usage

After installation, you need to grant the required permissions:

1. Open the Telegram Monitor app
2. Tap "Дать Overlay-разрешение" to grant the overlay permission
3. Tap "Включить сервис" to enable the accessibility service
4. In the Android Accessibility Settings, find and enable "Telegram Monitor"

Once properly configured, the app will:
- Run in the background
- Monitor screen content for the text "Meduza — LIVE"
- Display a popup notification when the channel is detected
- Allow you to minimize Telegram with one tap

## Permissions

This app requires the following permissions:

- **Overlay permission**: To display notification popups over other apps
- **Accessibility service**: To read screen content and detect the channel name

## How It Works

The app uses Android's Accessibility Service API to monitor text displayed on the screen. When it detects the specific text "Meduza — LIVE", it displays an overlay notification using the system alert window functionality.

## Privacy

This application only detects the presence of specific text ("Meduza — LIVE") on your screen. It does not collect, store, or transmit any data.