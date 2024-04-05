#!/usr/bin/env bash
SCRIPT_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
destination=/usr/local/bin/flc

[ -e "$destination" ] && rm -i "$destination"

ln -s "$SCRIPT_PATH"/build/bin/linuxX64/flcReleaseExecutable/flc.kexe "$destination"
