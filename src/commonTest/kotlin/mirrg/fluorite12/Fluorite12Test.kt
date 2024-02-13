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
        assertTrue((run(""" "abcABC123" """) as String) == "abcABC123")
        assertTrue((run(""" "" """) as String) == "")
        assertTrue((run(""" "abc$( "ABC" )123" """) as String) == "abcABC123")
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
