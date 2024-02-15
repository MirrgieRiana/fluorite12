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
    fun stringTest() = runTest {
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
        assertEquals("10", run(""" "$(1 < 2 ? 10 : 100)" """).string) // 式の埋め込み
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
private val FluoriteValue.string get() = (this as FluoriteString).value
private val FluoriteValue.obj get() = (this as FluoriteObject).toString()
