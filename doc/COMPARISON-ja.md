# 比較

数値同士や文字列同士の比較、あるいは特定の要素が配列に含まれているかといった判定を行うことができます。

# 比較系演算子

## 比較演算子

比較演算子には以下のものがあります。

- `left > right`: `left` が `right` より大きいか否かを返す
- `left < right`: `left` が `right` より小さいか否かを返す
- `left >= right`: `left` が `right` 以上であるか否かを返す
- `left <= right`: `left` が `right` 以下であるか否かを返す

```shell
$ flc '[1 > 2, 2 > 2, 3 > 2]'
# [FALSE;FALSE;TRUE]

$ flc '[1 < 2, 2 < 2, 3 < 2]'
# [TRUE;FALSE;FALSE]

$ flc '[1 >= 2, 2 >= 2, 3 >= 2]'
# [FALSE;TRUE;TRUE]

$ flc '[1 <= 2, 2 <= 2, 3 <= 2]'
# [TRUE;TRUE;FALSE]
```

両辺は数値である必要があり、DOUBLE値に変換されて比較されます。

## 等価演算子

等価演算子 `left == right` は、両辺の値が等しいか否かを返します。

```shell
$ flc '[1 == 2, 2 == 2, 3 == 2]'
# [FALSE;TRUE;FALSE]
```

現在、数値や文字列などの一部の型においてのみ定義されています。

## 不等価演算子

不等価演算子 `left != right` は、両辺の値が等しくないか否かを返します。

等価演算子の否定です。

```shell
$ flc '[1 != 2, 2 != 2, 3 != 2]'
# [TRUE;FALSE;TRUE]
```

## 含有演算子

含有演算子 `left @ right` は、 `left` が `right` に含まれているか否かを返します。

右辺のタイプごとの挙動は以下の通りです。

| 右辺のタイプ | 戻り値                       |
|--------|---------------------------|
| 文字列    | 左辺の文字列が右辺の文字列に含まれているか否か   |
| 配列     | 左辺の要素が右辺の配列に含まれているか否か     |
| オブジェクト | 左辺のキーが右辺のオブジェクトに含まれているか否か |

```shell
$ flc '"bcd" @ "abcde"'
# TRUE

$ flc '"123" @ "abcde"'
# FALSE

$ flc '1 @ [1, 2, 3]'
# TRUE

$ flc '4 @ [1, 2, 3]'
# FALSE

$ flc '"a" @ {a: 1; b: 2; c: 3}'
# TRUE

$ flc '"d" @ {a: 1; b: 2; c: 3}'
# FALSE
````

含有演算子の働きは、文字列は部分文字列、配列は要素、オブジェクトはキーと、一貫していません。

### 含有演算子のオーバーライド

含有演算子は、より厳密には右辺の値の `CONTAINS` メソッドを呼び出し、論理値化したものを返します。

```shell
$ flc '
  Basket := {
    CONTAINS: this, item -> item @ this.items
  }

  basket := Basket{items: ["apple", "orange", "banana"]}

  OUT << "Basket: $basket"
  "apple" @ basket && (OUT << "apple is in basket")
  "cherry" @ basket && (OUT << "cherry is in basket")

  ; ,
'
# Basket: {items:[apple;orange;banana]}
# apple is in basket
```

## instanceOf演算子

instanceOf演算子 `left ?= right` は、 `left` が `right` のインスタンスであるか否かを返します。

より厳密には、オブジェクト `left` が `right` と同一のインスタンスか、 `left` の継承チェーンのどこかに `right` が存在するかを判定します。

```shell
$ flc '
  Animal := {}
  Human := Animal {}
  socrates := Human{}
  pythagoras := Human{}

  Animal ?= Animal && (OUT << "Animal is Animal")
  Human ?= Human && (OUT << "Human is Human")
  socrates ?= socrates && (OUT << "Socrates is Socrates")
  pythagoras ?= pythagoras && (OUT << "Pythagoras is Pythagoras")

  Human ?= Animal && (OUT << "Human is Animal")
  Animal ?= Human && (OUT << "Animal is Human")

  socrates ?= Human && (OUT << "Socrates is Human")
  Human ?= socrates && (OUT << "Human is Socrates")

  socrates ?= Animal && (OUT << "Socrates is Animal")
  Animal ?= socrates && (OUT << "Animal is Socrates")

  socrates ?= pythagoras && (OUT << "Socrates is Pythagoras")
  pythagoras ?= socrates && (OUT << "Pythagoras is Socrates")

  ; ,
'
# Animal is Animal
# Human is Human
# Socrates is Socrates
# Pythagoras is Pythagoras
# Human is Animal
# Socrates is Human
# Socrates is Animal
```

## 比較系演算子の結合

比較系演算子同士の結合は特殊で、以下の手順を踏みます。

1. すべての比較系演算子について、左右の辺による比較を行う
2. それらの論理積を取る

例えば、 `3 <= x <= 5` は、まず `3 <= x` を計算し、次に `x <= 5` を計算し、最後にその論理積 `3 <= x && x <= 5` を取ります。

これにより、比較の連鎖を簡潔に記述できます。

```shell
$ flc '3 <= 4 <= 5'
# TRUE

$ flc '3 <= 10 <= 5'
# FALSE
```