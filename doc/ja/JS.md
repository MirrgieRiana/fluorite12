# JavaScript上での動作

fluorite12はJavaScript上での動作をサポートしています。

# JavaScriptオブジェクトの扱い

JavaScript側のオブジェクトは、 `JS_OBJECT` クラスで表現されます。

# 暗黙の型変換

関数呼び出しなどいくつかの状況では、fluorite12とJavaScriptの間で自動的に変換が行われます。

| 変換元<br>fluorite12 | 変換先<br>JavaScript |
|-------------------|-------------------|
| `JS_OBJECT`       | そのままの値            |
| `INT`             | `Number`          |
| `DOUBLE`          | `Number`          |
| `STRING`          | `String`          |
| `BOOLEAN`         | `Boolean`         |
| `ARRAY`           | `Array`           |
| `NULL`            | `null`            |
| `FUNCTION`        | `Function`        |
| それ以外              | 非対応による例外          |

| 変換元<br>JavaScript | 変換先<br>fluorite12 |
|-------------------|-------------------|
| そのままの値            | `JS_OBJECT`       |
| 整数 `Number`       | `INT`             |
| 小数 `Number`       | `DOUBLE`          |
| `String`          | `STRING`          |
| `Boolean`         | `BOOLEAN`         |
| `Array`           | `ARRAY`           |
| `null`            | `NULL`            |
| `undefined`       | `NULL`            |
| それ以外              | `JS_OBJECT`       |

# 関数呼び出し

JavaScriptの関数は、fluorite12の関数のように呼び出すことができます。

引数と戻り値の間で暗黙の型変換が行われます。

# JavaScript版限定組み込み関数

JavaScript版fluorite12でのみ利用可能な関数です。

## `JS_OBJECT` JavaScriptオブジェクトのクラス

`JS_OBJECT` はJavaScriptのオブジェクト全般を表すクラスです。

## `OUT` コンソールに出力

`OUT(value: VALUE): NULL`

値をWebアプリケーションごとに決められた出力欄に出力します。

## `WINDOW` windowオブジェクトの取得

`WINDOW: JS_OBJECT | NULL`

その実行環境から参照可能である場合、windowオブジェクトを返します。

## `JS` JavaScriptコードの実行

`JS(code: STRING): VALUE`

第1引数のJavaScriptコードを実行します。

戻り値は暗黙の型変換が行われます。

```
JS('1 + 2')
# 3
```

```
JS('(function(a, b) {
  return a + b;
})(1, 2)')
# 3
```
