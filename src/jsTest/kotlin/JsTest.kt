import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteValue
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

    @Test
    fun functionCall() = runTest {
        assertEquals(123, evalJs("JS('(function(a, b) { return a + b; })')(100; 23)").int) // JavaScriptの関数を呼び出せる
    }

    @Test
    fun new() = runTest {
        val evaluator = Evaluator()
        evaluator.defineMounts(defaultBuiltinMounts)

        """
            Obj := JS(%>
                function Obj(arg1) {
                    if (this !== undefined) this.arg1 = arg1;
                }
                Obj.prototype.toString = function() {
                    return "" + this.arg1;
                }
                Obj;
            <%);
        """.let { evaluator.get(it) }

        assertEquals(FluoriteNull, evaluator.get("Obj(123)"))
        assertEquals("123", evaluator.get("&Obj::new(123)").string)
    }

    @Test
    fun async() = runTest {
        assertEquals(123, evalJs("AWAIT(JS('Promise')::new(ASYNC(c -> c(123))))").int) // async関数を生成できる
    }

    @Test
    fun await() = runTest {
        assertEquals(123, evalJs("AWAIT(JS('new Promise(callback => callback(123))'))").int) // Promiseに対してAWAITするとサスペンドして取得する
        assertEquals(123, evalJs("AWAIT(JS('async () => 123')())").int) // async関数を実行した結果も同様
    }

}

val defaultBuiltinMounts by lazy {
    listOf(
        createCommonMount(),
        createJsMount {},
    )
}

suspend fun evalJs(src: String): FluoriteValue {
    val evaluator = Evaluator()
    evaluator.defineMounts(defaultBuiltinMounts)
    return evaluator.get(src)
}
