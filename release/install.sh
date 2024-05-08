#!/usr/bin/env bash
git clone --single-branch --branch release --depth 1 https://github.com/MirrgieRiana/fluorite12.git || exit
destination=/usr/local/bin/flc
[ -e "$destination" ] && { rm -i "$destination" || exit; }
ln -s fluorite12/bin/linuxX64/flcReleaseExecutable/flc.kexe "$destination" || exit
