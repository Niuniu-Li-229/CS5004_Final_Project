#!/bin/bash
# Compile and run script for TSLA Stock Analyzer
# Run from the project root: CS5004_Final_Project-main/
#
# Usage:
#   ./compile.sh          — compile all source files
#   ./compile.sh run      — compile then launch the GUI
#   ./compile.sh demo     — compile then run ModelDemo (CLI)
#   ./compile.sh test     — compile then run JUnit tests (requires junit-platform-console-standalone.jar)

set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"

echo "=== Compiling model ==="
mkdir -p "$OUT"
javac -d "$OUT" "$ROOT/src/model/"*.java

echo "=== Compiling controller ==="
javac -d "$OUT" -cp "$OUT" "$ROOT/src/controller/"*.java

echo "=== Compiling view ==="
javac -d "$OUT" -cp "$OUT" "$ROOT/src/view/"*.java

echo "=== Build complete. Output: out/ ==="

case "$1" in
  run)
    echo "=== Launching GUI ==="
    java -cp "$OUT" view.MainFrame
    ;;
  demo)
    echo "=== Running ModelDemo ==="
    java -cp "$OUT" controller.ModelDemo
    ;;
  test)
    JUNIT_JAR="$ROOT/lib/junit-platform-console-standalone.jar"
    if [ ! -f "$JUNIT_JAR" ]; then
      echo "ERROR: Place junit-platform-console-standalone.jar in lib/"
      exit 1
    fi
    echo "=== Compiling tests ==="
    javac -d "$OUT" -cp "$OUT:$JUNIT_JAR" "$ROOT/test/"*.java
    echo "=== Running tests ==="
    java -jar "$JUNIT_JAR" --class-path "$OUT" --scan-class-path
    ;;
esac
