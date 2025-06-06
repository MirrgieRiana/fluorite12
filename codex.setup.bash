#!/usr/bin/env bash

cd "$(dirname "$0")" || exit

real_java_path=$(readlink -f "$(command -v java)")
JAVA_HOME=$(dirname "$(dirname "$real_java_path")")

mkdir -p ./openjdk17/jdk || exit
wget -O ./openjdk17/openjdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk" || exit
tar -xzf ./openjdk17/openjdk17.tar.gz -C ./openjdk17/jdk --strip-components=1 || exit

rm -r ./openjdk17/jdk/lib/security/cacerts || exit
cp -r "$JAVA_HOME"/lib/security/cacerts ./openjdk17/jdk/lib/security/cacerts || exit
