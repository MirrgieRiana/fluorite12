# 関数呼び出し `function(argument; ...)`

後置丸括弧 `(` `)` により関数を呼び出すことができます。

引数は `;` で区切ります。

```shell
$ flc 'JOIN("-"; 1, 2, 3)'
# 1-2-3
```

---

関数呼び出しにおいて、カンマ `,` は引数の区切りではなく、ストリーム結合演算子であることに注意してください。

# メソッド呼び出し `value::method(argument; ...)`

メソッド呼び出しは、値の親オブジェクトに登録された関数を、その値とともに呼び出します。

```shell
$ flc '"value"::`$&_`()'
# "value"
```

---

メソッドの本体である関数は、第1引数にメソッドを呼び出した値を、第2引数以降にメソッド呼び出しの引数列を受け取ります。

この例では、クラス用のオブジェクト `Adder` を定義し、そのインスタンスとなる子オブジェクト `adder` を生成し、 `adder` に対してメソッド `add` を呼び出しています。

メソッド `add` の本体となる関数は 、呼び出し元である `adder` の親オブジェクトである `Adder` から検索されます。

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

# 関数の部分適用 `function[argument; ...]`

関数の部分適用は、関数を実行せずに、引数の適用を部分的に予約した関数を生成します。

元の関数は、部分適用された関数の呼び出し時に、部分適用時に与えられた引数列の後に部分適用された関数の呼び出し時の引数列を受け取ります。

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

この文法は、しばしばストリームを操作する関数の扱いに役立ちます。

```shell
$ flc '1, 2, 3 >> JOIN["-"]'
# 1-2-3
```

# メソッドの部分適用 `value::method[argument; ...]`

関数と同様に、メソッド呼び出しにも部分適用ができます。

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

# メソッド参照 `value::method`

メソッド参照は、値の親オブジェクトに登録された関数に、その値を部分適用した関数を生成します。

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

```shell
$ flc '
  Adder := {
    add: this, y -> this.x + y
    new: x -> Adder{x: x}
  }
  adder := Adder.new(100)

  OUT << adder::add(23)

  OUT << (adder::add)(23)

  function := adder::add
  OUT << function(23)

  ;,
'
# 123
# 123
# 123
```

# 名前付き引数

名前付き引数専用の文法はありませんが、エントリー演算子を使って近いことが実現できます。

```shell
$ flc '
  f := a1 -> (
    params := {__(1 ~ $#__)}

    OUT << "a1=$(a1)"
    OUT << "p1=$(params.p1)"
    OUT << "p2=$(params.p2)"
    OUT << "p3=$(params.p3)"
  )

  f("arg1"; p1: "param1"; p2: "param2")

  ; ,
'
# a1=arg1
# p1=1
# p2=2
# p3=NULL
```
