# FizzBuzz

以下の条件に従って1から100までの数値を出力します。

- その数が3で割り切れる場合、 `Fizz` を出力。
- その数が5で割り切れる場合、 `Buzz` を出力。
- その数が3でも5でも割り切れる場合、 `FizzBuzz` を出力。
- それ以外の場合、その数を出力。

```shell
$ flc '1..100|&(_%%3?"Fizz":"",_%%5?"Buzz":"")||_'
# 1
# 2
# Fizz
# 4
# Buzz
# Fizz
# 7
# 8
# ...
```

---

`%%` は左辺が右辺で割り切れるか否かを返す演算子です。

`_%%3?"Fizz":""` では、値が3で割り切れた場合に `Fizz` を、そうでない場合に空文字列を得ます。

同様に、 `_%%5?"Buzz":""` で5で割り切れた場合に `Buzz` を、そうでない場合に空文字列を得ます。

それを `,` で繋いでストリーム化し、 `&` で文字列結合を行います。

その結果が空文字列だった場合、元の値を返します。

これを `1..100|` で1から100まで繰り替えします。

# フィボナッチ数列

```shell
$ flc '
  fib := n -> n < 2 ? n : fib(n - 1) + fib(n - 2)
  fib(10)
'
# 55
```

# クイックソート

```shell
$ flc '
  quicksort := list -> $#list < 2 ? list : (
    pivot  := list.0
    high   := [list() | _ >  pivot ? _ : (,)]
    middle := [list() | _ == pivot ? _ : (,)]
    low    := [list() | _ <  pivot ? _ : (,)]
    quicksort(low) + middle + quicksort(high)
  )
  quicksort([3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5])
'
# [1;1;2;3;3;4;5;5;5;6;9]
```

# マージソート

```shell
$ flc '
  m := a, b -> (
    ai := 0
    bi := 0
    [0 ~ $#a + $#b | (
      ai != $#a && (bi == $#b || a(ai) < b(bi)) ? (
        v := a(ai)
        ai = ai + 1
        v
      ) : (
        v := b(bi)
        bi = bi + 1
        v
      )
    )]
  )

  ms := l -> $#l < 2 ? l : (
    c := FLOOR($#l / 2)
    m(
      ms(l[0 ~ c])
      ms(l[c ~ $#l])
    )
  )

  ms([3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5])
'
# [1;1;2;3;3;4;5;5;5;6;9]
```

# 編集距離

```shell
$ flc '
  edit_distance := a, b -> (
    dp := [0 .. $#a + 1 | [0 .. $#b + 1 | 0]]
    0 .. $#a | i => 0 .. $#b | j => (
      dp(i + 1)(j + 1) = a(i) == b(j)
        ? dp(i)(j)
        : 1 + MIN(
          dp(i    )(j    ),
          dp(i + 1)(j    ),
          dp(i    )(j + 1),
        )
    )

    # Output Table
    [
      ["", "", b()]
      0 ~ $#dp | i => [a.(i - 1) ?: "", dp(i)()]
    ]() | OUT << _() | "$%2s(_)" >> JOIN[""]

    dp(-1)(-1)
  )
  edit_distance("kitten"; "sitting")
'
# 3
```

## 素数判定

```shell
$ flc '
  is_prime := n ->
      n < 2 ? FALSE
    : n == 2 ? TRUE
    : !(2 .. FLOOR(SQRT(n)) | n %% _)
  0 .. 4 | x => (
    OUT << 0 .. 9 | y => x * 10 + y | n => "$%4s(is_prime(n) ? n : "-")" >> JOIN[""]
  )
  ; ,
'
#    -   -   2   -   -   5   -   7   -   -
#    -  11   -  13   -   -   -  17   -  19
#    -   -   -  23   -   -   -   -   -  29
#    -  31   -   -   -   -   -  37   -   -
#    -  41   -  43   -   -   -  47   -   -
```