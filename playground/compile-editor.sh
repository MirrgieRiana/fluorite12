#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

if [ -s "$HOME/.nvm/nvm.sh" ]
then
  . "$HOME/.nvm/nvm.sh"
  nvm use 20 >/dev/null 2>&1 || nvm install 20
fi

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
