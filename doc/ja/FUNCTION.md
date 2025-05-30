# 関数

fluorite12では、関数は数値や文字列などと同様に、変数に代入可能な値の一種として扱われます。

具体的には、ラムダ演算子 `arguments -> formula` 等によって関数オブジェクトを生成し、それを必要に応じて変数等に代入し、関数呼び出し演算子 `function(argument; ...)` 等によって実行します。

```shell
$ flc '
  plus := x, y -> x + y
  plus(3; 4)
'
# 7
```

# ラムダ式

ラムダ演算子 `arguments -> formula` は、関数オブジェクトを生成する演算子です。

`->` の左辺に引数名を、右辺に呼び出し結果となる式を記述します。

```shell
$ flc '
  f := x -> x + 23
  f(100)
'
# 123
```

## 複数の引数を取るラムダ式

引数名をカンマ `,` で区切ることで、複数の引数を取る関数オブジェクトを生成できます。

```shell
$ flc '
  f := x, y -> x + y
  f(100; 23)
'
# 123
```

## 引数を取らないラムダ式

引数がない場合は、 `() -> formula` のように書きます。

```shell
$ flc '
  f := () -> 123
  f()
'
# 123
```

## ラムダ演算子の引数部のより詳細な仕様

### 引数部の基本形

ラムダ演算子の引数部の基本形は、引数名を `(argument; ...) -> formula` のように `;` で区切り、 `(` `)` で囲ったものです。

```shell
$ flc '
  f := (x; y) -> x + y
  f(100; 23)
'
# 123
```

### 改行時の区切り文字の省略

引数部を改行した場合、 `;` は改行で代用可能というルールに従って、区切り文字を省略できます。

```shell
$ flc '
  f := (
    x
    y
  ) -> x + y
  f(100; 23)
'
# 123
```

### 区切り文字の置き換え

引数部の区切り文字は、自由に `,` に置き換えることができます。

```shell
$ flc '
  f := (x, y) -> x + y
  f(100; 23)
'
# 123
```

### 丸括弧の省略

引数部が `;` で区切られていない場合、 `(` `)` を省略できます。

```shell
$ flc '
  f := x, y -> x + y
  f(100; 23)
'
# 123
```

## 引数列

変数 `__` には、与えられたすべての引数が配列で渡されます。

これにより、ストリームを使う以外の方法で可変長引数を受け取ることができます。

```shell
$ flc '
  f := () -> "$__"
  f(100; 20; 3)
'
# [100;20;3]
```

### ストリームを含む引数列

引数列では、引数に指定されたストリームを展開せずにそのまま受け取ることができます。

```shell
$ flc '
  f := () -> __.1
  f(NULL; 1 .. 3; NULL)
'
# 1
# 2
# 3
```

# 関数呼び出し

関数呼び出し演算子 `function(argument; ...)` は、関数オブジェクトなどの値を関数として実行する演算子です。

引数は `;` で区切ります。

```shell
$ flc 'JOIN("-"; 1, 2, 3)'
# 1-2-3
```

---

関数呼び出しにおいて、カンマ `,` は引数の区切りではなく、ストリーム結合演算子であることに注意してください。

## 異なる個数の引数による呼び出し

fluorite12では、ラムダ式で定義した引数と異なる個数の引数で関数呼び出しを行うことができます。

引数が足りない場合はNULLが渡され、多すぎる場合は単に無視されます。

```shell
$ flc '(x, y -> "$x, $y")(1)'
# 1, NULL

$ flc '(x, y -> "$x, $y")(1; 2; 3)'
# 1, 2
```

## 関数の部分適用

関数の部分適用 `function[argument; ...]` は、関数を実行せずに、「引数の適用を部分的に予約した関数」を生成します。

元の関数は、部分適用された関数の呼び出し時に、部分適用時に与えられた引数列の後に部分適用された関数の呼び出し時の引数列を受け取ります。

---

言葉にするとややこしいですが、以下の例を見るのが簡単です。

```shell
$ flc 'JOIN("-"; 1, 2, 3)'
# 1-2-3

$ flc 'JOIN["-"](1, 2, 3)'
# 1-2-3

$ flc 'JOIN["-"; 1, 2, 3]()'
# 1-2-3

$ flc 'JOIN["-"][1, 2, 3]()'
# 1-2-3
```

---

関数の部分適用は、「第1引数のみが既に指定された関数」のようなものを作れるため、しばしばストリームを操作する関数の扱いに役立ちます。

```shell
$ flc '1, 2, 3 >> JOIN["-"]'
# 1-2-3
```

# メソッド呼び出し

メソッド呼び出し演算子 `receiver::method(argument; ...)` は、 `receiver` の祖先のオブジェクトに登録された関数を、 `receiver` とともに呼び出す演算子です。

引数列の先頭に `receiver` が追加されることを除き、細かい仕様は関数呼び出し演算子と共通です。

```shell
$ flc '
  Adder := {
    add: this, y, z -> this.x + y + z
  }
  adder := Adder{x: 100}

  adder::add(20; 3)
'
# 123
```

## メソッド呼び出しの部分適用

関数呼び出しと同じく、メソッド呼び出しも部分適用ができます。

```shell
$ flc '
  Adder := {
    add: this, y, z -> this.x + y + z
  }
  adder := Adder{x: 100}

  adder::add[20](3)
'
# 123
```

## メソッド参照

メソッド参照 `receiver::method` は、値の先祖のオブジェクトに登録された関数に、その値を部分適用した関数を生成します。

```shell
$ flc '
  Adder := {
    add: this, y, z -> this.x + y + z
  }
  adder := Adder{x: 100}

  function := adder::add

  function(20; 3)
'
# 123
```

---

メソッド呼び出しは、概念的にはメソッド参照と関数呼び出しの組み合わせと等価です。

| 演算子      | 記法                                | 意味                    |
|----------|-----------------------------------|-----------------------|
| メソッド呼び出し | `receiver::method(argument; ...)` | `receiver` を挿入して関数を実行 |
| メソッド参照   | `receiver::method`                | `receiver` を挿入        |
| 関数呼び出し   | `function(argument; ...)`         | 関数の実行                 |

## NULL安全

`?::` 演算子は `receiver` が `NULL` の場合にメソッドを実行せず、代わりに `NULL` を返します。

| 演算子                 | 記法                                 |
|---------------------|------------------------------------|
| NULL安全メソッド呼び出し      | `receiver?::method(argument; ...)` |
| NULL安全メソッド呼び出しの部分適用 | `receiver?::method[argument; ...]` |
| NULL安全メソッド参照        | `receiver?::method`                |

```shell
$ flc '
  Object := {
    get_name: this -> this.name
  }

  object1 := Object{name: 1}
  object2 := NULL
  object3 := Object{name: 3}

  object1, object2, object3 | _?::get_name()
'
# 1
# NULL
# 3
```

## フォールバックメソッド

レシーバに定義されていないメソッドの参照が行われた際、 `_::_` メソッドが呼び出されます。

フォールバックメソッドは、レシーバとメソッド名を引数として受け取り、「引数を受け取ると結果を返す関数」もしくはNULLを返します。

フォールバックメソッドがNULLを返した場合、フォールバックメソッドが定義されていない場合と同じ動作になります。

```shell
$ flc -q '
  Obj := {
    `_::_`: this, method -> () -> "$method$__ called"
  }
  obj := Obj{}

  OUT << obj::apple()
  OUT << obj::banana(1)
  OUT << obj::cherry(1; 2; 3)
'
# apple[] called
# banana[1] called
# cherry[1;2;3] called
```

### フォールバックメソッドによるメソッド実装のスイッチ

フォールバックメソッドは、次のようにメソッドの実装を切り替えるスイッチとして使うことができます。

```shell
$ flc -q '
  Obj := {
    `_::_`: this, method ->
      method == "apple"  ? this::apple_impl :
      method == "banana" ? this::banana_impl :
      method == "cherry" ? this::cherry_impl :
                           NULL
    apple_impl : this          -> "apple[] called"
    banana_impl: this, x       -> "banana[$x] called"
    cherry_impl: this, x, y, z -> "cherry[$x;$y;$z] called"
  }
  obj := Obj{}

  OUT << obj::apple()
  OUT << obj::banana(1)
  OUT << obj::cherry(1; 2; 3)
  OUT << obj::durian(1; 2; 3; 4; 5; 6) !? (e => e)
'
# apple[] called
# banana[1] called
# cherry[1;2;3] called
# Method not found: {}::durian
```

# クロージャ付き関数呼び出し

クロージャ付き関数呼び出し `function ( arguments => block )` は、関数 `function` に `block` 部分を関数として渡すことで呼び出します。

`block` 部分には文も記述できます。

イベントリスナーの登録などで用いると、可読性に貢献する可能性があります。

```shell
$ flc -q '
  register := listener -> (
    event := {x: 100; y: 200}
    listener(event)
  )

  register ( event =>
    string := "X: $(event.x), Y: $(event.y)"
    OUT << string
  )
'
# X: 100, Y: 200
```

## バリエーション

クロージャ付き関数呼び出しは、メソッド呼び出しや関数の部分適用としても書くことができます。

# 名前付き引数

名前付き引数専用の文法はありませんが、エントリー演算子を使って近いことが実現できます。

```shell
$ flc -q '
  f := a1 -> (
    params := {__(1 ~ $#__)}

    OUT << "a1=$(a1)"
    OUT << "p1=$(params.p1)"
    OUT << "p2=$(params.p2)"
    OUT << "p3=$(params.p3)"
  )

  f("arg1"; p1: "param1"; p2: "param2")
'
# a1=arg1
# p1=param1
# p2=param2
# p3=NULL
```

# 拡張関数

拡張関数は、変数やマウント上で定義された関数をオブジェクトのメソッドのように呼び出すことができる仕組みです。

## 拡張関数の基本

拡張関数の基本的な構文は次の通りです。

```
`::method` := (class): this, arguments -> formula
```

---

これは、次のように分解できます。

```
function   := this, arguments -> formula
entry      := (class): function
`::method` := entry
```

`function` は、メソッドの本体となる関数です。

`entry` は、対象となるクラスと `function` の組です。

エントリー演算子 `key: value` は、 `key` が識別子の場合にそれを文字列として扱うため、クラスを参照するには丸括弧が必要です。

変数名には `::` の接頭辞が必要です。

変数名に記号を含むため、バッククォート ``` ` ``` で囲う必要があります。

---

以下は実際に拡張関数を使用する例です。

```shell
$ flc '
  Adder := {}

  `::add` := (Adder): this, y -> this.x + y

  Adder{x: 100}::add(23)
'
# 123
```

## 拡張関数のオーバーロード

拡張関数のエントリーは、エントリーの配列でもかまいません。

その場合、配列内のすべてのエントリーが有効になります。

```shell
$ flc '
  NumberAdder := {}
  StringAdder := {}

  `::add` := [
    (NumberAdder): this, y -> this.x + y
    (StringAdder): this, y -> this.x & y
  ]

  NumberAdder{x: 100}::add(23),
  StringAdder{x: "a"}::add("bc"),
'
# 123
# abc
```

## マウントを使用した拡張関数

同名の変数は後に宣言したものが上書きする関係があるため、拡張関数の定義には不便です。

そのため、マウントを使用して拡張関数を定義することもできます。

```shell
$ flc '
  NumberAdder := {}
  StringAdder := {}

  @{
    `::add`: (NumberAdder): this, y -> this.x + y
  }
  @{
    `::add`: (StringAdder): this, y -> this.x & y
  }

  NumberAdder{x: 100}::add(23),
  StringAdder{x: "a"}::add("bc"),
'
# 123
# abc
```

メソッド名の解決時には、すべての同名のエントリーが検索の対象となります。

---

マウントを使用した拡張関数も、配列によるオーバーロードが可能です。

```shell
$ flc '
  NumberAdder := {}
  StringAdder := {}

  @{
    `::add`: [
      (NumberAdder): this, y -> this.x + y
      (StringAdder): this, y -> this.x & y
    ]
  }

  NumberAdder{x: 100}::add(23),
  StringAdder{x: "a"}::add("bc"),
'
# 123
# abc
```

## メソッドと拡張関数の優先順位

同名のメソッドの定義が複数存在した場合、以下の順に優先します。

1. 変数による拡張関数
2. そのオブジェクトのメソッド
3. そのオブジェクトのフォールバックメソッド
4. マウントによる拡張関数

変数による拡張関数がオブジェクトのメソッドよりも優先されることに注意してください。

これは、変数は静的解決であり、オブジェクトのメソッドは動的解決であることに起因する仕様です。
