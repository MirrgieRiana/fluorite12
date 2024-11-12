package mirrg.fluorite12

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ArrayTest {

    @Test
    fun arrayTest() = runTest {
        assertEquals("[1]", run("[1]").array) // [ ] で配列を作れる
        assertEquals("[1;2]", run("[1; 2]").array) // 要素は ; で区切ることができる
        assertEquals("[1;2]", run("[; ; 1; ; 2; ; ]").array) // ; は無駄に大量にあってもよい
        assertEquals("[]", run("[; ]").array) // ; しかなくてもよい
        assertEquals("[1;2]", run("[1, 2]").array) // 要素のストリームでもよい
        assertEquals("[1;2;3]", run("[1, 2; 3]").array) // 要素のストリームと要素が混在してもよい
        assertEquals("[1;2;3;4;5;6;7;8]", run("[1; 2..4; 0..2 | _ + 5; 8]").array) // 埋め込まれたストリームは自動的に展開される
        assertEquals("[]", run("[]").array) // 空でもよい
    }

    @Test
    fun arrayAssignmentTest() = runTest {
        assertEquals("[10;99;30]", run("array := [10, 20, 30]; array(1) = 99; array").array) // 配列の要素に代入できる
    }

    @Test
    fun bracketsAccessTest() = runTest {
        assertEquals("1,2,3", run("[1; 2; 3]()").stream()) // array() でストリームにする
        assertEquals(2, run("[1; 2; 3](1)").int) // array(index) で要素を得る
        assertEquals(3, run("[1; 2; 3](-1)").int) // 負のインデックスは後ろから数える
        assertEquals("3,3,1,2", run("[1; 2; 3](2, 2, 0, 1)").stream()) // array(indices) で要素のストリームを得る
    }

    @Test
    fun addTest() = runTest {
        // TODO
        assertEquals("[1;2;3;4]", run("[1; 2] + [3; 4]").array) // 配列の加算は配列同士を連結する
    }

}
