# Notebook

A study organiser for university students, built for the CSIE program at ASE Bucharest. Android, Kotlin, Jetpack Compose.

**[Download the latest APK](https://github.com/sleepdeprived-dev/Notebook/releases/latest)** (Android 8.0 or newer)

## Features

- **Schedule.** Import your timetable as a PDF or a photo. It's shown right in the app with pinch-to-zoom, and you can change it any time.
- **Courses.** Each subject starts with *Curs* and *Seminar* folders, and you can add more. Import PDFs (several at once) and open them in your PDF viewer. Files are copied into the app, so they stay even if you delete the originals.
  - Logo per subject (from your gallery)
  - Coloured tags on subjects and folders, with a filter on the Courses screen
  - Pin subjects, folders and PDFs to the top
  - Rename and delete everything
- **To Do.** Tasks with an optional subject and due date, sorted by deadline, with overdue ones highlighted. Swipe to delete (with Undo), tick to complete.
- **Settings.** Light, dark or automatic theme. Accent colour that matches Android's wallpaper colours, or one of red, orange, yellow, green, blue or purple.
- **In-app updates.** Settings → Updates checks this repo's latest release, downloads it (verifying its SHA-256) and hands it to Android's installer.
- **Tests**: coming next.

## Installing

1. Download `Notebook-v*.apk` from [Releases](https://github.com/sleepdeprived-dev/Notebook/releases).
2. Open it on your phone and allow installing from that source when asked.

From v1.1.0 on, newer versions can be installed from **Settings → Updates** inside the app. Your data is kept.

## Building

Requirements: Android Studio (it bundles the JDK) and Android SDK 37.

```sh
git clone https://github.com/sleepdeprived-dev/Notebook.git
cd Notebook
./gradlew assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press ▶.

### Release builds

`./gradlew assembleRelease` signs the APK if it finds a keystore. Put the keystore path and passwords in `local.properties`, which is never committed:

```properties
notebook.storeFile=/path/to/notebook-release.jks
notebook.storePassword=…
notebook.keyAlias=notebook
notebook.keyPassword=…
```

Or set `NOTEBOOK_STORE_FILE`, `NOTEBOOK_STORE_PASSWORD`, `NOTEBOOK_KEY_ALIAS` and `NOTEBOOK_KEY_PASSWORD`. Without either, the release APK is built unsigned.

## Tech

- Kotlin, Jetpack Compose, Material 3; one activity
- Room (exported schemas in `app/schemas/` and auto-migrations), ViewModel + StateFlow, Navigation Compose
- Storage Access Framework and the photo picker for imports, so the app needs no storage permissions; FileProvider for opening PDFs in other apps
- No third-party libraries besides AndroidX

## Credits

The interface uses [Figtree](https://github.com/erikdkennedy/figtree) by Erik Kennedy, under the SIL Open Font License ([`tools/fonts/Figtree-OFL.txt`](tools/fonts/Figtree-OFL.txt)).

## License

[MIT](LICENSE)
