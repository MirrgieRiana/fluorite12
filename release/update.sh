#!/usr/bin/env bash
SCRIPT_PATH=$(cd "$(dirname "$(readlinf -f "${BASH_SOURCE[0]}")")" && pwd)
git -C "$SCRIPT_PATH" fetch origin release || exit
git -C "$SCRIPT_PATH" reset --hard origin/release || exit
