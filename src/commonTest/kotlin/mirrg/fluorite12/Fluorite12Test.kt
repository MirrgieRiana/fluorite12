package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parsed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class Fluorite12Test {
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
    }

    @Test
    fun objectTest() = runTest {
        assertEquals("{a:1}", run(""" {"a": 1} """).obj) // { } でオブジェクトを作れる
        assertEquals("{a:1}", run("{a: 1}").obj) // キーの " は省略できる
        assertEquals("{1:2}", run("{1: 2}").obj) // キーは数値でもよい
        assertEquals("{1:2}", run("1 | a => {(a): 2}").obj) // キーに ( ) を付けると変数を参照できる
        assertEquals("{a:1,b:2}", run("{a: 1; b: 2}").obj) // エントリーは ; で区切ることができる
        assertEquals("{a:1,b:2}", run("{a: 1, b: 2}").obj) // エントリーのストリームでもよい
        assertEquals("{1:2,2:4,3:6}", run("{1 .. 3 | a => (a): a * 2}").obj) // エントリー列を返す式でもよい
    }

    @Test
    fun test() = runTest {
        assertTrue(run("a->a") is FluoriteFunction)
        assertEquals(5, run("(a->a)(5)").int)
        assertEquals(12, run("(a,b->a*b)(3;4)").int)
    }
}

private suspend fun run(src: String): FluoriteValue {
    val result = Fluorite12Grammar().tryParseToEnd(src) as Parsed
    return Frame().evaluate(result.value)
}

private val FluoriteValue.int get() = (this as FluoriteInt).value
private val FluoriteValue.double get() = (this as FluoriteDouble).value
private val FluoriteValue.string get() = (this as FluoriteString).value
private val FluoriteValue.obj get() = (this as FluoriteObject).toString()
