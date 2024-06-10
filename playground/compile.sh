#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

# Clean build directory
rm -rf build || exit
mkdir -p build || exit

# Copy html files
rsync -av src/ build/out || exit

# Copy fluorite12
rsync -av ../build/distributions/ build/out || exit
