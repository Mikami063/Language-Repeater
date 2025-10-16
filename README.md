# LanguageRepeater (Android)

A super-minimal 3-button recorder for language practice:

- **Record**: starts a new recording and automatically deletes/overwrites the previous temp recording.
- **Play**: plays the latest temporary recording.
- **Save**: saves the latest temporary recording into `Music/Repeater` (MediaStore).

## Requirements
- Android Studio (Giraffe/Koala or newer)
- Android SDK 34
- Device running Android 8.0+ (minSdk 26). Tested logic works on Android 12+.

## Permissions
- Microphone (RECORD_AUDIO)

## Build & Run
1. Open this folder in Android Studio: `File > Open > LanguageRepeater`
2. Let Gradle sync.
3. Run on your device (Pixel 8 recommended). Grant microphone permission on first launch.

## Notes
- Temporary recording is kept in app cache: `cache/temp_recording.m4a`. Each new **Record** overwrites the temp file by deleting the old one first.
- **Save** copies the temp recording into public storage via MediaStore, at `Music/Repeater/`.
- No comparison, no clutter, no ads.

## UI
- A single Activity with three buttons (Record / Play / Save) and a status line.

## Known behavior
- Recording starts immediately on tapping **Record**, and keeps recording until you tap **Play**, **Save**, or press **Record** again (which overwrites by starting a new take).
- If you prefer a *tap-to-start, tap-to-stop* record button, you can add a separate `Stop` button or a toggle behavior in `MainActivity.kt`.
