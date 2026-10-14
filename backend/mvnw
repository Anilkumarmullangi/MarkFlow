#!/usr/bin/env sh
set -eu
MAVEN_VERSION="3.9.11"
WRAPPER_ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/.mvn/wrapper"
MAVEN_HOME="$WRAPPER_ROOT/apache-maven-$MAVEN_VERSION"
if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_ROOT"
  ARCHIVE="$WRAPPER_ROOT/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "Downloading Apache Maven $MAVEN_VERSION (one-time setup)..."
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$ARCHIVE"
  else wget -O "$ARCHIVE" "$URL"
  fi
  tar -xzf "$ARCHIVE" -C "$WRAPPER_ROOT"
  rm -f "$ARCHIVE"
fi
exec "$MAVEN_HOME/bin/mvn" "$@"
