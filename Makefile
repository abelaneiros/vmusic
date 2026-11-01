.PHONY: debug build clean uninstall version

MVN = mvn -B

ifeq (version,$(firstword $(MAKECMDGOALS)))
V ?= $(word 2,$(MAKECMDGOALS))
ifneq ($(V),)
$(eval $(V):;@:)
endif
endif

DEBUG_APK = ./app/target/app-debug.apk

default:

debug: build
	adb install -r $(DEBUG_APK)

build: clean
	$(MVN) package

clean:
	$(MVN) clean

version:
	./scripts/set-version.sh "$(V)"

clear-app:
	adb shell pm clear aneiros.vmusic

install-app:
	adb install -r $(DEBUG_APK)

uninstall-app:
	adb uninstall aneiros.vmusic

start-emulator:
	./scripts/start-android.sh

stop-emulator:
	./scripts/stop-android.sh

