#!/usr/bin/env bash
set -euo pipefail

usage() {
	echo "Usage: $0 <major.minor.patch>" >&2
	exit 1
}

[ $# -eq 1 ] || usage
NEW_VERSION="$1"
[[ "$NEW_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || usage

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"
VERSION_CODE=$((10#$MAJOR * 10000 + 10#$MINOR * 100 + 10#$PATCH))
RELEASE_DATE="$(date '+%d/%m/%Y')"

cd "$(dirname "$0")/.."

ROOT_POM="pom.xml"
APP_POM="app/pom.xml"
MANIFEST="app/src/main/AndroidManifest.xml"
ABOUT="app/src/main/assets/about.html"
FILES=("$ROOT_POM" "$APP_POM" "$MANIFEST" "$ABOUT")

die() {
	echo "Error: $*" >&2
	exit 1
}

for f in "${FILES[@]}"; do
	[ -f "$f" ] || die "$f not found"
	if grep -qE '^(<{7}|={7}|>{7})( |$)' "$f"; then
		die "$f contains merge conflict markers"
	fi
done

ABOUT_VERSION_LINE='^<p><b>Version:</b> .*<br><b>Released:</b> .*</p>$'
[ "$(grep -c '<b>Version:</b>' "$ABOUT" || true)" -eq 1 ] || die "$ABOUT must contain exactly one Version line"
grep -qE "$ABOUT_VERSION_LINE" "$ABOUT" || die "$ABOUT Version line must look like: <p><b>Version:</b> X.Y.Z<br><b>Released:</b> DD/MM/YYYY</p>"

[ -z "$(git status --porcelain)" ] || die "working tree is not clean"

CURRENT_VERSION="$(sed -n '/<artifactId>music-parent<\/artifactId>/{n;s|.*<version>\([^<]*\)</version>.*|\1|p;q}' "$ROOT_POM")"
[ "$CURRENT_VERSION" != "$NEW_VERSION" ] || die "version is already $NEW_VERSION"

echo "Updating version $CURRENT_VERSION -> $NEW_VERSION (versionCode $VERSION_CODE)"

sed -i "/<artifactId>music-parent<\/artifactId>/{n;s|<version>[^<]*</version>|<version>$NEW_VERSION</version>|}" "$ROOT_POM" "$APP_POM"

sed -i \
	-e "s|android:versionName=\"[^\"]*\"|android:versionName=\"$NEW_VERSION\"|" \
	-e "s|android:versionCode=\"[^\"]*\"|android:versionCode=\"$VERSION_CODE\"|" \
	"$MANIFEST"

sed -i -E \
	-e "s|$ABOUT_VERSION_LINE|<p><b>Version:</b> $NEW_VERSION<br><b>Released:</b> $RELEASE_DATE</p>|" \
	"$ABOUT"

check_changes() {
	local file="$1" expected="$2" actual
	actual="$(git diff --numstat -- "$file" | cut -f1)"
	actual="${actual:-0}"
	if [ "$actual" -ne "$expected" ]; then
		git checkout -- "${FILES[@]}"
		die "$file: expected $expected changed lines, got $actual (changes reverted)"
	fi
}

check_changes "$ROOT_POM" 1
check_changes "$APP_POM" 1
check_changes "$MANIFEST" 2
check_changes "$ABOUT" 1

if ! grep -qxF "<p><b>Version:</b> $NEW_VERSION<br><b>Released:</b> $RELEASE_DATE</p>" "$ABOUT"; then
	git checkout -- "${FILES[@]}"
	die "$ABOUT: Version line was not updated to $NEW_VERSION / $RELEASE_DATE (changes reverted)"
fi
echo "About page: Version $NEW_VERSION Released: $RELEASE_DATE"

git --no-pager diff --stat -- "${FILES[@]}"

git add -- "${FILES[@]}"
git commit -m "$NEW_VERSION"
git push origin HEAD

echo "Version $NEW_VERSION committed and pushed"
