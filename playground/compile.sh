#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

# Clean build directory
rm -rf build || exit
mkdir -p build || exit

# Compile editor
npm install || exit
mkdir -p build/out || exit
node_modules/.bin/rollup \
  editor.mjs \
  -f iife \
  -o build/out/editor.bundle.js \
  -p @rollup/plugin-node-resolve \
  || exit

# Copy html files
rsync -av src/ build/out || exit

# Copy fluorite12
rsync -av ../build/distributions/ build/out || exit
