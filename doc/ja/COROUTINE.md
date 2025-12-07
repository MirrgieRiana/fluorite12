# コルーチン

コルーチンは、関数の中断（サスペンド）と再開、非同期処理の派生（コルーチンの起動）からなるプログラミングの概念です。

## コルーチンの起動

`LAUNCH` 関数を使用して新たなコルーチンを起動できます。

### `LAUNCH`: 新しいコルーチンを起動する

`<T> LAUNCH(function: () -> T): PROMISE<T>`

`function` をコルーチンとして非同期に起動します。

起動されたコルーチンは、 `LAUNCH` の呼び出し元のスレッドが次にサスペンドした際に実行されます。

この関数は `function` の戻り値もしくは `function` 内でスローされた例外が格納される `PROMISE` を返します。

```shell
$ flc '
  promise := LAUNCH ( =>
    "apple"
  )
  promise::await()
'
# apple
```

---

`function` は呼び出し元とは独立して起動され、呼び出し元スレッドがサスペンドされ次第実行されます。

```shell
$ flc '
  result := PROMISE.new()
  LAUNCH ( =>
    result::complete("apple")
  )
  result::await()
'
# apple
```

## `PROMISE`: 非同期結果コンテナ

`PROMISE` は、遅延して内容が確定するコンテナです。

### `new`: 新しい `PROMISE` を生成する

`<T> PROMISE.new(): PROMISE<T>`

未完了の新しい `PROMISE` を生成します。

### `complete`: `PROMISE` を完了する

`<T> PROMISE<T>::complete([value: T]): NULL`

`PROMISE` を `VALUE` の内容で完了します。

`value` が省略された場合、 `NULL` を内容として `PROMISE` を完了します。

### `fail`: `PROMISE` を失敗として完了する

`<T> PROMISE<T>::fail([error: VALUE]): NULL`

`PROMISE` を `error` で失敗として完了します。

### `await`: `PROMISE` の完了を待機し、内容を取得する

`<T> PROMISE<T>::await(): T`

`PROMISE` の内容が完了するまで待機し、その内容を返します。

---

`PROMISE` が失敗として完了した場合、 `await` はその例外をスローします。

```shell
$ flc '
  promise := PROMISE.new()
  promise::fail("ERROR!!")
  promise::await() ?: (e => e)
'
# ERROR!!
```

### `isCompleted`: `PROMISE` の完了状態を調べる

`<T> PROMISE<T>::isCompleted(): BOOLEAN`

`PROMISE` が完了、もしくは失敗として完了しているかどうかを返します。
