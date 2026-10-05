#!/bin/bash

# ============================================================
# Dynamic Retail Dashboard Launcher
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1

JAVA="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java"

echo "======================================"
echo " Dynamic Retail Dashboard"
echo "======================================"
echo ""

# ============================================================
# 1. STOP OLD BACKEND
# ============================================================

OLD_PID=$(lsof -tiTCP:3000 -sTCP:LISTEN)

if [ -n "$OLD_PID" ]; then
    echo "Stopping old backend..."
    kill "$OLD_PID" 2>/dev/null
    sleep 0.3
fi

# ============================================================
# 2. START BACKEND
# ============================================================

echo "Starting backend..."

node "$PROJECT_DIR/src/main/java/dashboard/database/server.js" &

BACKEND_PID=$!

# Give Node a moment to start
sleep 0.5

# Wait a little longer only if necessary
ATTEMPTS=0

while ! lsof -iTCP:3000 -sTCP:LISTEN >/dev/null 2>&1; do

    sleep 0.25

    ATTEMPTS=$((ATTEMPTS + 1))

    if [ "$ATTEMPTS" -ge 12 ]; then
        echo ""
        echo "ERROR: Backend failed to start."

        read -p "Press Enter to close..."
        exit 1
    fi

done

echo "Backend ready."

# ============================================================
# 3. PREPARE / COMPILE JAVA
# ============================================================

echo ""

# First launch:
# compile project and create dependency classpath
if [ ! -f "$PROJECT_DIR/target/classpath.txt" ]; then

    echo "Preparing dashboard for first launch..."

    mvn -q compile dependency:build-classpath \
        -Dmdep.outputFile=target/classpath.txt

    if [ $? -ne 0 ]; then

        echo ""
        echo "ERROR: Java compilation failed."

        kill "$BACKEND_PID" 2>/dev/null

        read -p "Press Enter to close..."
        exit 1
    fi

else

    # Future launches:
    # Maven only recompiles changed Java files
    echo "Checking for code changes..."

    mvn -q compile

    if [ $? -ne 0 ]; then

        echo ""
        echo "ERROR: Java compilation failed."

        kill "$BACKEND_PID" 2>/dev/null

        read -p "Press Enter to close..."
        exit 1
    fi

fi

# ============================================================
# 4. CREATE CLASSPATH
# ============================================================

DEPENDENCIES=$(cat "$PROJECT_DIR/target/classpath.txt")

CLASSPATH="$PROJECT_DIR/target/classes:$DEPENDENCIES"

# ============================================================
# 5. START DASHBOARD
# ============================================================

echo "Starting dashboard..."
echo ""

"$JAVA" \
    -cp "$CLASSPATH" \
    dashboard.Main

# ============================================================
# 6. CLOSE BACKEND WHEN DASHBOARD CLOSES
# ============================================================

echo ""
echo "Closing backend..."

kill "$BACKEND_PID" 2>/dev/null

echo "Dashboard closed."