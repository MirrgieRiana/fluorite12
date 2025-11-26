import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StreamTest {

    @Test
    fun property() = runTest {
        assertEquals("1,2", eval("({a: 1}, {a: 2}).a").stream()) // ストリームのプロパティーは要素のプロパティーの連結
        assertEquals("1,NULL", eval("({a: 1}, {b: 2}).a").stream()) // プロパティをサポートするかは要素ごとに変わる
        assertEquals("1,2", eval("([1], [2]).0").stream()) // 数値以外のキーも指定可能
        assertEquals("[1],[2],[3],[4]", eval("({a: 1, 2}, {a: 3, 4}).a | [_]").stream()) // プロパティーのストリームは連結される
        assertTrue(eval("(,).a").empty()) // 空ストリームのプロパティーは空ストリーム
    }

}
