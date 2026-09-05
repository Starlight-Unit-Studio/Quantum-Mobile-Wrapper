#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
URL="https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
    mkdir -p "$(dirname "$JAR")"
    TMP="$JAR.tmp"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$URL" -o "$TMP"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$URL" -O "$TMP"
    else
        echo "curl or wget is required once to bootstrap Gradle." >&2
        exit 1
    fi

    if command -v sha256sum >/dev/null 2>&1; then
        ACTUAL=$(sha256sum "$TMP" | awk '{print $1}')
        [ "$ACTUAL" = "$EXPECTED" ] || { echo "Gradle wrapper checksum mismatch." >&2; rm -f "$TMP"; exit 1; }
    fi
    mv "$TMP" "$JAR"
fi

exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
