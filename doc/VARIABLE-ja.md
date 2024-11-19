## 識別子 `identifier`

識別子は組み込み定数や変数、引数などを指し表す文字列です。

```shell
$ flc 'TRUE'
# TRUE

$ flc 'x := 10; y := 2; x + y'
# 12

$ flc '(x, y -> x + y)(10; 2)'
# 12
```

---

識別子は1文字以上の英数字もしくはアンダースコア `_` であり、かつ先頭は数字であってはいけません。

`[A-Za-z_][A-Za-z_0-9]*`

## クォート識別子

``` ` ` ``` で囲った識別子はクォート識別子と呼ばれます。

識別子とクォート識別子は、書き方が違うだけで同じ識別子を指します。

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

# マウント

マウントは、変数が宣言されていない識別子が特殊なテーブルから値を参照することができる仕組みです。

マウントはライブラリを利用する際によく用いられます。

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

  lib.drink = "coffee"

  drink
'
# No such mount entry: drink
```

また、同様の理由でマウントに対してメソッドを呼び出すこともできません。

## マウントは複数できる

既にマウントが行われた状態で更にマウントを行うと、両方のエントリーがマウントされた状態になります。

```shell
$ flc '
  favorite_fruit := {
    fruit: "apple"
  }
  @favorite_fruit

  favorite_drink := {
    drink: "coffee"
  }
  @favorite_drink

  "My favorite fruit is $fruit and drink is $drink"
'
# My favorite fruit is apple and drink is coffee
```

## マウントは既存のマウントを上書きする

同じ名前に対して複数のマウントが行われた場合、あとのものが優先されます。

```shell
$ flc '
  favorite_foods := {
    fruit: "apple"
    bread: "epi"
  }
  @favorite_foods

  favorite_plants := {
    vegetable: "tomato"
    fruit: "orange"
  }
  @favorite_plants

  "My favorite fruit is $fruit"
'
# My favorite fruit is orange
```

## マウントは変数と同様のスコープを持つ

マウントは、スコープを抜けると解除されます。

マウントのスコープは、マウント演算子によってマウントされてから、その階層の括弧類を抜けるまでです。

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
