package mirrg.fluorite12

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ArrayTest {

    @Test
    fun create() = runTest {
        assertEquals("[1]", run("[1]").array()) // 配列の生成
        assertEquals("[1;2]", run("[1; 2]").array()) // 複数の要素を持つ配列
        assertEquals("[1;2]", run("[; ; 1; ; 2; ; ]").array()) // ; は無駄に大量にあってもよい
        assertEquals("[]", run("[; ]").array()) // ; しかなくてもよい
        assertEquals("[1;2]", run("[1, 2]").array()) // ストリームは展開される
        assertEquals("[1;2;3]", run("[1, 2; 3]").array()) // ストリームと要素が混在してもよい
        assertEquals("[1;2;3;4;5;6;7;8]", run("[1; 2..4; 0..2 | _ + 5; 8]").array()) // 複雑なストリームの例
        assertEquals("[]", run("[]").array()) // 空でもよい
    }

    @Test
    fun assign() = runTest {

        // 配列呼び出しによる代入
        """
           array := [1; 2; 3]
           array(1) = 9
           array
        """.let { assertEquals("[1;9;3]", run(it).array()) }

        // プロパティアクセスによるストリームの代入
        """
           array := [1; 2; 3]
           array.1 = 4 .. 6
           array
        """.let { assertEquals("[1;456;3]", run(it).array()) }

    }

    @Test
    fun subArray() = runTest {
        assertEquals("[1;2;3]", run("[1; 2; 3][]").array()) // 配列のコピー
        assertEquals("[2]", run("[1; 2; 3][1]").array()) // 単一要素による部分配列の取得
        assertEquals("[2]", run("[1; 2; 3]['0.95']").array()) // インデックスは数値化し、四捨五入される
        assertEquals("[NULL]", run("[1; 2; 3][3]").array()) // 範囲外のインデックスは NULL が返る
        assertEquals("[3]", run("[1; 2; 3][-1]").array()) // 負のインデックスは後ろから数える
        assertEquals("[3;3;1;2]", run("[1; 2; 3][2, 2, 0, 1]").array()) // インデックスのストリームは要素のストリームを返す

        assertEquals("[2;3;4]", run("[1; 2; 3; 4; 5][1 .. 3]").array()) // 範囲指定による部分配列の取得

        // 配列のコピーは別のインスタンスを返す
        """
            array1 := [1; 2; 3]
            array2 := array1[]
            array2(1) = 99
            [array1, array2]
        """.let { assertEquals("[[1;2;3];[1;99;3]]", run(it).array()) }
    }

    @Test
    fun invoke() = runTest {
        assertEquals("1,2,3", run("[1; 2; 3]()").stream()) // ストリーム化
        assertEquals(2, run("[1; 2; 3](1)").int) // 関数呼び出しによる単一要素の取得
        assertEquals(2, run("[1; 2; 3]('0.95')").int) // インデックスは数値化し、四捨五入される
        assertEquals(FluoriteNull, run("[1; 2; 3](3)")) // 範囲外のインデックスは NULL が返る
        assertEquals(3, run("[1; 2; 3](-1)").int) // 負のインデックスは後ろから数える
        assertEquals("3,3,1,2", run("[1; 2; 3](2, 2, 0, 1)").stream()) // インデックスのストリームは要素のストリームを返す
    }

    @Test
    fun access() = runTest {
        assertEquals(2, run("[1; 2; 3].1").int) // 要素アクセス
        assertEquals(2, run("[1; 2; 3].'0.95'").int) // インデックスは数値化し、四捨五入される
        assertEquals(FluoriteNull, run("[1; 2; 3].3")) // 範囲外のインデックスは NULL が返る
        assertEquals(FluoriteNull, run("[1; 2; 3].(-1)")) // 負のインデックスも範囲外扱い
    }

    @Test
    fun length() = runTest {
        assertEquals(3, run("$#[1; 2; 3]").int) // 長さの取得
        assertEquals(0, run("$#[]").int) // 空配列の長さは 0
    }

    @Test
    fun plus() = runTest {
        assertEquals("[1;2;3;4]", run("[1; 2] + [3; 4]").array()) // 配列同士の連結
    }

    @Test
    fun times() = runTest {
        assertEquals("[1;2;1;2;1;2]", run("[1; 2] * 3").array()) // 配列を繰り返した配列の取得
    }

}
