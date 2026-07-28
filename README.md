# Medication Wizard 🧙‍♂️

Medication Wizard is a robust, feature-rich Android application designed by **Wise Medical Apps** to help users manage their medication schedules with "magical" ease and precision.

## 🌟 Key Features

- **Unified Notifications**: Intelligent grouping of medications scheduled for the exact same time into a single, clean notification tray entry with bulk actions (Take All, Skip All, Snooze All).
- **Smart Dose Recovery**: Proactive handling of unskipped or "untaken" doses. The app automatically restores future alarms or prompts for a recovery action (Take Now, Reschedule) for past doses.
- **Precision Reminders**: High-accuracy alerts using `AlarmManager`, with customizable alert sounds, volumes, and custom vibration/flash patterns.
- **Bypass System Volume**: Option to play reminders even when the device is set to silent or Do Not Disturb.
- **Flexible Scheduling**: Support for various frequencies (Once a day, multiple times a day) and specific intake instructions (e.g., "After eating").
- **Health Journey Tracking**: Comprehensive history and statistics tracking with a "Magic Streak" indicator to reward consistency.
- **Google Drive Backup**: Securely backup and restore medication data and history using Google Drive integration.
- **Multi-Layout Support**: Optimized UI for Phone and Tablet (7" & 10") in both Portrait and Landscape orientations.

## 🛠 Tech Stack

- **Language**: Primary Java (Business Logic & UI), Kotlin (Previews & Modern Components).
- **Architecture**: MVVM (Model-View-ViewModel) with Android Jetpack.
- **Database**: Room Persistence Library for dose history and specific instances.
- **Preferences**: SharedPreferences for global settings and medication definitions.
- **Navigation**: Jetpack Navigation Component.
- **Background Work**: WorkManager for periodic cleanup and AlarmManager for precise reminders.
- **UI**: Material Design 3 (Material You) with ViewBinding.
- **Image Loading**: Glide.
- **Integrations**:
    - **Firebase**: Remote Config, Analytics, Messaging, Crashlytics.
    - **Google Play Services**: AdMob, Auth, Drive Backup, In-App Billing, In-App Review.

## 🏗 Architecture Overview

The project follows a modified MVVM pattern organized by feature:
- `ui/`: Contains Fragments, ViewModels, and Adapters (e.g., `todaysmedications`, `medicationslist`).
- `reminders/` & `notifications/`: The core reminder engine handling system alarms and notification lifecycle.
- `database/` & `entities/`: Data layer managing Room entities, DAOs, and domain models.
- `backup/`, `billing/`, `ads/`: Modular controllers for integrated services.

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 21
- Android SDK 37 (Compile SDK)
- A physical device or emulator running Android 9.0 (API 28) or higher

### Installation
1. Clone the repository.
2. Open the project in Android Studio.
3. Add your `google-services.json` to the `/app` directory (required for Firebase/Google Play features).
4. Build and run the `:app` module.

## 📜 License & Development

Developed by **Wise Medical Apps**.
© 2026 Wise Medical Apps. All rights reserved.

---
*Helping you manage your health, one spell at a time.*
