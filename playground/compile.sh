#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

# Clean build directory
echo "[INFO ] Cleaning build directory..."
rm -rf build || exit
mkdir -p build || exit

# Compile editor
echo "[INFO ] Installing npm packages..."
npm install || exit
echo "[INFO ] Compiling editor..."
mkdir -p build/out || exit
node_modules/.bin/rollup \
  editor.mjs \
  -f iife \
  -o build/out/editor.bundle.js \
  -p @rollup/plugin-node-resolve \
  || exit

# Copy html files
echo "[INFO ] Copying html files..."
rsync -av src/ build/out || exit

# Copy fluorite12
echo "[INFO ] Copying fluorite12..."
rsync -av ../build/distributions/ build/out || exit
