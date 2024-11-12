# 論理値化 `?value`

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

# 否定論理値化 `!value`

前置 `!` 演算子は論理値化演算子と似ていますが、否定の値を得ます。

```shell
$ flc '!TRUE'
# FALSE

$ flc '!FALSE'
# TRUE
```

# 条件演算子

## 条件演算子 `condition ? then : else`

条件演算子は、条件によって2つの値のうちどちらかを返します。

```shell
$ flc 'TRUE ? "Yes" : "No"'
# Yes

$ flc 'FALSE ? "Yes" : "No"'
# No
```

---

条件演算子は入れ子にしたり、演算子の前で改行することができます。

```shell
$ flc '
  get_name := is_parent, is_man ->
    is_parent
      ? is_man
        ? "King"
        : "Queen"
      : is_man
        ? "Prince"
        : "Princess"

  get_name(TRUE; TRUE),
  get_name(TRUE; FALSE),
  get_name(FALSE; TRUE),
  get_name(FALSE; FALSE),
'
# King
# Queen
# Prince
# Princess
```

## エルビス演算子 `value ?: default`

エルビス演算子は、 `value` がNULLである場合に `default` を返す演算子です。

```shell
$ flc '"Orange" ?: "Apple"'
# Orange

$ flc 'NULL ?: "Apple"'
# Apple
```
