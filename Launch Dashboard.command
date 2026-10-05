#!/bin/bash

# Move to the folder where this launcher is located
cd "$(dirname "$0")"

echo "Starting Dynamic Retail Dashboard..."

# Start backend only if port 3000 isn't already being used
if ! lsof -i :3000 >/dev/null 2>&1; then
    echo "Starting backend..."
    node src/main/java/dashboard/database/server.js &
    BACKEND_PID=$!

    # Give backend time to start
    sleep 2
else
    echo "Backend is already running."
fi

echo "Starting dashboard..."

# Run the existing Java application through Maven
mvn compile exec:java -Dexec.mainClass="dashboard.Main"

# Stop backend when dashboard closes
if [ ! -z "$BACKEND_PID" ]; then
    kill $BACKEND_PID 2>/dev/null
fi
