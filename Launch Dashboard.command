#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1

echo "======================================"
echo " Dynamic Retail Dashboard"
echo "======================================"
echo ""

# --------------------------------------------------
# BACKEND
# --------------------------------------------------

echo "Stopping old backend..."

OLD_PID=$(lsof -tiTCP:3000 -sTCP:LISTEN)

if [ -n "$OLD_PID" ]; then
    kill "$OLD_PID" 2>/dev/null
    sleep 1
fi

echo "Starting backend..."

node "$PROJECT_DIR/src/main/java/dashboard/database/server.js" &

BACKEND_PID=$!

sleep 2

if ! lsof -iTCP:3000 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Backend failed to start."
    read -p "Press Enter to close..."
    exit 1
fi

echo "Backend started."

# --------------------------------------------------
# COMPILE CURRENT JAVA CODE
# --------------------------------------------------

echo ""
echo "Compiling dashboard..."

mvn compile dependency:build-classpath \
    -Dmdep.outputFile=target/classpath.txt

if [ $? -ne 0 ]; then
    echo ""
    echo "Compilation failed."
    kill "$BACKEND_PID" 2>/dev/null
    read -p "Press Enter to close..."
    exit 1
fi

# --------------------------------------------------
# BUILD CLASSPATH
# --------------------------------------------------

DEPENDENCIES=$(cat target/classpath.txt)

CLASSPATH="$PROJECT_DIR/target/classes:$DEPENDENCIES"

# --------------------------------------------------
# RUN JAVA DIRECTLY — SAME STYLE AS VS CODE
# --------------------------------------------------

echo ""
echo "Starting dashboard..."

"/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java" \
    -cp "$CLASSPATH" \
    dashboard.Main

# --------------------------------------------------
# CLEAN UP
# --------------------------------------------------

echo ""
echo "Closing backend..."

kill "$BACKEND_PID" 2>/dev/null