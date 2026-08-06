#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$PROPERTIES" ]; then
    echo "ERROR: missing $PROPERTIES" >&2
    exit 1
fi

read_property() {
    sed -n "s/^$1=//p" "$PROPERTIES" | tail -n 1 | tr -d '\r'
}

DISTRIBUTION_URL=$(read_property distributionUrl)
EXPECTED_SHA512=$(read_property distributionSha512Sum)

if [ -z "$DISTRIBUTION_URL" ] || [ -z "$EXPECTED_SHA512" ]; then
    echo "ERROR: Maven distribution URL and SHA-512 must be pinned" >&2
    exit 1
fi

MAVEN_VERSION=$(printf '%s' "$DISTRIBUTION_URL" | sed -n 's#.*apache-maven-\([0-9][0-9.]*\)-bin\.zip#\1#p')
if [ -z "$MAVEN_VERSION" ]; then
    echo "ERROR: cannot derive Maven version from $DISTRIBUTION_URL" >&2
    exit 1
fi

MAVEN_USER_HOME=${MAVEN_USER_HOME:-"$HOME/.m2"}
INSTALL_ROOT="$MAVEN_USER_HOME/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_HOME="$INSTALL_ROOT/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

verify_sha512() {
    archive=$1
    if command -v shasum >/dev/null 2>&1; then
        actual=$(shasum -a 512 "$archive" | awk '{print $1}')
    elif command -v sha512sum >/dev/null 2>&1; then
        actual=$(sha512sum "$archive" | awk '{print $1}')
    else
        echo "ERROR: shasum or sha512sum is required" >&2
        return 1
    fi

    if [ "$actual" != "$EXPECTED_SHA512" ]; then
        echo "ERROR: Maven distribution SHA-512 mismatch" >&2
        echo "Expected: $EXPECTED_SHA512" >&2
        echo "Actual:   $actual" >&2
        return 1
    fi
}

if [ ! -x "$MAVEN_BIN" ]; then
    command -v unzip >/dev/null 2>&1 || {
        echo "ERROR: unzip is required to install Maven Wrapper distribution" >&2
        exit 1
    }

    mkdir -p "$INSTALL_ROOT"
    TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/coupon-service-maven.XXXXXX")
    trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM
    ARCHIVE="$TMP_DIR/apache-maven-$MAVEN_VERSION-bin.zip"

    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --silent --show-error "$DISTRIBUTION_URL" --output "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget --quiet "$DISTRIBUTION_URL" --output-document="$ARCHIVE"
    else
        echo "ERROR: curl or wget is required to download Maven" >&2
        exit 1
    fi

    verify_sha512 "$ARCHIVE"
    rm -rf "$MAVEN_HOME"
    unzip -q "$ARCHIVE" -d "$INSTALL_ROOT"
fi

exec "$MAVEN_BIN" "$@"
