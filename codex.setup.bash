#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

bash gradlew --no-daemon --refresh-dependencies downloadDependencies

wget -O openjdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
