#!/usr/bin/env bash

SRC="$HOME/Music/music.android"
DEST="/storage/emulated/0/Music" 

android emulator start medium_phone &

adb wait-for-device
# wait until Android has fully booted, not just until adbd is up
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do
  sleep 1
done

adb shell mkdir -p "$DEST"
adb push --sync "$SRC/." "$DEST/"   # --sync only copies new/changed files