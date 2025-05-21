#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

bash gradlew --no-daemon --refresh-dependencies downloadDependencies
