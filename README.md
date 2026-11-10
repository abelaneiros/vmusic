vMusic
======================

vMusic is a [GPLv3](LICENSE) licensed MP3/OGG/FLAC/PCM player for Android with the following features:
* multiple playlist support
* grouping by artist, album, or genre
* plain filesystem browsing
* [ReplayGain](https://en.wikipedia.org/wiki/ReplayGain) support
* headset/Bluetooth controls
* accelerometer/shake control
* cover art support
* [Simple Last.fm Scrobbler](https://github.com/tgwizard/sls) support

Origin
======
vMusic is a fork of [Vanilla Music](https://github.com/vanilla-music/vanilla) by Adrian Ulrich and contributors, based on upstream commit `38e17613`.

Building
========

To build, you will need:
 * A JDK, version 17 or newer
 * [Apache Maven](https://maven.apache.org/) 3.6.3 or newer
 * The Android SDK with `platforms;android-34` and `build-tools;36.0.0` installed, and `ANDROID_HOME` pointing to it (or pass `-Dandroid.sdk.path=/path/to/sdk`)


Building from command-line:
 * `mvn package` to build the APKs:
   * `app/target/app-debug.apk` (signed with the debug key, `~/.android/debug.keystore` is created if missing)
   * `app/target/app-release-unsigned.apk` (shrunk with R8, zipaligned, unsigned)
 * `adb install -r app/target/app-debug.apk` to install the APK to a connected device


Release notes
=============

### 2.6.0
- History item is not removed once the paying queue ends.

### 2.5.0
- Lower min SDK version to 21.
- Shuffle button simplified. Now with only 2 statuses: `No shuffle`, `Shuffle tracks within queue`.
- Repeat button simplified. Now with only 3 statuses: `No repeat`, `Repeat tracks within queue`, and `Repeat current track`.
- About page simplified.

### 2.4.0
- Release notes added to `README.md`.
- Back 15 secs and forward 15 secs buttons added to the control bar.
- Seek gestures to move 15 secs instead of 10 secs.

### 2.3.0
- Public repo created in Github and code pushed.

### 2.2.0
- New icon with green colour.
- Added message explaining why the app needs each permission.
- Playing bar more visible (play/pause and other buttons always visible).
- The history tab was added.

### 2.0.0
- App renamed to vMusic.
- The default package was renamed to abelaneiros.vmusic.
- Migrated from Gradle to Maven.

### 1.3.2
- Fork/clone to original version.
