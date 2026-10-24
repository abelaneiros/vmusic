vMusic
======================

vMusic is a [GPLv3](LICENSE) licensed MP3/OGG/FLAC/PCM player for Android with the following features:
* multiple playlist support
* grouping by artist, album or genre
* plain filesystem browsing
* [ReplayGain](https://en.wikipedia.org/wiki/ReplayGain) support
* headset/Bluetooth controls
* accelerometer/shake control
* cover art support
* [Simple Last.fm Scrobbler](https://github.com/tgwizard/sls) support

Origin
======
vMusic is a fork of [Vanilla Music](https://github.com/vanilla-music/vanilla) by Adrian Ulrich and contributors, based on upstream commit `38e17613`.

It is a modified version: changes include the History tab, Maven build, minSdk 26, the reworked permission flow, English and Spanish only, and a new icon.


Building
========
To build you will need:

 * A JDK, version 17 or newer
 * [Apache Maven](https://maven.apache.org/) 3.6.3 or newer
 * The Android SDK with `platforms;android-34` and `build-tools;36.0.0` installed, and `ANDROID_HOME` pointing to it (or pass `-Dandroid.sdk.path=/path/to/sdk`)

Building from command-line
--------------------------
 * `mvn package` to build the APKs:
   * `app/target/app-debug.apk` (signed with the debug key, `~/.android/debug.keystore` is created if missing)
   * `app/target/app-release-unsigned.apk` (shrunk with R8, zipaligned, unsigned)
 * Optional: `adb install -r app/target/app-debug.apk` to install the APK to a connected device

Building with Android Studio
---------------------
You can also build with Android Studio by importing this project into it.

Building from Eclipse
---------------------
You can also build from Eclipse. Create a new Android Project, choosing "Create
project from exisiting source", then set the compiler compliance level to 1.6
in project settings.

Documentation
=============
Javadocs can be generated using `ant doc`

