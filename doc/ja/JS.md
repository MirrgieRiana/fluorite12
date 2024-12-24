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
