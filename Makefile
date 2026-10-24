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

install:
	adb install -r $(DEBUG_APK)

uninstall:
	adb uninstall aneiros.vmusic

version:
	./scripts/set-version.sh "$(V)"

start:
	./scripts/start-android.sh

stop:
	./scripts/stop-android.sh

