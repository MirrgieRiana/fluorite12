このドキュメント内では、結合優先度の高い演算子から順に記載されています。

# コメント

## 行コメント `# comment`

行コメントは `#` に続けて書くことができます。

```shell
$ flc '
  [
    1
    2 # コメント
    3
  ]
'
# [1;2;3]
```

# 因子

因子は単独で機能するリテラルや括弧類を指します。

因子は演算子の結合において、原子的な性質を持ちます。

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

## 整数リテラル `123`

1個以上の数字の列は整数リテラルになります。

```shell
$ flc '123'
# 123
```

---

0で始まる数字列であっても常に10進数として解釈されます。

```shell
$ flc '00123'
# 123
```

## 16進整数リテラル `H#123abc`

`H#` に続いて16進数を書くことができます。

`H#` の部分は大文字でなければなりませんが、16進数の部分は大文字小文字を区別しません。

```shell
$ flc 'H#FF'
# 255
```

## 浮動小数点数リテラル `1.23`

実数を「整数部 `.` 小数部」の形で書くことができます。

実数値の表現や演算には計算誤差が含まれます。

```shell
$ flc '1.5'
# 1.5
```

## 生文字列リテラル `'contents'`

生文字列リテラルは `'` `'` で囲まれた文字列であり、ほとんどの文字をそのまま解釈します。

| 文字列     | 意味      |
|---------|---------|
| `''`    | `'`     |
| CRLF    | LF      |
| CR      | LF      |
| LF      | LF      |
| `'`     | 生文字列の終端 |
| それ以外の文字 | その文字自身  |

### シングルクォートコンテント `''`

生文字列内で `'` を記述するには `''` と書きます。

```shell
$ flc " 'abc''def' "
# abc'def
```

### 改行コンテント

生文字列内には改行を含めることができますが、すべてLFに統一されます。

この性質は、ソースコードの改行コードが変更されてもプログラムの動作が変わらないことを保証します。

```shell
$ flc " 'abc
def' "
# abc
# def
```

### 文字コンテント `abcABC123`

上記を除く文字は書いたとおりに解釈されます。

これは `$` や ` \ ` も例外ではありません。

```shell
$ flc \''abc$def\nop'\'
# abc$def\nop
```

## テンプレート文字列リテラル `"contents"`

テンプレート文字列リテラルは `"` `"` で囲まれた文字列であり、エスケープや埋め込みなどの機能が使えます。

| 文字列                   | 意味           |
|-----------------------|--------------|
| `\"`                  | `"`          |
| `\$`                  | `$`          |
| ` \\ `                | ` \ `        |
| `\t`                  | タブ文字         |
| `\r`                  | CR           |
| `\n`                  | LF           |
| 上記以外の ` \ ` で始まるシーケンス | 構文エラー        |
| CRLF                  | LF           |
| CR                    | LF           |
| LF                    | LF           |
| `$` 識別子や括弧類など         | 埋め込み         |
| `$%` フォーマット指定子 括弧類    | フォーマット付き埋め込み |
| `"`                   | テンプレート文字列の終端 |
| それ以外の文字               | その文字自身       |

### エスケープシーケンスコンテント `\n`

エスケープシーケンスコンテントは ` \ ` で始まる一連のシーケンスであり、それぞれ決められた文字を表します。

エスケープシーケンスの一覧は上記の表を参照してください。

` \ ` で始まるシーケンスは将来の機能のために予約されており、不正なシーケンスは構文エラーとなります。

```shell
$ flc ' "abc\"def\\ghi\njkl" '
# abc"def\ghi
# jkl
```

### 改行コンテント

生文字列リテラルの改行コンテントと同様です。

### 埋め込みコンテント `$factor`

`$` に続いて任意の識別子、リテラル、括弧類を記述でき、その値は文字列化されたうえで文字列に埋め込まれます。

最も一般的な利用形態は、丸括弧によって任意の式を埋め込むことや、単一の識別子を埋め込むことです。

```shell
$ flc ' "value is $(100 + 20 + 3)" '
# value is 123

$ flc '
  value := 123
  "value is $value"
'
# value is 123
```

### フォーマット付き埋め込みコンテント `$%-+ 07.2f(value)`

フォーマット付き埋め込みコンテントは通常の埋め込みコンテントに似ていますが、以下の点で異なります。

- `$` に続いて、 `%` から始まるフォーマット指定子を記述します。
- 識別子などを直接埋め込むことはできず、括弧類に限られます。

```shell
$ flc ' "[$%+09.2f(123)]" '
# [+00123.00]
```

---

フォーマット指定子の文法は以下の通りです。

1. `%`
    - 必須のマジックワードです。
2. フラグ（省略可能）
    - 下表のフラグを0個以上指定することができます。
3. 幅（省略可能）
    - 文字列の長さが幅未満である場合に半角空白で補充します。
4. `.` 精度（省略可能）
    - 表示する小数の桁数を指定します。
5. 変換
    - 下表の変換を指定します。

| フラグ  | 意味                   |
|------|----------------------|
| `-`  | 左揃えにします。             |
| `+`  | 符号を常に表示します。          |
| 半角空白 | 符号のための半角空白を表示します。    |
| `0`  | スペースの代わりに `0` で埋めます。 |

| 変換 | 意味    |
|----|-------|
| d  | 10進整数 |
| x  | 16進整数 |
| f  | 10進小数 |
| s  | 文字列   |

### 文字コンテント `abcABC123`

上記を除く文字は書いたとおりに解釈されます。

## 埋め込み文字列リテラル `%>contents<%`

埋め込み文字列リテラルは `%>` `<%` で囲われた独特な見た目の文字列リテラルです。

| 文字列          | 意味         |
|--------------|------------|
| `<%%`        | `<%`       |
| CRLF         | LF         |
| CR           | LF         |
| LF           | LF         |
| `<%=` 式 `%>` | 埋め込み       |
| `<%`         | 埋め込み文字列の終端 |
| それ以外の文字      | その文字自身     |

---

埋め込み文字列リテラルは、特にHTMLコードの生成と相性がよいです。

```shell
$ flc '
  %>
    <table>
      <tr style="color: red;"><th>x</th><th>x×10</th></tr>
      <%= 1 .. 3 | x => %>
        <tr><td><%= x %></td><td><%= x * 10 %></td></tr>
      <% %>
    </table>
  <%
'
#
#    <table>
#      <tr style="color: red;"><th>x</th><th>x×10</th></tr>
#
#        <tr><td>1</td><td>10</td></tr>
#
#        <tr><td>2</td><td>20</td></tr>
#
#        <tr><td>3</td><td>30</td></tr>
#
#    </table>
#
```

<table>
  <tr style="color: red;"><th>x</th><th>x×10</th></tr>
  <tr><td>1</td><td>10</td></tr>
  <tr><td>2</td><td>20</td></tr>
  <tr><td>3</td><td>30</td></tr>
</table>

### 埋め込み文字列終了シーケンスコンテント `<%%`

埋め込み文字列内で `<%` を記述するには `<%%` と書きます。

```shell
$ flc '%>[ <%% ]<%'
#[ <% ]
```

### 改行コンテント

生文字列リテラルの改行コンテントと同様です。

### 埋め込みコンテント `<%= value %>`

`<%=` `%>` で囲うと任意の値を文字列化したうえで埋め込むことができます。

丸括弧と同様、内部で宣言された変数が外部に出ない効果があります。

```shell
$ flc ' %>value is <%= 100 + 20 + 3 %><% '
# value is 123
```

### 文字コンテント `abcABC123`

上記を除く文字は書いたとおりに解釈されます。

## 括弧 `(value)`

括弧 `(` `)` は任意の式を内部に含めることができる、何も行わない因子です。

括弧は計算順序の変更などに使うことができます。

```shell
$ flc '(1 + 2) * 10'
# 30
```

---

括弧内には、実際には複文を書くことができます。

以下のサンプルコードでは、変数 `a` を0で初期化して、括弧内で `a` に100を代入し、括弧自体は `a + 20` である120を返します。括弧の値は3と和を取られ、結果、プログラムの出力は123になります。

fluorite12では、多くのプログラミング言語とは異なり、複文に `{` `}` を使うことはできません。

```shell
$ flc '
  a := 0
  (
    a = 100
    a + 20
  ) + 3
'
# 123
```

---

括弧の内側で宣言された変数は、括弧外に影響を及ぼしません。

```shell
$ flc '
  a := 10
  (
    a := 20
    OUT(a)
    a = 30
    OUT(a)
  )
  a
'
# 20
# 30
# 10
```

## 配列リテラル `[value; ...]`

`[` `]` は配列を作るリテラルです。

括弧内には、 `;` で区切って0個以上の値を書くことができます。

```shell
$ flc '[1; 2; 3]'
# [1;2;3]
```

---

`;` で区切られた項がストリームである場合、ストリームそのものではなくストリームの各要素が配列に格納されます。

```shell
$ flc '[1 .. 3 | _ * 10; 4 .. 6 | _ * 100]'
# [10;20;30;400;500;600]
```

---

配列リテラル内での `;` の記述は柔軟です。

項の前後や中間に `;` を余計に多く書いても問題ありません。

また、改行は `;` の代わりになります。

```shell
$ flc '[
  10
  20
  ; ; ; 30; ; ;
]'
# [10;20;30]
```

## オブジェクトリテラル `{entry; ...}`

`{` `}` はオブジェクトを作るリテラルです。

括弧内には、 `;` で区切って0個以上のエントリーを書くことができます。

`{` `}` は文レベルの場所に書かれても、コードブロックではなくオブジェクトリテラルとして振舞います。

```shell
$ flc '{a: 1; b: 2}'
# {a:1;b:2}
```

---

`;` で区切られた項が `key: value` の形式であることは強制ではなく、実際には2要素の配列、およびそのストリームを受け付けます。

エントリー演算子 `:` は、オブジェクトリテラルとは本質的に無関係な、両辺を2要素の配列にする演算子です。

```shell
$ flc '{1 .. 3 | "Item$_": _ * 10; 4 .. 6 | ["Item$_", _ * 100]}'
# {Item1:10;Item2:20;Item3:30;Item4:400;Item5:500;Item6:600}
```

---

オブジェクトリテラル内での `;` の記述は柔軟です。

項の前後や中間に `;` を余計に多く書いても問題ありません。

また、改行は `;` の代わりになります。

```shell
$ flc '{
  a: 1
  b: 2
  ; ; ; c: 3; ; ;
}'
# {a:1;b:2;c:3}
```

# 後置演算子

後置演算子は基部の後ろに付与する演算子です。

後置演算子は単項のものと二項のものに分かれ、いずれも左優先結合です。

## 前置単項演算子の後置表現 `value.+` ...

すべての前置単項演算子は、 `.` に続けて書く後置演算子のバリエーションが存在します。

以下の組はすべて同じ働きをします。

```
 +A  A.+
 -A  A.-
 ?A  A.?
!!A  A.!!
 !A  A.!
 &A  A.&
$#A  A.$#
$&A  A.$&
$*A  A.$*
```

後置表現を使うと、一部の構文において丸括弧を削減し、可読性を向上できる場合があります。

## 関数呼び出し `function(argument; ...)`

後置丸括弧 `(` `)` により関数を呼び出すことができます。

引数は `;` で区切ります。

```shell
$ flc 'JOIN("-"; 1, 2, 3)'
# 1-2-3
```

---

関数呼び出しにおいて、カンマ `,` は引数の区切りではなく、ストリーム結合演算子であることに注意してください。

## メソッド呼び出し `value::method(argument; ...)`

メソッド呼び出しは、値の親オブジェクトに登録された関数を、その値とともに呼び出します。

```shell
$ flc '"value"::TO_JSON()'
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

## 配列のストリーム化 `array[]`

配列の各要素を順番にイテレートするストリームを返します。

```shell
$ flc '[1, 2, 3][]'
# 1
# 2
# 3
```

---

以下は、配列を、各要素を10倍した配列にする例です。

```shell
$ flc '[1, 2, 3][] | _ * 10 >> ARRAY'
# [10;20;30]
```

## オブジェクトのストリーム化 `object[]`

配列のストリーム化と似ていますが、こちらは各エントリーのキーと値で構成される配列のストリームを返します。

```shell
$ flc '{a: 1; b: 2; c: 3}[]'
# [a;1]
# [b;2]
# [c;3]
```

---

以下は、オブジェクトを、各キーの末尾に `z` を付け、各値を10倍したオブジェクトにする例です。

```shell
$ flc '{a: 1; b: 2; c: 3}[] | _.0 & "z": _.1 * 10 >> OBJECT'
# {az:10;bz:20;cz:30}
```

## 関数の部分適用 `function[argument; ...]`

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

## メソッドの部分適用 `value::method[argument; ...]`

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

## オブジェクトの継承 `parent{entry; ...}`

オブジェクトに `{` `}` を後置すると、そのオブジェクトを親とする子オブジェクトを生成します。

オブジェクトの継承は主にメソッドの検索に使われ、エントリーの継承は行われません。

オブジェクトの生成方法はオブジェクトリテラルと共通です。

```shell
$ flc '{a: 1; m: this -> 3}{b: 2}'
# {b:2}

$ flc '{a: 1; m: this -> 3}{b: 2}.a'
# NULL

$ flc '{a: 1; m: this -> 3}{b: 2}::m()'
# 3
```

## オブジェクトの要素アクセス `object.key`

`.` 演算子でオブジェクトの要素にアクセスできます。

```shell
$ flc '{x: 123}.x'
# 123
```

---

オブジェクトが親オブジェクトを持つ場合でも、親オブジェクトの要素は継承されません。

```shell
$ flc '{x: 123}{}.x'
# NULL
```

---

`.` の右辺に括弧を置くことで、任意の式によって参照できます。

```shell
$ flc '
  obj := {item1: 123; item2: 456}
  index := 2
  obj.("item$index")
'
# 456
```

---

`.` は、右辺が識別子の場合、それを変数ではなくキーとして解釈する性質があります。

したがって、括弧の有無によって参照するエントリーに違いが現れます。

```shell
$ flc '
  key := "item1"
  obj := {key: 123; item1: 456}
  [obj.key; obj.(key)]
'
# [123;456]
```

## 配列の要素アクセス `array.index`

`.` 演算子で配列の要素にアクセスできます。

```shell
$ flc '[10, 20, 30].1'
# 20
```

---

オブジェクトの要素アクセスと同様に、 `.` の右辺を式にすることができます。

```shell
$ flc '[10, 20, 30].(1 + 1)'
# 30
```

---

添え字が負数であった場合、配列の末尾からの位置を示します。

```shell
$ flc '[10, 20, 30].(-1)'
# 30
```

## 文字列の要素アクセス `string.index`

`.` 演算子で文字列の要素にアクセスできます。

```shell
$ flc '"abc".1'
# b
```

---

オブジェクトの要素アクセスと同様に、 `.` の右辺を式にすることができます。

```shell
$ flc '"abc".(1 + 1)'
# c
```

---

添え字が負数であった場合、配列の末尾からの位置を示します。

```shell
$ flc '"abc".(-1)'
# c
```

# べき乗系演算子

べき乗系演算子は、べき乗演算子のみで構成されます。

べき乗系演算子は右優先結合です。

## べき乗 `number ^ number`

べき乗演算子は、左辺の値を右辺の値で累乗します。

整数の整数乗の結果も浮動小数点数で返されます。

```shell
$ flc '2 ^ 3'
# 8
````

# 前置演算子

前置演算子は基部の前に付与する演算子です。

前置演算子は右優先結合です。

## 数値化 `+value`

前置 `+` 演算子は値の数値化を行います。

| 値のタイプ | 数値化の結果           |
|-------|------------------|
| NULL  | 0                |
| 数値    | その数値自身           |
| 論理値   | TRUEなら1、FALSEなら0 |
| 文字列   | 数値としてパースした結果     |
| ストリーム | 各要素の合計           |

主な用途は、文字列として表現されている数値データを内部的な数値に変換することです。

```shell
$ flc ' "+123"'
# +123

$ flc '+"+123"'
# 123
```

---

数値化は、値の `TO_NUMBER` メソッドを参照します。

オブジェクトの `TO_NUMBER` メソッドをオーバーライドすることで、数値化の処理を変更することができます。

```shell
$ flc '+{TO_NUMBER: this -> this.value * 2}{value: 100}'
# 200
```

## 負の数値化 `-value`

前置 `-` 演算子は数値化演算子と似ていますが、その値を負にします。

この演算子は「任意の値を数値に変換する処理」と「数値を負にする処理」を同時に行います。

```shell
$ flc '-"123"'
# -123

$ flc '-(100 + 20 + 3)'
# -123
```

## 論理値化 `?value`

前置 `?` 演算子は値の論理値化を行います。

```shell
$ flc '?1'
# TRUE

$ flc '?0'
# FALSE
```

---

値のタイプによってどのように論理値化が行われるかが異なります。

| タイプ    | 動作               |
|--------|------------------|
| NULL   | 常に偽              |
| 数値     | 0でない場合に真         |
| 論理値    | それ自身を返す          |
| 文字列    | 空文字列でない場合に真      |
| ストリーム  | いずれかの要素が真の場合に真   |
| 配列     | 常に真              |
| オブジェクト | オーバーライドしない場合、常に真 |

---

論理値化は、値の `TO_BOOLEAN` メソッドを参照します。

オブジェクトの `TO_BOOLEAN` メソッドをオーバーライドすることで、論理値化の処理を変更することができます。

```shell
$ flc '?{TO_BOOLEAN: this -> this.value > 100}{value: 50}'
# FALSE

$ flc '?{TO_BOOLEAN: this -> this.value > 100}{value: 200}'
# TRUE
```

## 否定論理値化 `!value`

前置 `!` 演算子は論理値化演算子と似ていますが、否定の値を得ます。

```shell
$ flc '!TRUE'
# FALSE

$ flc '!FALSE'
# TRUE
```

## 文字列化 `&value`

前置 `&` 演算子は、値の文字列表現を得るのに使います。

```shell
$ flc '&[1..3]'
# [1;2;3]
```

---

値の種類ごとの文字列表現は以下の通りです。

- NULLは `NULL` になります。
- 整数値の文字列表現は、その整数値の10進数表現になります。
- TRUEは `TRUE` 、FALSEは `FALSE` になります。
- 文字列の文字列表現は、その文字列自身です。
- 配列の文字列表現は、 `[1;2;3]` のように、 `;` で区切って `[` `]` で囲んだものです。
- オブジェクトの文字列表現は、 `{a:1;b:2}` のように、キーと値を `:` で区切り、そのエントリーを `;` で区切り、全体を `{` `}` で囲んだものです。

---

文字列化の実態は、値のTO_STRINGメソッドを呼び出すことで行われます。

オブジェクトのTO_STRINGメソッドをオーバーライドすることで、文字列化の処理を変更することができます。

```shell
$ flc '&{TO_STRING: this -> "The value is $(this.value)"}{value: 100}'
# The value is 100
```

## 長さの取得 `$#value`

前置 `$#` 演算子は、値の「長さ」を取得します。

```shell
$ flc '$#"123"'
# 3

$ flc '$#[1, 2, 3]'
# 3

$ flc '$#{a: 1; b: 2; c: 3}'
# 3
```

---

この演算子は値のタイプによって異なるものを返します。

| タイプ    |                     |
|--------|---------------------|
| 文字列    | UTF16による文字の長さ       |
| 配列     | 要素数                 |
| オブジェクト | 親オブジェクトを無視した、エントリの数 |

## スロー `!!error`

fluorite12では、例外のスローを前置 `!!` 演算子で行います。

スローする値はどのようなタイプの値であっても構いません。

スローした値は、キャッチした際に渡されます。

```shell
$ flc '!!"12345" !? e => "Error ($e)"'
# Error (12345)
```

# 乗除算系演算子

乗除算系演算子は、乗除算に類する動作を行う演算子で構成されます。

乗除算系演算子は左優先結合です。

## 乗算 `number * number`

乗算演算子は、2つの値を乗算します。

```shell
$ flc '2 * 3'
# 6
```

---

文字列の乗算はその文字列を繰り返します。

```shell
$ flc '"abc" * 3'
# abcabcabc
```

---

配列の乗算はその配列を繰り返します。

```shell
$ flc '[1, 2, 3] * 3'
# [1;2;3;1;2;3;1;2;3]
```

## 除算 `number / number`

除算演算子は、2つの値を除算します。

```shell
$ flc '6 / 3'
# 2
```

---

整数同士の除算でも、結果は浮動小数点数で返されます。

```shell
$ flc '7 / 4'
# 1.75
```

## 剰余 `number % number`

剰余演算子は、2つの値を除算し、その余りを返します。

```shell
$ flc '7 % 4'
# 3
```

---

小数も扱うことができます。

```shell
$ flc '1.75 % 0.5'
# 0.25
```

## 整除性 `integer %% integer`

左辺の値が右辺の値で割り切れるかどうかを返します。

```shell
$ flc '7 %% 4'
# FALSE
```

---

小数も扱うことができます。

```shell
$ flc '1.5 %% 0.5'
# TRUE

$ flc '1.75 %% 0.5'
# FALSE
```

# 加減算系演算子

加減算系演算子は、加減算に類する動作を行う演算子で構成されます。

加減算系演算子は左優先結合です。

## 加算 `number + number`

加算演算子は、2つの値を加算します。

```shell
$ flc '1 + 2'
# 3
```

---

左辺が配列である場合、右辺の配列と連結します。

```shell
$ flc '[1; 2] + [3; 4]'
# [1;2;3;4]
```

## 減算 `number - number`

減算演算子は、2つの値を減算します。

```shell
$ flc '3 - 1'
# 2
```

## 文字列の結合 `string & string`

文字列の連結を行うには `&` 演算子を使います。

```shell
$ flc '"abc" & "def"'
# abcdef
```

# 範囲系演算子

## 閉区間 `start .. end`

閉区間演算子は、左辺から右辺までの整数の範囲のストリームを生成します。

end自身はストリームに含まれます。

```shell
$ flc '1 .. 3'
# 1
# 2
# 3
```

---

左辺が右辺よりも大きい場合、カウントダウンを行います。

```shell
$ flc '3 .. 1'
# 3
# 2
# 1
```

## 半開区間 `start ~ end`

半開区間演算子は、左辺から右辺の1つ手前までの整数の範囲のストリームを生成します。

end自身はストリームに含まれません。

```shell
$ flc '1 ~ 3'
# 1
# 2
```

---

半開区間演算子は、閉区間演算子とは異なり、右辺が左辺よりも大きい場合、空のストリームを生成します。

```shell
$ flc '[3 ~ 1]'
# []
```

# ストリーム結合系演算子

## ストリームの結合 `items, ...`

演算子 `,` は、左右の要素またはストリームを結合したストリームを生成します。

fluorite12では、ラムダ演算子の左辺のような特殊な場所でない限り、 `,` は引数や配列要素等の区切りではなくストリーム結合演算子として解釈されます。

```shell
$ flc '1, 2 .. 4, 5'
# 1
# 2
# 3
# 4
# 5
```

---

ストリーム結合演算子は余計に多く書いても無視されます。

```shell
$ flc ', , 1, , , , 2, , '
# 1
# 2
```

---

ストリーム結合演算子のみを記述することができ、その場合は空ストリームを生成します。

```shell
$ flc '[,]'
# []
```

---

flcコマンドはデフォルトの挙動で与えられたソースコードの戻り値を出力しますが、空のストリームに対しては何も出力しないため、flcコマンドの出力を抑制するのに使われることもあります。

```shell
$ flc '"何らかの副作用を伴う処理"; ,'
```

# ストリーム系演算子

ストリーム系演算子は、ストリームの加工や代入などを行う演算子です。

## ストリーム系演算子の簡単な紹介

結合優先度についての解説のため、ストリーム系に属する演算子を軽く紹介します。

---

パイプ `stream | argument => formula` は、 `stream` の各要素について `formula` を適用したストリームを得ます。

`formula` 内では、 `argument` でその要素を参照できます。

```shell
$ flc '1, 2, 3 | x => x * 10'
# 10
# 20
# 30
```

---

実行パイプ `value >> function` は、 `function` に `value` を渡して実行します。

```shell
$ flc '1, 2, 3 >> REVERSE'
# 3
# 2
# 1
```

---

変数宣言 `variable := value` は、変数 `variable` を宣言しつつ、その値を `value` で初期化します。

```shell
$ flc '
  x := 123
  x
'
# 123
```

---

代入 `variable = value` は、変数 `variable` に `value` を代入します。

```shell
$ flc '
  x := 123
  x = 456
  x
'
# 456
```

## 結合優先度について

ストリーム系演算子は、実用上の理由から、以下の文法で表される複雑な結合規則を持っています。

```
ストリームノード :=
    ストリーム結合ノード  代入系演算子  ストリームノード
  / ストリーム結合ノード  ストリーム後方付加部*

ストリーム後方付加部 :=
    パイプ演算子  パイプ右辺
  / 実行パイプ演算子  実行パイプ右辺

実行パイプ右辺 :=
    ストリーム結合ノード  代入系演算子  ストリームノード
  / ストリーム結合ノード

パイプ右辺 :=
    ストリーム結合ノード  パイプ系演算子  パイプ右辺
  / ストリーム結合ノード  代入系演算子  ストリームノード
  / ストリーム結合ノード
```

以下では、ストリーム系演算子の文法を例を用いて解説します。

---

パイプ系演算子は、原則として右優先結合です。

このため、前段の変数を後段から参照することができます。

```shell
$ flc '10, 20 | x => 3, 4 | y => x + y'
# 13
# 14
# 23
# 24

$ flc '10, 20 | x => (3, 4 | y => x + y)'
# 13
# 14
# 23
# 24
```

---

実行パイプ系演算子は、左側にあるパイプ系・実行パイプ系演算子をまとめて取ります。

これにより、様々に加工したストリームの全体を関数に入力することができます。

```shell
$ flc '10, 20 | x => 3, 4 | y => x + y >> JOIN["-"]'
# 13-14-23-24
$ flc '(10, 20 | x => 3, 4 | y => x + y) >> JOIN["-"]'
# 13-14-23-24
```

実行パイプ系演算子は他の実行パイプ系演算子も左辺にまとめて取ります。

また、実行パイプ系演算子による結果を、さらに別のパイプ系演算子で加工出来ます。

```shell
$ flc '1 .. 3 | _ * 10 >> REVERSE | _ + 5 >> JOIN["-"]'
# 35-25-15

$ flc '((1 .. 3 | _ * 10) >> REVERSE | _ + 5) >> JOIN["-"]'
# 35-25-15
```

---

代入系演算子は、右辺を左辺から分離します。

代入系演算子の右辺にある実行パイプ系演算子は、代入系演算子の左辺には影響を及ぼしません。

```shell
$ flc '
  pow2_joiner := stream -> stream | x => x * x >> JOIN["-"]
  pow2_joiner(1, 2, 3)
'
# 1-4-9
```

以下では、関数 `setter` を呼び出すと、与えた数値に36を足して平方根を取った値を変数 `variable` に代入します。

`SQRT` の左にある `>>` は、その左の `=` の手前までを左辺に取ります。

```shell
$ flc '
  variable := NULL
  setter := x -> x + 36 | x2 => variable = x2 >> SQRT
  setter(64)
  variable
'
# 10.0
```

## 変数の宣言（代入系） `variable := value`

変数宣言演算子は、書かれたスコープ内で変数を宣言しつつ、右辺の値で初期化します。

fluorite12では、変数は宣言されると同時に初期化される必要があります。

```shell
$ flc '
  a := 123
  OUT(a)
  ; ,
'
# 123
```

---

宣言された変数は、そのスコープ内でのみ有効です。

`(` `)` など、スコープを生成する演算子内で宣言された変数は、そのスコープを抜けると破棄されます。

```shell
$ flc '
  a := 10
  OUT(a)
  (
    a := 20
    OUT(a)
    a = 30
    OUT(a)
  )
  OUT(a)
  ; ,
'
# 10
# 20
# 30
# 10
```

## 代入（代入系） `variable = value`

代入演算子は、左辺の変数に右辺の値を代入します。

代入先の変数はすでに宣言されている必要があります。

```shell
$ flc '
  a := 123
  OUT(a)
  a = 456
  OUT(a)
  ; ,
'
# 123
# 456
```

## 配列の要素への代入（代入系） `array[index] = value`

左辺が配列の要素の参照であった場合、その要素に右辺の値を代入します。

```shell
$ flc '
  array := [1, 2, 3]
  OUT(array)
  array[1] = 4
  OUT(array)
  ; ,
'
# [1;2;3]
# [1;4;3]
```

## ラムダ演算子（代入系） `arguments -> formula`

ラムダ演算子は関数オブジェクトを作る演算子です。

`->` の左辺に引数名を、右辺に戻り値である式を記述します。

```shell
$ flc '
  func := x, y -> x * y
  func(3; 4)
'
# 12
```

---

ラムダ式の引数の基本形は `(argument; ...)` のように `;` で区切り、 `(` `)` で囲ったものです。

シンタックスシュガーとして、 `;` の代わりに `,` で区切ることができ、その場合は `(` `)` を省略できます。

その結果、引数列を以下のように書くことができます。

```
(x; y) -> NULL
(x, y) -> NULL
x, y -> NULL

(x) -> NULL
x -> NULL

() -> NULL
```

---

変数 `__` には、与えられた引数列が配列で渡されます。

これにより、ストリームを使う以外の方法で可変長引数を受け取ることができます。

```shell
$ flc '(() -> __)(1; 2; 3; 4; 5)'
# [1;2;3;4;5]
```

引数列では、引数に指定されたストリームを展開せずにそのまま受け取ることができます。

```shell
$ flc '(() -> &[__.1])(1 .. 3; 4 .. 6; 7 .. 9)'
# [4;5;6]
```

---

fluorite12では、引数の個数の異なる関数呼び出しを行うことができます。

引数が足りない場合はNULLが渡され、多すぎる場合は無視されます。

```shell
$ flc '(x, y -> y)(1)'
# NULL
$ flc '(x, y -> y)(1; 2; 3)'
# 2
```

## エントリー演算子（代入系） `key: value`

エントリー演算子は、両辺を要素とする2要素の配列を生成する演算子です。

```shell
$ flc 'a: 1'
# [a;1]
```

---

左辺が識別子の場合、同名の変数があっても、変数を参照するのではなく文字列として扱います。

```shell
$ flc '
  a := "b"
  a: 1
'
# [a;1]
```

---

左辺で変数を参照したい場合は、括弧で囲むことで文字列として扱われることを回避できます。

```shell
$ flc '
  a := "b"
  (a): 1
'
# [b;1]
```

---

エントリー演算子は配列リテラルとは異なり、ストリームを展開せず、常に2要素の配列を生成します。

```shell
$ flc '["key"; 1 .. 3]'
# [key;1;2;3]

$ flc 'key: 1 .. 3'
# [key;123]
```

---

この演算子はオブジェクトを生成する際に有用です。

```shell
$ flc '
  {
    a: 1
    b: 2
  }
'
# {a:1;b:2}
```

## 左実行パイプ（代入系） `function << value`

左実行パイプは、右辺の値を左辺の関数の第1引数に指定して呼び出します。

右実行パイプの左右が逆のバージョンですが、結合優先度が代入系扱いです。

使い方によっては可読性に貢献する可能性を秘めています。

```shell
$ flc '
  OUT << "Hello, World"
  ; ,
'
# Hello, World
```

## キャッチ演算子（代入系） `try !? catch`

キャッチ演算子は、例外が発生した場合にその例外をキャッチする演算子です。

`!?` の左辺に実行を試す式、右辺に例外をキャッチした場合の処理を記述します。

受け取った例外の値は変数 `_` として右辺に渡されます。

例外の値は、オブジェクトや文字列だけでなく、どのようなタイプの値でもある可能性があります。

```shell
$ flc '"OK" !? "Error ($_)"'
# OK

$ flc '!!12345 !? "Error ($_)"'
# Error (12345)
```

---

右辺を `variable => formula` の形にすることで、例外の値を受け取る変数名を変更できます。

```shell
$ flc '!!12345 !? e => "Error ($e)"'
# Error (12345)
```

## パイプ（パイプ系） `stream | formula`

パイプ演算子 `|` は、左辺のストリームの各値に対して右辺を評価し、そのフラットなストリームを返します。

右辺では、変数 `_` によって左辺の各要素の値を得ることができます。

```shell
$ flc '1 .. 3 | _, _ * 10'
# 1
# 10
# 2
# 20
# 3
# 30
```

---

左辺がストリームでない場合、右辺の返却値はストリームで改めてラッピングされることなく、そのままの型で返されます。

```shell
$ flc '(5 | _ * 10) + 7'
# 57
```

---

右辺に渡される変数は `=>` によって変更できます。

```shell
$ flc '5 | x => x * 10'
# 50
```

---

パイプ演算子をループ構文のように使うこともできます。

```shell
$ flc '
  x := 0
  1 .. 10 | (
    x = x + _
  )
  x
'
# 55
```

## 右実行パイプ（実行パイプ系） `value >> function`

右実行パイプは、左辺の値を右辺の関数の第1引数に指定して呼び出します。

```shell
$ flc '1 .. 3 >> JOIN["-"]'
# 1-2-3
```

---

この演算子はストリームを扱う関数の実行に便利です。

```shell
$ flc '"1+2+3" >> SPLIT["+"] | +_ * 2 >> JOIN["-"]'
# 2-4-6
```

# ルートノード

ルートノードではファイル全体や括弧の直下など、どんな式でも書ける場所のルールを定義します。

## 複文 `runner; ...; getter`

複文は、0個以上の文と複文自体の値となる末尾式を `;` で区切って繋いだものです。

以下のサンプルコードにおいて、最後の `a` の行のみが式（getter）扱いで、その上の3行は文（runner）扱いです。

以下のサンプルコードでは、変数 `a` を宣言しつつ10を代入し、10が格納されている `a` の値を出力、次に `a` に20を代入し、末尾式として20の格納されている `a` を指定しています。

```shell
$ flc '
  a := 10;
  OUT(a);
  a = 20;
  a
'
# 10
# 20
```

---

末尾式は省略することができます。

その場合、文が末尾式でないことを示すために、末尾に `;` が必要となる場合があります。

末尾式が省略された複文の値は常に `NULL` となります。

```shell
$ flc '1 + 2;'
# NULL

$ flc ''
# NULL
```

---

複文内での `;` の記述は、末尾を除いて柔軟です。

項の前や中間に `;` を余計に多く書いても問題ありません。

末尾式の後にはセミコロンを書くことはできません。

また、改行は `;` の代わりになります。

```shell
$ flc '
  a := 10
  OUT(a)
  ; ; ; a = 20; ; ;
  a
'
# 10
# 20
```

# 組み込み定数

組み込み定数は、コード上で定義することなく利用可能な、言語機能に付属する定数です。

組み込み定数は大文字および `_` のみを使って定義されます。

組み込みの関数も組み込み定数と同じメカニズムによって提供されます。

## 組み込みオブジェクトのクラス定数

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

## 定数

各種、特別な値を表す定数です。

| 定数      | 意味      |
|---------|---------|
| `NULL`  | NULL値   |
| `TRUE`  | 真       |
| `FALSE` | 偽       |
| `EMPTY` | 空のストリーム |

---

数学系の組み込み定数です。

| 定数   | 意味    |
|------|-------|
| `PI` | 円周率   |
| `E`  | ネイピア数 |

## 数学系関数

### `FLOOR` 小数点以下切り捨て

`FLOOR(number: NUMBER): INTEGER`

第1引数の数値を、値が小さい方の整数に丸めます。

```shell
$ flc 'FLOOR(1.5)'
# 1
```

### `SQRT` 平方根の取得

`SQRT(number: NUMBER): NUMBER`

第1引数の正の平方根を返します。

```shell
$ flc 'SQRT(100.0)'
# 10.0
```

## ストリーム系関数

### `REVERSE` ストリームを逆順にする

`REVERSE(stream: STREAM<VALUE>): STREAM<VALUE>`

第1引数のストリームの要素を逆順にしたストリームを返します。

```shell
$ flc 'REVERSE(1 .. 3)'
# 3
# 2
# 1
```

### `JOIN` ストリームを文字列に連結

`JOIN(separator: VALUE; stream: VALUE): STRING`

第2引数のストリームの各要素を第1引数のセパレータで連結した文字列を返します。

```shell
$ flc 'JOIN("|"; "a", "b", "c")'
# a|b|c
```

---

セパレータやストリームの各要素は文字列化されます。

```shell
$ flc 'JOIN(0; 1, "b", {TO_STRING: _ -> "c"}{})'
# 10b0c
```

---

部分適用とともに用いることで、パイプチェーンに組み込みやすくなります。

```shell
$ flc '1 .. 3 | _ * 10 >> JOIN["|"]'
# 10|20|30
```

### `SPLIT` 文字列をストリームに分割

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

### `KEYS` オブジェクトのキーのストリームを取得

`KEYS(object: OBJECT): STREAM<STRING>`

第1引数のオブジェクトのキーのストリームを返します。

```shell
$ flc 'KEYS({a: 1; b: 2; c: 3})'
# a
# b
# c
```

### `VALUES` オブジェクトの値のストリームを取得

`VALUES(object: OBJECT): STREAM<VALUE>`

第1引数のオブジェクトの値のストリームを返します。

```shell
$ flc 'VALUES({a: 1; b: 2; c: 3})'
# 1
# 2
# 3
```

### `SUM` ストリームの要素の合計

`SUM(numbers: STREAM<NUMBER>): NUMBER`

第1引数のストリームの各要素を加算した値を返します。

```shell
$ flc 'SUM(1 .. 3)'
# 6
```

### `ARRAY` ストリームを配列に変換

`ARRAY(stream: STREAM<VALUE>): ARRAY<VALUE>`

第1引数のストリームの各要素を配列に変換します。

```shell
$ flc 'ARRAY(1 .. 3)'
# [1;2;3]
```

### `OBJECT` エントリーのストリームをオブジェクトに変換

`OBJECT(stream: STREAM<ARRAY<STRING; VALUE>>): OBJECT`

第1引数のストリームの各要素をエントリーとしてオブジェクトに変換します。

```shell
$ flc 'OBJECT(("a": 1), ("b": 2), ("c": 3))'
# {a:1;b:2;c:3}
```

## CLI版限定の関数

CLI版fluorite12でのみ利用可能な関数です。

### `ARGS` コマンドライン引数を取得

コマンドライン引数が配列で格納されています。

`flc` コマンドやワンライナーのソースコードは含まれません。

```shell
$ flc 'ARGS' 1 2 3
# [1;2;3]
```

### `IN` コンソールから入力

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

### `OUT` コンソールに出力

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

# コマンドラインツール

## `flc`

`flc` は第1引数に渡されたfluorite12のコードをその場で実行するコマンドです。

`flc` コマンドはコードの戻り値を自動的に出力するため、結果の出力のために明示的に `OUT` 関数などを使う必要はありません。

```shell
$ flc '1 + 2'
# 3
```

---

戻り値の出力の際には文字列化が行われます。

```shell
$ flc '{TO_STRING: _ -> "Hello, World"}{}'
# Hello, World
```

---

コードの値がストリームだった場合、各要素を1行ずつ出力します。

```shell
$ flc '1 .. 3'
# 1
# 2
# 3
```

---

何も出力させたくない場合は、コードの戻り値を空ストリームにします。

```shell
$ flc '1 + 2; ,'
```

# サンプルコード

## フィボナッチ数列

```shell
$ flc '
  fib := n -> n < 2 ? n : fib(n - 1) + fib(n - 2)
  fib(10)
'
# 55
```

## クイックソート

```shell
$ flc '
  quicksort := list -> $#list < 2 ? list : (
    pivot  := list.0
    high   := [list[] | _ >  pivot ? _ : (,)]
    middle := [list[] | _ == pivot ? _ : (,)]
    low    := [list[] | _ <  pivot ? _ : (,)]
    quicksort(low) + middle + quicksort(high)
  )
  quicksort([3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5])
'
# [1;1;2;3;3;4;5;5;5;6;9]
```