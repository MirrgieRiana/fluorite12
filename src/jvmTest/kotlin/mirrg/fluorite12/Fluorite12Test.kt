package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parsed
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class Fluorite12Test {
    @Test
    fun lambdaTest(): Unit = runBlocking {
        Assert.assertTrue(run("a->a") is FluoriteFunction)
        Assert.assertEquals(5, run("(a->a)(5)"))
        Assert.assertEquals(12.0, run("(a,b->a*b)(3;4)") as Double, 0.01)
    }
}

private suspend fun run(src: String): Any? {
    val result = Fluorite12Grammar().tryParseToEnd(src) as Parsed
    return Frame().evaluate(result.value)
}
