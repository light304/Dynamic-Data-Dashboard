#!/usr/bin/env bash
# Linux launcher: run with ./Launch_Dashboard_Linux.sh (or double-click and
# choose "Run in Terminal", depending on your file manager).

cd "$(dirname "$0")" || exit 1

if ! command -v node >/dev/null 2>&1; then
    echo "ERROR: Node.js was not found."
    echo "Install it from https://nodejs.org or your package manager and try again."
    read -r -p "Press Enter to close..."
    exit 1
fi

node launcher.js
