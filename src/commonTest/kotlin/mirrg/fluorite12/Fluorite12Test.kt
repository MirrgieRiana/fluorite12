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
        assertEquals("abcABC123", run(""" "abcABC123" """) as String) // " で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(" ! # %&'()*+,-./", run(""" " ! # %&'()*+,-./" """) as String) // " $ はエスケープが必要
        assertEquals("0123456789:;<=>?", run(""" "0123456789:;<=>?" """) as String)
        assertEquals("@ABCDEFGHIJKLMNO", run(""" "@ABCDEFGHIJKLMNO" """) as String)
        assertEquals("PQRSTUVWXYZ[ ]^_", run(""" "PQRSTUVWXYZ[ ]^_" """) as String) // \ はエスケープが必要
        assertEquals("`abcdefghijklmno", run(""" "`abcdefghijklmno" """) as String)
        assertEquals("pqrstuvwxyz{|}~ ", run(""" "pqrstuvwxyz{|}~ " """) as String)

        assertEquals("あ", run(""" "あ" """) as String) // マルチバイト文字
        assertEquals("㎡", run(""" "㎡" """) as String) // MS932
        assertEquals("🍰", run(""" "🍰" """) as String) // サロゲートペア

        assertEquals(""" " $ \ """, run(""" " \" \$ \\ " """) as String) // エスケープが必要な記号
        assertEquals(" \r \n \t ", run(""" " \r \n \t " """) as String) // 制御文字のエスケープ

        assertEquals("10", run(""" "$10" """) as String) // 数値の埋め込み
        assertEquals("10", run(""" (a -> "${'$'}a")(10) """) as String) // 変数の埋め込み
        assertEquals("10", run(""" "$(1 < 2 ? 10 : 100)" """) as String) // 式の埋め込み
    }

    @Test
    fun test() = runTest {
        assertTrue(run("a->a") is FluoriteFunction)
        assertEquals(5, run("(a->a)(5)"))
        assertEquals(12.0, run("(a,b->a*b)(3;4)") as Double, 0.01)
    }
}

private suspend fun run(src: String): Any? {
    val result = Fluorite12Grammar().tryParseToEnd(src) as Parsed
    return Frame().evaluate(result.value)
}
