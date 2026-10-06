#!/bin/bash
# Mac launcher: double-click to start the dashboard.

cd "$(dirname "$0")" || exit 1

# Double-clicked scripts start with a minimal PATH, so add the usual
# Homebrew locations (Apple Silicon and Intel).
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

if ! command -v node >/dev/null 2>&1; then
    echo "ERROR: Node.js was not found."
    echo "Install it from https://nodejs.org and try again."
    read -r -p "Press Enter to close..."
    exit 1
fi

node launcher.js
