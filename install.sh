#!/usr/bin/env bash

# インストールディレクトリの判定
if [ "${BASH_SOURCE[0]}" = "" ]
then
  # パイプで渡された
  SCRIPT_DIR=$(pwd)/fluorite12
else
  # ファイルを実行した
  SCRIPT_DIR=$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)
fi

# インストールディレクトリの準備
mkdir -p "$SCRIPT_DIR" || exit

# リポジトリを最新の状態に更新
if [ -d "$SCRIPT_DIR"/.git ]
then
  git -C "$SCRIPT_DIR" fetch origin release || exit
  git -C "$SCRIPT_DIR" reset --hard origin/release || exit
else
  git clone --single-branch --branch release --depth 1 https://github.com/MirrgieRiana/fluorite12.git "$SCRIPT_DIR" || exit
fi

# /usr/local/bin にインストール
destination=/usr/local/bin/flc
rm -f "$destination" || exit
ln -s "$SCRIPT_DIR"/bin/linuxX64/flcReleaseExecutable/flc.kexe "$destination" || exit
destination=/usr/local/bin/flc-update
rm -f "$destination" || exit
ln -s "$SCRIPT_DIR"/install.sh "$destination" || exit
