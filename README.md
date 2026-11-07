# anton

## What it does
A native Android app that trains German police officers and trainees to spell correctly over the radio. You work through fictional ID cards, driving licences and licence plates by speaking the classic German spelling alphabet (Anton, Berta, Cäsar, Dora, ...) and climb the ranks of the Berlin police through error-free cases. Speech recognition runs fully on-device through a local Vosk model, with no internet permission.

## Usage
Requires Android 8 or newer. Install the APK from the releases page, allow the microphone and tap "Nächster Vorgang". Hold the on-screen talk button or a volume key, spell the marked field and let go.

To build it yourself, with JDK 17 and the Android SDK 37: `./gradlew :app:assembleDebug`

## License
MIT, see [LICENSE](LICENSE). Third-party components are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
