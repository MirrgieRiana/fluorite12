# コミットタイトル

## 記述例

```
ADD: GROUP

CHANGE: rename: pi -> PI

CHANGE: HEAD -> TAKE; ADD: TAKER

internal: add: FluoriteValue.fluoriteEquals

refactor: STREAM::_+_

build: format:
```

## ドメイン

プログラムのメイン部分以外に対する操作は、カテゴリの前にドメイン部を置く。

- build: ビルドツール関連
- ci: GitHub Actions関連
- doc: ドキュメント関連
- test: テスト関連

## カテゴリ

コミットで行ったことを大雑把に示す。

- add: 機能追加
- change: 動作の変更
- remove: 機能の削除
- comment: コメントの変更
- doc: ドキュメント（KDocなども含む）の変更
- internal: 表に出ない変更、APIの追加のみのコミットなど
- refactor: プログラムの意味を変えない構造の変更
- cleanup: プログラムの構造を変えないシンタックスシュガーレベルの変更
- format: プログラムの構文木を変えない空白文字の書き方の変更

ドキュメンテーションが必要な変更は、 `ADD` のように大文字で書く。

## 本文

極力、何をしたかを自然言語を使わずに書く。

それが無理な場合、日本語で書く。
