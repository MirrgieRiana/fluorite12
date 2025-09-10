#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

# Compile editor
echo "[INFO ] Installing npm packages..."
npm install || exit
echo "[INFO ] Compiling editor..."
mkdir -p build/editor || exit
node_modules/.bin/rollup \
  editor.mjs \
  -f iife \
  -o build/editor/editor.bundle.js \
  -p @rollup/plugin-node-resolve \
  || exit
