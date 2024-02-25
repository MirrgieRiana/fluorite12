package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Fluorite12Test {
    @Test
    fun builtInConstantTest() = runTest {
        assertEquals(FluoriteNull, run("NULL"))
        assertEquals(true, run("TRUE").boolean)
        assertEquals(false, run("FALSE").boolean)
    }

    @Test
    fun numberTest() = runTest {
        assertEquals(1, run("1").int) // 整数を記述できる
        assertEquals(0, run("0").int) // 0も普通に書ける
        assertEquals(100, run("00100").int) // 整数は先頭に余計な 0 があっても10進数として扱われる

        assertEquals(1.1, run("1.1").double, 0.001) // 小数を記述できる
        assertEquals(0.0, run("0.0").double) // 0.0も普通に書ける
        assertEquals(1.0, run("1.0").double) // .0 を付けると浮動小数点数で整数値を得る
        assertEquals(100.0, run("00100.00").double) // 小数も先頭と末尾に余計な 0 があっても10進数として扱われる

        assertEquals(-10, run("-10").int) // 負の整数が書ける
        assertEquals(-1.1, run("-1.1").double, 0.001) // 負の小数が書ける
    }

    @Test
    fun rawStringTest() = runTest {
        assertEquals("abcABC123", run(" 'abcABC123' ").string) // ' で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%& ()*+,-./""", run(""" ' !"#$%& ()*+,-./' """).string) // ' はエスケープが必要
        assertEquals("""0123456789:;<=>?""", run(""" '0123456789:;<=>?' """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", run(""" '@ABCDEFGHIJKLMNO' """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", run(""" 'PQRSTUVWXYZ[\]^_' """).string) // \ すらもエスケープ不要
        assertEquals("""`abcdefghijklmno""", run(""" '`abcdefghijklmno' """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, run(""" 'pqrstuvwxyz{|}~ ' """).string)

        assertEquals("あ", run(" 'あ' ").string) // マルチバイト文字
        assertEquals("㎡", run(" '㎡' ").string) // MS932
        assertEquals("🍰", run(" '🍰' ").string) // サロゲートペア

        assertEquals(" ' ", run(" ' '' ' ").string) // '' が ' になる
    }

    @Test
    fun templateStringTest() = runTest {
        assertEquals("abcABC123", run(""" "abcABC123" """).string) // " で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(" ! # %&'()*+,-./", run(""" " ! # %&'()*+,-./" """).string) // " $ はエスケープが必要
        assertEquals("0123456789:;<=>?", run(""" "0123456789:;<=>?" """).string)
        assertEquals("@ABCDEFGHIJKLMNO", run(""" "@ABCDEFGHIJKLMNO" """).string)
        assertEquals("PQRSTUVWXYZ[ ]^_", run(""" "PQRSTUVWXYZ[ ]^_" """).string) // \ はエスケープが必要
        assertEquals("`abcdefghijklmno", run(""" "`abcdefghijklmno" """).string)
        assertEquals("pqrstuvwxyz{|}~ ", run(""" "pqrstuvwxyz{|}~ " """).string)

        assertEquals("あ", run(""" "あ" """).string) // マルチバイト文字
        assertEquals("㎡", run(""" "㎡" """).string) // MS932
        assertEquals("🍰", run(""" "🍰" """).string) // サロゲートペア

        assertEquals(""" " $ \ """, run(""" " \" \$ \\ " """).string) // エスケープが必要な記号
        assertEquals(" \r \n \t ", run(""" " \r \n \t " """).string) // 制御文字のエスケープ

        assertEquals("10", run(""" "$10" """).string) // 数値の埋め込み
        assertEquals("10", run(""" (a -> "${'$'}a")(10) """).string) // 変数の埋め込み
        assertEquals("10", run(""" "$(2 * 5)" """).string) // 式の埋め込み
        assertEquals(" abc ", run(""" " $( "abc" ) " """).string) // 入れ子状の埋め込み
    }

    @Test
    fun embeddedStringTest() = runTest {
        assertEquals("abcABC123", run(" %>abcABC123<% ").string) // %> <% で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%&'()*+,-./""", run(""" %> !"#$%&'()*+,-./<% """).string) // すべての文字はエスケープ不要
        assertEquals("""0123456789:;<=>?""", run(""" %>0123456789:;<=>?<% """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", run(""" %>@ABCDEFGHIJKLMNO<% """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", run(""" %>PQRSTUVWXYZ[\]^_<% """).string) // \ もエスケープ不要
        assertEquals("""`abcdefghijklmno""", run(""" %>`abcdefghijklmno<% """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, run(""" %>pqrstuvwxyz{|}~ <% """).string)

        assertEquals("あ", run(" %>あ<% ").string) // マルチバイト文字
        assertEquals("㎡", run(" %>㎡<% ").string) // MS932
        assertEquals("🍰", run(" %>🍰<% ").string) // サロゲートペア

        assertEquals(" <% ", run(" %> <%% <%").string) // <%% で <% になる

        assertEquals(" 10 ", run(" %> <%= 1 < 2 ? 10 : 100 %> <% ").string) // 式の埋め込み
        assertEquals(" abc ", run(" %> <%= %>abc<% %> <% ").string) // 入れ子状の埋め込み
    }

    @Test
    fun arrayTest() = runTest {
        assertEquals("[1]", run("[1]").array) // [ ] で配列を作れる
        assertEquals("[1;2]", run("[1; 2]").array) // 要素は ; で区切ることができる
        assertEquals("[1;2]", run("[1, 2]").array) // 要素のストリームでもよい
        assertEquals("[1;2;3]", run("[1, 2; 3]").array) // 要素のストリームと要素が混在してもよい
        assertEquals("[1;2;3;4;5;6;7;8]", run("[1; 2..4; 0..2 | _ + 5; 8]").array) // 埋め込まれたストリームは自動的に展開される
        assertEquals("[]", run("[]").array) // 空でもよい
    }

    @Test
    fun objectTest() = runTest {
        assertEquals("{a:1}", run(""" {"a": 1} """).obj) // { } でオブジェクトを作れる
        assertEquals("{a:1}", run("{a: 1}").obj) // キーの " は省略できる
        assertEquals("{1:2}", run("{1: 2}").obj) // キーは数値でもよい
        assertEquals("{1:2}", run("1 | a => {(a): 2}").obj) // キーに ( ) を付けると変数を参照できる
        assertEquals("{a:1;b:2}", run("{a: 1; b: 2}").obj) // エントリーは ; で区切ることができる
        assertEquals("{a:1;b:2}", run("{(a: 1), (b: 2)}").obj) // エントリーのストリームでもよい
        assertEquals("{a:1;b:2;c:3}", run("{(a: 1), (b: 2); c: 3}").obj) // エントリーのストリームとエントリーが混在してもよい
        assertEquals("{1:2;2:4;3:6}", run("{1 .. 3 | a => (a): a * 2}").obj) // エントリー列を返す式でもよい
        assertEquals("{}", run(""" {} """).obj) // 空でもよい

        assertEquals(true, run(""" A := {}; a := A {}; a ?= A """).boolean) // 親クラスを取るオブジェクト
    }

    @Test
    fun accessTest() = runTest {
        assertEquals("b", run(" 'abc'.1 ").string) // 文字列に数値アクセスするとそのインデックスの文字を得る
        assertEquals(FluoriteNull, run(" 'abc'.3 ")) // 文字列の範囲外にアクセスすると NULL が返る
        assertEquals("c", run(" 'abc'.(1 + 1) ").string) // キーを ( ) で囲むと式で参照できる

        assertEquals(20, run(" [10, 20, 30].1 ").int) // 配列に数値アクセスするとそのインデックスの要素を得る
        assertEquals(FluoriteNull, run(" [10, 20, 30].3 ")) // 配列の範囲外にアクセスすると NULL が返る
        assertEquals(30, run(" [10, 20, 30].(1 + 1) ").int) // キーを ( ) で囲むと式で参照できる

        assertEquals(10, run(" {a: 10; b: 20}.a ").int) // オブジェクトに識別子アクセスするとその要素を得る
        assertEquals(10, run(" {a: 10; b: 20}.'a' ").int) // キーは文字列リテラルでもよい
        assertEquals(FluoriteNull, run(" {a: 10; b: 20}.c ")) // 存在しない要素にアクセスすると NULL が返る
        assertEquals(20, run(" 'b' | a => {a: 10; b: 20}.(a) ").int) // キーを ( ) で囲むと式で参照できる
    }

    @Test
    fun toStringTest() = runTest {
        // & で文字列になる
        assertEquals("NULL", run("&NULL").string)
        assertEquals("10", run("&10").string)
        assertEquals("TRUE", run("&TRUE").string)
        assertEquals("FALSE", run("&FALSE").string)
        assertEquals("abc", run("&'abc'").string)
        assertEquals("[1;2;3]", run("&[1, 2, 3]").string)
        assertEquals("{a:1;b:2}", run("&{a: 1; b: 2}").string)

        assertEquals("10", run("&{a: 10; TO_STRING: this -> &this.a}").string) // 文字列化のオーバーライド
    }

    @Test
    fun jsonTest() = runTest {
        // $& でFluoriteValueがjson文字列になる
        assertEquals("10", run("$&10").string) // トップレベルがJsonArrayやJsonObjectでなくてもよい
        assertEquals("10.5", run("$&10.5").string)
        assertEquals("\"abc\"", run("$&'abc'").string)
        assertEquals("true", run("$&TRUE").string)
        assertEquals("false", run("$&FALSE").string)
        assertEquals("null", run("$&NULL").string)
        assertEquals("[1,2,3]", run("$&[1; 2; 3]").string)
        assertEquals("{\"a\":1,\"b\":2}", run("$&{a: 1; b: 2}").string)

        // $* でjson文字列がFluoriteValueになる
        assertEquals(10, run("$*'10'").int)
        assertEquals(10.5, run("$*'10.5'").double, 0.001)
        assertEquals("abc", run("$*'\"abc\"'").string)
        assertEquals(true, run("$*'true'").boolean)
        assertEquals(false, run("$*'false'").boolean)
        assertEquals(FluoriteNull, run("$*'null'"))
        assertEquals("[1;2;3]", run("&$*'[1,2,3]'").string)
        assertEquals("{a:1;b:2}", run("&$*'{\"a\":1,\"b\":2}'").string)
    }

    @Test
    fun rangeTest() = runTest {
        assertEquals("[1;2;3;4]", run("&[1 .. 4]").string) // .. でその範囲をイテレートするストリームを得る
        assertEquals("[0;1;2;3]", run("&[0 .. 4 - 1]").string) // 項は0や四則演算等でもよい
        assertEquals("[-1;0;1]", run("&[-1 .. 1]").string) // 項は0や四則演算等でもよい
        assertEquals("[[1;2;3;4];[1;2;3;4]]", run("a := 1 .. 4; &[[a]; [a]]").string) // 範囲ストリームは再利用できる
    }

    @Test
    fun builtInClassTest() = runTest {
        // 各クラスのtrue判定
        assertEquals(true, run("1 ?= VALUE_CLASS").boolean)
        assertEquals(true, run("NULL ?= NULL_CLASS").boolean)
        assertEquals(true, run("1 ?= INT_CLASS").boolean)
        assertEquals(true, run("1.2 ?= DOUBLE_CLASS").boolean)
        assertEquals(true, run("TRUE ?= BOOLEAN_CLASS").boolean)
        assertEquals(true, run("'a' ?= STRING_CLASS").boolean)
        assertEquals(true, run("[1] ?= ARRAY_CLASS").boolean)
        assertEquals(true, run("{a: 1} ?= OBJECT_CLASS").boolean)
        assertEquals(true, run("(a -> 1) ?= FUNCTION_CLASS").boolean)
        assertEquals(true, run("(1, 2) ?= STREAM_CLASS").boolean)

        // falseテスト
        assertEquals(false, run("'10' ?= INT_CLASS").boolean)
        assertEquals(false, run("10 ?= STRING_CLASS").boolean)
        assertEquals(false, run("(1, 2) ?= ARRAY_CLASS").boolean)
        assertEquals(false, run("1.2 ?= INT_CLASS").boolean)
        assertEquals(false, run("1 ?= DOUBLE_CLASS").boolean)
    }

    @Test
    fun variableTest() = runTest {
        assertEquals(10, run("a := 10; a").int) // := で変数を定義できる
        assertEquals(20, run("a := 10; a = 20; a").int) // = で既存の変数に代入できる
        assertEquals(10, run("a := 10; (a := 20; a = 30); a").int) // 変数は ( ) の外部に伝搬しない
    }

    @Test
    fun lambdaTest() = runTest {
        assertEquals(10, run("((a) -> a)(10)").int) // (a) -> b で関数を作り、 f(a) で実行する
        assertEquals(12, run("((a; b) -> a * b)(3; 4)").int) // ; で引数を複数取れる
        assertEquals(12, run("((a, b) -> a * b)(3; 4)").int) // ラムダ引数は , で区切ってもよい
        assertEquals(10, run("(() -> 10)()").int) // () で引数を無しにできる
        assertEquals(10, run("(a -> a)(10)").int) // 引数がある場合、 ( ) は省略してもよい
        assertEquals(12, run("(a, b -> a * b)(3; 4)").int) // 引数が複数の場合も ( ) を省略できる

        assertEquals("[1;2;3;4]", run("(s -> &[s])(1, 2, 3, 4)").string) // 引数で , を使うとストリームを渡せる

        assertEquals(120, run("f := n -> n == 0 ? 1 : n * f(n - 1); f(5)").int) // 再帰関数の例
        assertEquals(120, run("(f -> f(f))(f -> n -> n == 0 ? 1 : n * f(f)(n - 1))(5)").int) // 複雑なラムダ計算の例
    }

    @Test
    fun methodTest() = runTest {
        assertEquals(10, run("{method: () -> 10}::method()").int) // a::b() でaのbを呼び出せる
        assertEquals(10, run("{a: 10; method: this -> this.a}::method()").int) // メソッド関数は最初の引数にthisを受け取る
        assertEquals(20, run("{a: 10; method: this, b -> this.a * b}::method(2)").int) // 2個目以降の引数にメソッド呼び出し時の引数を受け取る

        assertEquals("10", run("10::TO_STRING()").string) // 組み込みメソッドの呼び出し

        assertEquals(10, run("A := {m: _ -> _.v}; a := A {v: 10}; a::m()").int) // メソッドの継承
    }

    @Test
    fun rootTest() = runTest {
        assertEquals(10, run("10").int) // 式を書ける
        assertEquals(20, run("10; 20").int) // ; で区切ると左は式文になり、右が使われる
        assertEquals(20, run("10\n20").int) // 改行で区切ってもよい
        assertEquals(FluoriteVoid, run("10;")) // 式を省略した場合、VOIDになる
        assertEquals(10, run("10\n").int) // 式の前後に余計な改行があっても無視される
        assertEquals(FluoriteVoid, run("10;\n")) // ; の後に改行があった場合もVOIDになる
        assertEquals(30, run("10; 20; 30").int) // ; が複数あってもよい
        assertEquals(20, run("; 10; ; 20").int) // ; の左は省略されていてもよい
        assertEquals(20, run("\n\n;;\n\n10\n\n;;\n\n;;\n\n20\n\n").int) // 改行と;が無駄に大量にあってもよい
    }
}

private suspend fun run(src: String): FluoriteValue {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runners = frame.defineCommonBuiltinVariables()
    val getter = frame.compileToGetter(parseResult.value)
    val env = Environment(null, frame.nextVariableIndex)
    runners.forEach {
        it.evaluate(env)
    }
    return getter.evaluate(env)
}

private val FluoriteValue.int get() = (this as FluoriteInt).value
private val FluoriteValue.double get() = (this as FluoriteDouble).value
private val FluoriteValue.boolean get() = (this as FluoriteBoolean).value
private val FluoriteValue.string get() = (this as FluoriteString).value
private val FluoriteValue.obj get() = (this as FluoriteObject).toString()
private val FluoriteValue.array get() = (this as FluoriteArray).toString()
