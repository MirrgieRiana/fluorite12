#!/usr/bin/env bash

mkdir -p fluorite12 || exit

if [ -d fluorite12/.git ]
then
  git -C fluorite12 fetch origin release
  git -C fluorite12 reset --hard origin/release
else
  git clone --single-branch --branch release --depth 1 https://github.com/MirrgieRiana/fluorite12.git fluorite12 || exit
fi

destination=/usr/local/bin/flc

if [ -e "$destination" ]
then
  rm -i "$destination" || exit
fi

ln -s fluorite12/bin/linuxX64/flcReleaseExecutable/flc.kexe "$destination" || exit
