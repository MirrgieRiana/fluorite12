# 識別子

識別子 `identifier` は、変数、組み込み定数、組み込み関数、引数などを指し表すための文字列です。

```shell
$ flc '
  variable := 10
  variable
'
# 10

$ flc 'MATH.PI'
# 3.141592653589793

$ flc 'SQRT(4)'
# 2.0

$ flc '
  function := x, y, z -> x + y + z
  function(100; 20; 3)
'
# 123
```

## 識別子に使用できる文字

識別子は1文字以上の英数字もしくはアンダースコア `_` であり、かつ先頭は数字であってはいけません。

正規表現で表すと、以下のようになります。

```
[A-Za-z_][A-Za-z_0-9]*
```

## クォート識別子

``` ` ` ``` で囲った識別子はクォート識別子と呼ばれます。

識別子とクォート識別子は、書き方が違うだけで同じ識別子を表します。

```shell
$ flc '
  `variable` := 10
  `variable`
'
# 10

$ flc '
  variable := 10
  `variable`
'
# 10
```

## 記号を含むクォート識別子

クォート識別子は記号を含むこともできます。

```shell
$ flc '
  `#` := 10
  `#`
'
# 10
```

# 変数

変数は、識別子によって値に名前を付けて格納・参照するための仕組みです。

```shell
$ flc '
  x := 100
  y :=  20
  z :=   3

  x + y + z
'
# 123
```

## 変数の宣言

変数の宣言には、変数宣言演算子 `variable := value` を使います。

変数宣言演算子は、書かれたスコープ内で変数を宣言しつつ、右辺の値で初期化します。

fluorite12では、変数は宣言と同時に何らかの値で初期化する必要があります。

```shell
$ flc '
  x := 10
  x
'
# 10
```

## 変数への値の代入

変数に値を代入するには、代入演算子 `variable = value` を使います。

代入先の変数はすでに宣言されている必要があります。

```shell
$ flc '

  x := 10
  OUT << x

  x = 123
  OUT << x

  ; ,
'
# 10
# 123
```

## 変数のスコープ

宣言された変数は、宣言以降、そのスコープ内でのみ有効です。

`( )` など、スコープを生成する演算子内で宣言された変数は、そのスコープを抜けると破棄されます。

```shell
$ flc '

  x := "A (outer initial value)"
  OUT << x

  (
    x = "B (outer assigned value)"
    OUT << x
  
    x := "C (inner initial value)"
    OUT << x

    x = "D (inner assigned value)"
    OUT << x
  )

  OUT << x

  ; ,
'
# A (outer initial value)
# B (outer assigned value)
# C (inner initial value)
# D (inner assigned value)
# B (outer assigned value)
```

## 変数宣言演算子の右辺からの自身の参照

変数宣言演算子の右辺では、その変数自身を参照することができます。

この性質は、再帰関数を作るときに便利です。

```shell
$ flc '
  factorial := n -> n == 0 ? 1 : n * factorial(n - 1)
  factorial(5)
'
# 120
```

# マウント

マウントは、変数が宣言されていない識別子が特殊なテーブルから値を参照することができる仕組みです。

マウントはライブラリを利用する際によく用いられます。

`TRUE` `NULL` などの組み込み定数、 `JOIN` `SQRT` などの組み込み関数は、すべてマウントの仕組みを利用して提供されています。

```shell
$ flc '
  lib := {
    fruit: "apple"
  }

  @lib

  fruit
'
# apple
```

## マウント演算子

マウント演算子 `@object` は、オブジェクト `object` の内容を現在の位置にマウントします。

定義されていない変数にアクセスしようとしたとき、マウントされているエントリーから値を探します。

```shell
$ flc '
  @{
    fruit: "apple"
  }

  fruit
'
# apple
```

`object` はオブジェクトである必要があります。

## マウント演算子はオブジェクトの中身をマウントする

マウントはオブジェクトそのものではなく、オブジェクトの各エントリーについて行われます。

マウント後に行われたオブジェクトへの改変は、マウント状態には反映されません。

```shell
$ flc '
  lib := {
    fruit: "apple"
  }

  @lib

  lib.fruit = "orange"

  fruit
'
# apple
```

また、同様の理由でマウントを使ったメソッド呼び出しもできません。

## マウントは多重にできる

既にマウントが行われた状態で更にマウントを行うと、両方のエントリーがマウントされた状態になります。

```shell
$ flc '
  @{
    fruit: "apple"
  }

  @{
    drink: "coffee"
  }

  "fruit=$fruit, drink=$drink"
'
# fruit=apple, drink=coffee
```

## マウントは既存のマウントを上書きする

同じ名前に対して複数のマウントが行われた場合、あとのものが優先されます。

```shell
$ flc '
  @{
    fruit: "apple"
    bread: "epi"
  }

  @{
    vegetable: "tomato"
    fruit: "orange"
  }

  "fruit=$fruit, bread=$bread, vegetable=$vegetable"
'
# fruit=orange, bread=epi, vegetable=tomato
```

## マウントは変数と同様のスコープを持つ

マウントは、スコープを抜けると解除されます。

マウントのスコープは、変数と同様に、マウント演算子によってマウントされてからその階層の括弧類を抜けるまでです。

```shell
$ flc '
  @{
    fruit: "apple"
  }

  (
    OUT << fruit

    @{
      fruit: "banana"
    }
    
    OUT << fruit
  )

  OUT << fruit

  ; ,
'
# apple
# banana
# apple
```

## 宣言済みの変数はマウントに優先する

同じ名前で変数とマウントの両方にアクセス可能な場合、宣言順序に関係なく常に変数が優先されます。

```shell
$ flc '
  fruit := "apple"

  @{
    fruit: "banana"
  }

  fruit
'
# apple
```

この仕様は、意図しないマウントの利用を防ぐために設けられています。
