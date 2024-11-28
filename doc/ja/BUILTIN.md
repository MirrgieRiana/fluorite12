組み込み定数は、コード上で定義することなく利用可能な、言語機能に付属する定数です。

組み込み定数は大文字および `_` のみを使って定義されます。

組み込みの関数も組み込み定数と同じメカニズムによって提供されます。

# 組み込みオブジェクトのクラス定数

各種組み込みオブジェクトのクラスを参照できます。

- `VALUE_CLASS`
- `NULL_CLASS`
- `INT_CLASS`
- `DOUBLE_CLASS`
- `BOOLEAN_CLASS`
- `STRING_CLASS`
- `ARRAY_CLASS`
- `OBJECT_CLASS`
- `FUNCTION_CLASS`
- `STREAM_CLASS`

# 定数

各種、特別な値を表す定数です。

| 定数          | 意味      |
|-------------|---------|
| `NULL` `N`  | NULL値   |
| `TRUE` `T`  | 真       |
| `FALSE` `F` | 偽       |
| `EMPTY` `E` | 空のストリーム |

---

数学系の組み込み定数です。

| 定数        | 意味    |
|-----------|-------|
| `MATH.PI` | 円周率   |
| `MATH.E`  | ネイピア数 |

# 数学系関数

## `FLOOR` 小数点以下切り捨て

`FLOOR(number: NUMBER): INTEGER`

第1引数の数値を、値が小さい方の整数に丸めます。

```shell
$ flc 'FLOOR(1.5)'
# 1
```

## `SQRT` 平方根の取得

`SQRT(number: NUMBER): NUMBER`

第1引数の正の平方根を返します。

```shell
$ flc 'SQRT(100.0)'
# 10.0
```

# ストリーム系関数

## `REVERSE` ストリームを逆順にする

`REVERSE(stream: STREAM<VALUE>): STREAM<VALUE>`

第1引数のストリームの要素を逆順にしたストリームを返します。

```shell
$ flc 'REVERSE(1 .. 3)'
# 3
# 2
# 1
```

## `JOIN` ストリームを文字列に連結

`JOIN(separator: VALUE; stream: VALUE): STRING`

第2引数のストリームの各要素を第1引数のセパレータで連結した文字列を返します。

```shell
$ flc 'JOIN("|"; "a", "b", "c")'
# a|b|c
```

---

セパレータやストリームの各要素は文字列化されます。

```shell
$ flc 'JOIN(0; 1, "b", {`&_`: _ -> "c"}{})'
# 10b0c
```

---

部分適用とともに用いることで、パイプチェーンに組み込みやすくなります。

```shell
$ flc '1 .. 3 | _ * 10 >> JOIN["|"]'
# 10|20|30
```

## `SPLIT` 文字列をストリームに分割

`SPLIT(separator: VALUE; string: VALUE): STREAM<STRING>`

第2引数の文字列を第1引数のセパレータで分割し、各部分をストリームとして返します。

パイプ演算子との親和性のために配列ではなくストリームとして返されることに注意してください。

`SPLIT` は概念的に `JOIN` と逆の操作を行います。

```shell
$ flc 'SPLIT("|"; "a|b|c")'
# a
# b
# c
```

---

セパレータや分割対象文字列は文字列化されて評価されます。

---

部分適用とともに用いることで、パイプチェーンに組み込みやすくなります。

```shell
$ flc '"10|20|30" >> SPLIT["|"] | +_ / 10'
# 1
# 2
# 3
```

## `KEYS` オブジェクトのキーのストリームを取得

`KEYS(object: OBJECT): STREAM<STRING>`

第1引数のオブジェクトのキーのストリームを返します。

```shell
$ flc 'KEYS({a: 1; b: 2; c: 3})'
# a
# b
# c
```

## `VALUES` オブジェクトの値のストリームを取得

`VALUES(object: OBJECT): STREAM<VALUE>`

第1引数のオブジェクトの値のストリームを返します。

```shell
$ flc 'VALUES({a: 1; b: 2; c: 3})'
# 1
# 2
# 3
```

## `SUM` ストリームの要素の合計

`SUM(numbers: STREAM<NUMBER>): NUMBER`

第1引数のストリームの各要素を加算した値を返します。

```shell
$ flc 'SUM(1 .. 3)'
# 6
```

## `MIN` ストリームの最小値

`MIN(numbers: STREAM<NUMBER>): NUMBER`

第1引数のストリームの最小値を返します。

ストリームが空の場合は `NULL` を返します。

```shell
$ flc 'MIN(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)'
# 1

$ flc 'MIN(,)'
# NULL
```

## `MAX` ストリームの最大値

`MAX(numbers: STREAM<NUMBER>): NUMBER`

第1引数のストリームの最大値を返します。

ストリームが空の場合は `NULL` を返します。

```shell
$ flc 'MAX(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)'
# 9

$ flc 'MAX(,)'
# NULL
```

## `REDUCE` ストリームの要素を累積する

`REDUCE(function: VALUE, VALUE -> VALUE; stream: STREAM<VALUE>): VALUE`

`REDUCE` は `stream` の隣り合った要素を `function` によって累積し、一つの値に集約する関数です。

`REDUCE` は、しばしば部分適用によってストリームを処理する関数として用いられます。

```shell
$ flc 'REDUCE(a, b -> a + b; 1 .. 4)'
# 10

$ flc '1 .. 4 >> REDUCE[a, b -> a + b]'
# 10
```

この例は、1から4までのすべての隣接する値に対して `a + b` を適当します。

すなわち、 `1 + 2 + 3 + 4` と同じです。

---

ストリームの要素が1つしかない、もしくはストリームでない場合、その要素をそのまま返します。

```shell
$ flc '1 >> REDUCE[a, b -> a + b]'
# 1
```

---

ストリームが空の場合、 `NULL` を返します。

```shell
$ flc ', >> REDUCE[a, b -> a + b]'
# NULL
```

## `ARRAY` ストリームを配列に変換

`ARRAY(stream: STREAM<VALUE>): ARRAY<VALUE>`

第1引数のストリームの各要素を配列に変換します。

```shell
$ flc 'ARRAY(1 .. 3)'
# [1;2;3]
```

## `OBJECT` エントリーのストリームをオブジェクトに変換

`OBJECT(stream: STREAM<ARRAY<STRING; VALUE>>): OBJECT`

第1引数のストリームの各要素をエントリーとしてオブジェクトに変換します。

```shell
$ flc 'OBJECT(("a": 1), ("b": 2), ("c": 3))'
# {a:1;b:2;c:3}
```

# CLI版限定の関数

CLI版fluorite12でのみ利用可能な関数です。

## `ARGS` コマンドライン引数を取得

コマンドライン引数が配列で格納されています。

`flc` コマンドやワンライナーのソースコードは含まれません。

```shell
$ flc 'ARGS' 1 2 3
# [1;2;3]
```

## `IN` コンソールから入力

標準入力を1行ずつストリームとして取得します。

```shell
$ { echo 123; echo 456; } | flc 'IN'
# 123
# 456
```

---

ストリームは逐次的であるため、非常に大きな反復も少ないメモリ消費で行うことができます。

```shell
$ flc '1 .. 10000 | "#" * 10000' | flc 'IN | $#_ >> SUM'
# 100000000
```

## `OUT` コンソールに出力

標準出力に出力します。

```shell
$ flc '
  OUT(123)
  OUT(456)
  ; ,
'
# 123
# 456
```

---

ストリームが指定された場合、各要素を1行ずつ出力します。

```shell
$ flc '
  OUT(1 .. 3)
  ; ,
'
# 1
# 2
# 3
```

---

`OUT` 関数自体は `NULL` を返します。

# JavaScript版限定API

## `OUT` コンソールに出力

`OUT(value: VALUE): NULL`

値をWebアプリケーションごとに決められた出力欄に出力します。
