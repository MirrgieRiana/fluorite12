import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mirrg.fluorite12.Environment
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.Frame
import mirrg.fluorite12.compilers.compileToGetter
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.defineBuiltinMount
import mirrg.fluorite12.mounts.createCommonMount
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class JsTest {

    @Test
    fun window() = runTest {
        // テスト環境では動作しない
        //assertEquals("[object Window]", runJs("+WINDOW").string)
    }

    @Test
    fun js() = runTest {
        assertEquals(123, evalJs("JS('100 + 23')").int) // JSでJavaScriptが実行できる
    }

}

private suspend fun evalJs(src: String): FluoriteValue {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runners = listOf(
        frame.defineBuiltinMount(createCommonMount()),
        frame.defineBuiltinMount(createJsMount {}),
    )
    val getter = frame.compileToGetter(parseResult.value)
    val env = Environment(null, frame.nextVariableIndex, frame.mountCount)
    runners.forEach {
        it.evaluate(env)
    }
    return getter.evaluate(env)
}
