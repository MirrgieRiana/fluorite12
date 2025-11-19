import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.mounts.createCommonMounts
import mirrg.fluorite12.operations.FluoriteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class Fluorite12Test {

    @Test
    fun cacheTest() = runTest {
        // メモ化が行われているか
        assertEquals("a", eval(""" "$( "$( "$( "$( "$( "$( "$( "$( "$( "$( "a" )" )" )" )" )" )" )" )" )" )" """).string)
        assertEquals("a", eval(""" %><%= %><%= %><%= %><%= %><%= %><%= %>a<% %><% %><% %><% %><% %><% %><% """).string)
        assertEquals(1, eval("(((((((((1)))))))))").int)
        assertEquals("[[[[[[[[[1]]]]]]]]]", eval("&[[[[[[[[[1]]]]]]]]]").string)
        assertEquals("{a:{a:{a:{a:{a:{a:{a:{a:1}}}}}}}}", eval("&{a:{a:{a:{a:{a:{a:{a:{a:1}}}}}}}}").string)
        assertEquals(1, eval("a := x -> x; a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(1))))))))))))))))").int)
        assertEquals(1, eval("1?1?1?1?1?1?1?1?1?1?1?1?1?1?1?1?1:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0").int)
        assertEquals(1, eval("0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:1").int)
        assertEquals(1, eval("NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:1").int)
        assertEquals("[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]", eval("&[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1]").string)
        assertEquals("[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]", eval("&[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]").string)
    }

    @Test
    fun commentTest() = runTest {

        // 行コメントが書ける
        """
            [
                1
                2 # comment
                3
            ]
        """.let { assertEquals("[1;2;3]", eval(it).array()) }

        // // による行コメント
        """
            [
                1
                2 // comment
                3
            ]
        """.let { assertEquals("[1;2;3]", eval(it).array()) }

        // 行コメントの次の行に中置演算子としても解釈可能な前置演算子があったとしても、その行を結合しない
        """
            [
                1
                2 # comment
                -3
            ]
        """.let { assertEquals("[1;2;-3]", eval(it).array()) }
        """
            [
                1
                2 // comment
                -3
            ]
        """.let { assertEquals("[1;2;-3]", eval(it).array()) }

        // 行コメントの本文が空でもよい
        assertEquals(1, eval("1 #").int)
        assertEquals(1, eval("1 //").int)

        // 行コメントの後に改行が無くてもよい
        assertEquals(1, eval("1 # comment").int)
        assertEquals(1, eval("1 // comment").int)

        assertEquals("[1;2;3]", eval("[1, /* comment */2, 3]").array()) // ブロックコメントが書ける
        assertEquals("[1;2;3]", eval("[1, /**/2, 3]").array()) // ブロックコメントが空でもよい
        assertEquals("[1;2;3]", eval("[1, /* /* comment */ */2, 3]").array()) // ブロックコメントを入れ子にできる
        assertEquals("[1;2;3]", eval("/* comment */[1, 2, 3]").array()) // ブロックコメントの前に何もなくてもよい
        assertEquals("[1;2;3]", eval("[1, 2, 3]/* comment */").array()) // ブロックコメントの後に何もなくてもよい

        // ブロックコメントの途中に改行が入ってもよい
        """
            [
                1
                /*
                 * comment
                 */
                2
            ]
        """.let { assertEquals("[1;2]", eval(it).array()) }

    }

    @Test
    fun builtInConstantTest() = runTest {
        assertEquals(FluoriteNull, eval("NULL"))
        assertEquals(true, eval("TRUE").boolean)
        assertEquals(false, eval("FALSE").boolean)
        assertEquals(true, eval("LOOP") is FluoriteStream)
    }

    @Test
    fun numberTest() = runTest {
        assertEquals(1, eval("1").int) // 整数を記述できる
        assertEquals(0, eval("0").int) // 0も普通に書ける
        assertEquals(100, eval("00100").int) // 整数は先頭に余計な 0 があっても10進数として扱われる

        assertEquals(0x10, eval("H#10").int) // H# で16進数を記述できる
        assertEquals(-0x10, eval("-H#10").int) // 負の16進数
        assertEquals(0xabcdef, eval("H#abcdef").int) // 英字の16進数
        assertEquals(0xABCDEF, eval("H#ABCDEF").int) // 大文字の英字の16進数

        assertEquals(1.1, eval("1.1").double, 0.001) // 小数を記述できる
        assertEquals(0.0, eval("0.0").double) // 0.0も普通に書ける
        assertEquals(1.0, eval("1.0").double) // .0 を付けると浮動小数点数で整数値を得る
        assertEquals(100.0, eval("00100.00").double) // 小数も先頭と末尾に余計な 0 があっても10進数として扱われる

        assertEquals(-10, eval("-10").int) // 負の整数が書ける
        assertEquals(-1.1, eval("-1.1").double, 0.001) // 負の小数が書ける
    }

    @Test
    fun rawStringTest() = runTest {
        assertEquals("abcABC123", eval(" 'abcABC123' ").string) // ' で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%& ()*+,-./""", eval(""" ' !"#$%& ()*+,-./' """).string) // ' はエスケープが必要
        assertEquals("""0123456789:;<=>?""", eval(""" '0123456789:;<=>?' """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", eval(""" '@ABCDEFGHIJKLMNO' """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", eval(""" 'PQRSTUVWXYZ[\]^_' """).string) // \ すらもエスケープ不要
        assertEquals("""`abcdefghijklmno""", eval(""" '`abcdefghijklmno' """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, eval(""" 'pqrstuvwxyz{|}~ ' """).string)

        assertEquals("あ", eval(" 'あ' ").string) // マルチバイト文字
        assertEquals("㎡", eval(" '㎡' ").string) // MS932
        assertEquals("🍰", eval(" '🍰' ").string) // サロゲートペア

        assertEquals(" ' ", eval(" ' '' ' ").string) // '' が ' になる

        assertEquals("\n \n \n", eval(" '\n \r \r\n' ").string) // 改行は \n に統一される
    }

    @Test
    fun templateStringTest() = runTest {
        assertEquals("abcABC123", eval(""" "abcABC123" """).string) // " で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(" ! # %&'()*+,-./", eval(""" " ! # %&'()*+,-./" """).string) // " $ はエスケープが必要
        assertEquals("0123456789:;<=>?", eval(""" "0123456789:;<=>?" """).string)
        assertEquals("@ABCDEFGHIJKLMNO", eval(""" "@ABCDEFGHIJKLMNO" """).string)
        assertEquals("PQRSTUVWXYZ[ ]^_", eval(""" "PQRSTUVWXYZ[ ]^_" """).string) // \ はエスケープが必要
        assertEquals("`abcdefghijklmno", eval(""" "`abcdefghijklmno" """).string)
        assertEquals("pqrstuvwxyz{|}~ ", eval(""" "pqrstuvwxyz{|}~ " """).string)

        assertEquals("あ", eval(""" "あ" """).string) // マルチバイト文字
        assertEquals("㎡", eval(""" "㎡" """).string) // MS932
        assertEquals("🍰", eval(""" "🍰" """).string) // サロゲートペア
        assertEquals("あ", eval(""" "\u3042" """).string) // 文字参照

        assertEquals(""" " $ \ """, eval(""" " \" \$ \\ " """).string) // エスケープが必要な記号
        assertEquals(" \r \n \t ", eval(""" " \r \n \t " """).string) // 制御文字のエスケープ

        assertEquals("10", eval(""" "$10" """).string) // 数値の埋め込み
        assertEquals("10", eval(""" (a -> "${'$'}a")(10) """).string) // 変数の埋め込み
        assertEquals("10", eval(""" "$(2 * 5)" """).string) // 式の埋め込み
        assertEquals(" abc ", eval(""" " $( "abc" ) " """).string) // 入れ子状の埋め込み

        assertEquals("\n \n \n", eval(""" "${"\n \r \r\n"}" """).string) // 改行は \n に統一される
    }

    @Test
    fun formatTest() = runTest {
        // %-+ 09d  空白埋め  0埋め  左揃え  符号表示  符号余白  変換

        // 整数
        run {
            val s = """123456, 12345, 123, 0, -123, -1234, -12345, -123456"""
            assertEquals("[123456;12345;123;0;-123;-1234;-12345;-123456]", eval(""" [$s | "$%d(_)"] """).array()) // %d で整数
            assertEquals("[123456;12345;  123;    0; -123;-1234;-12345;-123456]", eval(""" [$s | "$%5d(_)"] """).array()) // 空白埋め
            assertEquals("[123456;12345;00123;00000;-0123;-1234;-12345;-123456]", eval(""" [$s | "$%05d(_)"] """).array()) // 0埋め
            assertEquals("[123456;12345;123  ;0    ;-123 ;-1234;-12345;-123456]", eval(""" [$s | "$%-5d(_)"] """).array()) // 左揃え空白埋め
            assertEquals("[+123456;+12345;+123;+0;-123;-1234;-12345;-123456]", eval(""" [$s | "$%+d(_)"] """).array()) // 符号表示
            assertEquals("[ 123456; 12345; 123; 0;-123;-1234;-12345;-123456]", eval(""" [$s | "$% d(_)"] """).array()) // 符号余白
            assertEquals("[+123456;+12345; +123;   +0; -123;-1234;-12345;-123456]", eval(""" [$s | "$%+5d(_)"] """).array()) // 符号表示 空白埋め
            assertEquals("[ 123456; 12345;  123;    0; -123;-1234;-12345;-123456]", eval(""" [$s | "$% 5d(_)"] """).array()) // 符号余白 空白埋め
            assertEquals("[+123456;+12345;+0123;+0000;-0123;-1234;-12345;-123456]", eval(""" [$s | "$%+05d(_)"] """).array()) // 符号表示 0埋め
            assertEquals("[ 123456; 12345; 0123; 0000;-0123;-1234;-12345;-123456]", eval(""" [$s | "$% 05d(_)"] """).array()) // 符号余白 0埋め
        }

        // 16進数
        run {
            val s = """H#abcdef, H#abcde, H#abc, H#0, -H#abc, -H#abcd, -H#abcde, -H#abcdef"""
            assertEquals("[abcdef;abcde;abc;0;-abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%x(_)"] """).array()) // %x で16進数
            assertEquals("[abcdef;abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%5x(_)"] """).array()) // 空白埋め
            assertEquals("[abcdef;abcde;00abc;00000;-0abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%05x(_)"] """).array()) // 0埋め
            assertEquals("[abcdef;abcde;abc  ;0    ;-abc ;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%-5x(_)"] """).array()) // 左揃え空白埋め
            assertEquals("[+abcdef;+abcde;+abc;+0;-abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%+x(_)"] """).array()) // 符号表示
            assertEquals("[ abcdef; abcde; abc; 0;-abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$% x(_)"] """).array()) // 符号余白
            assertEquals("[+abcdef;+abcde; +abc;   +0; -abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%+5x(_)"] """).array()) // 符号表示 空白埋め
            assertEquals("[ abcdef; abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$% 5x(_)"] """).array()) // 符号余白 空白埋め
            assertEquals("[+abcdef;+abcde;+0abc;+0000;-0abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$%+05x(_)"] """).array()) // 符号表示 0埋め
            assertEquals("[ abcdef; abcde; 0abc; 0000;-0abc;-abcd;-abcde;-abcdef]", eval(""" [$s | "$% 05x(_)"] """).array()) // 符号余白 0埋め
        }

        // 小数
        run {
            assertEquals("1.5", eval(""" "$%f(1.5)" """).string) // 小数の埋め込み
            assertEquals("-1.5", eval(""" "$%f(-1.5)" """).string) // 負数
            assertEquals("0", eval(""" "$%f(0.0)" """).string) // 0
            assertEquals("1", eval(""" "$%f(1.0)" """).string) // 整数値は小数点以降が省略される
            assertEquals("0.5", eval(""" "$%f(0.5)" """).string) // 小数点の前は省略されない

            assertEquals("1.111", eval(""" "$%.3f(1.111222)" """).string) // 小数の切り詰め
            assertEquals("1.112", eval(""" "$%.3f(1.111777)" """).string) // 四捨五入をする
            assertEquals("1.112", eval(""" "$%.3f(1.111500)" """).string) // 真ん中は絶対値が大きい方に丸められる
            assertEquals("-1.112", eval(""" "$%.3f(-1.111500)" """).string) // 負の場合も絶対値が増える方向に丸められる
            assertEquals("100.000", eval(""" "$%.3f(99.999999)" """).string) // 丸めによって桁数が増える場合のテスト
            assertEquals("-100.000", eval(""" "$%.3f(-99.999999)" """).string) // 負の場合のテスト
            assertEquals("1.500", eval(""" "$%.3f(1.5)" """).string) // 小数の埋め合わせ
            assertEquals("1.111", eval(""" "$%.3f(1.111)" """).string) // 精度が丁度

            // 小数点以下0桁の場合、小数点も消える
            assertEquals("2", eval(""" "$%.0f(1.5)" """).string)
            assertEquals("1", eval(""" "$%.0f(1.0)" """).string)
            assertEquals("-2", eval(""" "$%.0f(-1.5)" """).string)
            assertEquals("-1", eval(""" "$%.0f(-1.0)" """).string)

            assertEquals("  1.5", eval(""" "$%5f(1.5)" """).string) // 空白埋め指定は全体の文字数に作用する
            assertEquals(" -1.5", eval(""" "$%5f(-1.5)" """).string) // 負の空白埋め
            assertEquals("12345.5", eval(""" "$%5f(12345.5)" """).string) // 空白埋めは文字数を切り詰めない
            assertEquals("1.5  ", eval(""" "$%-5f(1.5)" """).string) // 左詰め
            assertEquals("001.5", eval(""" "$%05f(1.5)" """).string) // 0埋め
            assertEquals("+01.5", eval(""" "$%+05f(1.5)" """).string) // +を表示
            assertEquals(" 01.5", eval(""" "$% 05f(1.5)" """).string) // 符号用余白
            assertEquals("1.000", eval(""" "$%.3f(1.0)" """).string) // もともと小数点が含まれず、精度が1以上

            assertEquals("-01.5", eval(""" "$%05f(-1.5)" """).string) // 負の0埋めは符号を先に書く
            assertEquals(" 01.5", eval(""" "$% 05f(1.5)" """).string) // 0埋めでも符号用の余白は空白を書く

            // 小数点なし左詰め0埋めは数学的に矛盾した挙動を示す
            assertEquals("10000", eval(""" "$%-05.0f(1.0)" """).string)
            assertEquals("20000", eval(""" "$%-05.0f(1.5)" """).string)

            assertEquals("  1.123", eval(""" "$%7.3f(1.123456)" """).string) // 空白埋めかつ精度指定
        }

        // 文字列
        run {
            val s = """ "", "abcd", "abcde", "abcdef" """
            assertEquals("[;abcd;abcde;abcdef]", eval(""" [$s | "$%s(_)"] """).array()) // %s で文字列
            assertEquals("[     ; abcd;abcde;abcdef]", eval(""" [$s | "$%5s(_)"] """).array()) // 空白埋め
            assertEquals("[     ;abcd ;abcde;abcdef]", eval(""" [$s | "$%-5s(_)"] """).array()) // 左揃え空白埋め
        }
    }

    @Test
    fun embeddedStringTest() = runTest {
        assertEquals("abcABC123", eval(" %>abcABC123<% ").string) // %> <% で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%&'()*+,-./""", eval(""" %> !"#$%&'()*+,-./<% """).string) // すべての文字はエスケープ不要
        assertEquals("""0123456789:;<=>?""", eval(""" %>0123456789:;<=>?<% """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", eval(""" %>@ABCDEFGHIJKLMNO<% """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", eval(""" %>PQRSTUVWXYZ[\]^_<% """).string) // \ もエスケープ不要
        assertEquals("""`abcdefghijklmno""", eval(""" %>`abcdefghijklmno<% """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, eval(""" %>pqrstuvwxyz{|}~ <% """).string)

        assertEquals("あ", eval(" %>あ<% ").string) // マルチバイト文字
        assertEquals("㎡", eval(" %>㎡<% ").string) // MS932
        assertEquals("🍰", eval(" %>🍰<% ").string) // サロゲートペア

        assertEquals(" <% ", eval(" %> <%% <%").string) // <%% で <% になる

        assertEquals(" 10 ", eval(" %> <%= 1 < 2 ? 10 : 100 %> <% ").string) // 式の埋め込み
        assertEquals(" abc ", eval(" %> <%= %>abc<% %> <% ").string) // 入れ子状の埋め込み

        assertEquals("_30_10_10", eval(" a := 10; b := %>_<%= a := 20; a = 30; a %>_<%= a %>_<%; b & a ").string) // スコープを作る

        assertEquals("\n \n \n", eval(" %>\n \r \r\n<% ").string) // 改行は \n に統一される
    }

    @Test
    fun bracketsTest() = runTest {
        assertEquals(1, eval("(1)").int) // ( ) で囲うと中身をそのまま得られる
        assertEquals(FluoriteNull, eval("()")) // () でNULLになる
        assertEquals("", eval("(,)").stream()) // (,) で空ストリームになる
    }

    @Test
    fun objectTest() = runTest {
        assertEquals("{a:1}", eval(""" {"a": 1} """).obj) // { } でオブジェクトを作れる
        assertEquals("{a:1}", eval("{a: 1}").obj) // キーの " は省略できる
        assertEquals("{1:2}", eval("{1: 2}").obj) // キーは数値でもよい
        assertEquals("{1:2}", eval("1 | a => {(a): 2}").obj) // キーに ( ) を付けると変数を参照できる
        assertEquals("{a:1;b:2}", eval("{a: 1; b: 2}").obj) // エントリーは ; で区切ることができる
        assertEquals("{a:1;b:2}", eval("{; ; a: 1; ; b: 2; ; }").obj) // ; は無駄に大量にあってもよい
        assertEquals("{}", eval("{; }").obj) // ; しかなくてもよい
        assertEquals("{a:1;b:2}", eval("{(a: 1), (b: 2)}").obj) // エントリーのストリームでもよい
        assertEquals("{a:1;b:2;c:3}", eval("{(a: 1), (b: 2); c: 3}").obj) // エントリーのストリームとエントリーが混在してもよい
        assertEquals("{1:2;2:4;3:6}", eval("{1 .. 3 | a => (a): a * 2}").obj) // エントリー列を返す式でもよい
        assertEquals("{}", eval(""" {} """).obj) // 空でもよい

        assertEquals(true, eval(""" A := {}; a := A {}; a ?= A """).boolean) // 親クラスを取るオブジェクト

        assertEquals("{a:100;b:123}", eval("{a := 100; b: a + 23}").obj) // オブジェクト内で変数が宣言できる
        assertEquals("{a:100;b:120;c:123}", eval("{a := 100; b := a + 20; c: b + 3}").obj) // 変数の初期化子の中からも変数を参照できる
        assertEquals("{a:100}", eval("{a := 100}").obj) // 変数宣言しかなくても初期化される
        assertEquals("{a:100;b:123}", eval("{b: a + 23; a := 100}").obj) // オブジェクトリテラル内の変数は参照した時点で初期化される
        assertEquals(20, eval("o := {v := 10; f: () -> v}; o.v = 20; o.f()").int) // オブジェクト変数はそのオブジェクトのエントリーの値と連動する
        assertEquals(20, eval("o := {v := 10; f: (n) -> v = n}; o.f(20); o.v").int) // 代入も可能
    }

    @Test
    fun toNumberTest() = runTest {
        // + で数値になる

        assertEquals(0, eval("+NULL").int) // NULLは0

        // 文字列の数値化
        assertEquals(123, eval("+'123'").int)
        assertEquals(123.456, eval("+'123.456'").double, 0.001)

        assertEquals(1, eval("+TRUE").int) // TRUEは1
        assertEquals(0, eval("+FALSE").int) // FALSEは0

        assertEquals(1, eval("+1").int) // 整数はそのまま
        assertEquals(1.0, eval("+1.0").double, 0.001) // 小数もそのまま

        assertEquals(55, eval("+(1 .. 10)").int) // ストリームは各要素の合計
        assertEquals(6, eval("+('1', '2', '3')").int) // ストリームは各要素の数値化の合計
        assertEquals(10, eval("+('10')").int) // 1要素のストリームはそれの数値化
        assertEquals(0, eval("+(,)").int) // 空ストリームはINTの0


        assertEquals(123, eval("+{`+_`: this -> 123}{}").int) // オブジェクトの数値化はTO_NUMBERメソッドでオーバーライドできる
    }

    @Test
    fun toBooleanTest() = runTest {
        // ? で論理値になる

        assertEquals(false, eval("?NULL").boolean) // NULLはFALSE

        assertEquals(false, eval("?FALSE").boolean) // FALSEはFALSE

        assertEquals(true, eval("?1").boolean) // 0以外であればTRUE
        assertEquals(false, eval("?0").boolean) // 0はFALSE
        assertEquals(true, eval("?-1").boolean) // 負の数もTRUE

        assertEquals(true, eval("?1.0").boolean) // 0.0以外であればTRUE
        assertEquals(false, eval("?0.0").boolean) // 0.0はFALSE
        assertEquals(true, eval("?-1.0").boolean) // 負の数もTRUE

        assertEquals(true, eval("?TRUE").boolean) // TRUEはTRUE
        assertEquals(false, eval("?FALSE").boolean) // FALSEはFALSE

        assertEquals(true, eval("?'0'").boolean) // '' 以外であればTRUE
        assertEquals(false, eval("?''").boolean) // '' はFALSE
        assertEquals(true, eval("?'FALSE'").boolean) // 'FALSE' もTRUE
        assertEquals(true, eval("?'false'").boolean) // 'false' もTRUE

        assertEquals(false, eval("?(FALSE, FALSE, FALSE)").boolean) // ストリームは各要素のORを取る
        assertEquals(true, eval("?(FALSE, TRUE, FALSE)").boolean) // 1個でもTRUEがあればTRUE
        assertEquals(false, eval("?EMPTY").boolean) // 空ストリームはFALSE


        assertEquals(false, eval("!TRUE").boolean) // TRUEはFALSE
        assertEquals(true, eval("!FALSE").boolean) // FALSEはTRUE
        assertEquals(true, eval("!0").boolean) // ! も論理値に自動変換される


        assertEquals(false, eval("?[]").boolean) // 空配列の論理値化はFALSE
        assertEquals(true, eval("?[NULL]").boolean) // 要素がある配列の論理値化はTRUE
        assertEquals(false, eval("?{}").boolean) // 空オブジェクトの論理値化はFALSE
        assertEquals(true, eval("?{a: 1}").boolean) // 要素があるオブジェクトの論理値化はTRUE
        assertEquals(true, eval("?{`?_`: this -> TRUE}{}").boolean) // `?_` メソッドでオーバーライドできる
        assertEquals(true, eval("TRUE::`?_`()").boolean) // 論理値に対しても `?_` が呼び出せる
    }

    @Test
    fun rightTest() {
        assertEquals("ToNumberGetter[LiteralGetter[100]]", parse("100.+"))

        assertEquals(parse("+100"), parse("100.+"))
        assertEquals(parse("-100"), parse("100.-"))
        assertEquals(parse("?100"), parse("100.?"))
        assertEquals(parse("!100"), parse("100.!"))
        assertEquals(parse("&100"), parse("100.&"))
        assertEquals(parse("$#100"), parse("100.$#"))
        assertEquals(parse("$&100"), parse("100.$&"))
        assertEquals(parse("$*100"), parse("100.$*"))

        assertEquals(parse("-+100"), parse("100.+.-"))
    }

    @Test
    fun toStringTest() = runTest {
        // & で文字列になる
        assertEquals("NULL", eval("&NULL").string)
        assertEquals("10", eval("&10").string)
        assertEquals("TRUE", eval("&TRUE").string)
        assertEquals("FALSE", eval("&FALSE").string)
        assertEquals("abc", eval("&'abc'").string)
        assertEquals("[1;2;3]", eval("&[1, 2, 3]").string)
        assertEquals("{a:1;b:2}", eval("&{a: 1; b: 2}").string)

        assertEquals("10", eval("&{`&_`: this -> &this.a}{a: 10}").string) // 文字列化のオーバーライド

        assertEquals("[1;2;3]", eval("&[1;2;3]").string) // 配列の文字列化
        assertEquals("{a:1;b:2}", eval("&{a:1;b:2}").string) // オブジェクトの文字列化
    }

    @Test
    fun jsonTest() = runTest {
        // $& でFluoriteValueがjson文字列になる
        assertEquals("10", eval("$&10").string) // トップレベルがJsonArrayやJsonObjectでなくてもよい
        assertEquals("10.5", eval("$&10.5").string)
        assertEquals("\"abc\"", eval("$&'abc'").string)
        assertEquals(""""a\"b\nc\\d"""", eval(""" $&"a\"b\nc\\d" """).string)
        assertEquals("true", eval("$&TRUE").string)
        assertEquals("false", eval("$&FALSE").string)
        assertEquals("null", eval("$&NULL").string)
        assertEquals("[1,2,3]", eval("$&[1; 2; 3]").string)
        assertEquals("{\"a\":1,\"b\":2}", eval("$&{a: 1; b: 2}").string)

        // $* でjson文字列がFluoriteValueになる
        assertEquals(10, eval("$*'10'").int)
        assertEquals(10.5, eval("$*'10.5'").double, 0.001)
        assertEquals("abc", eval("$*'\"abc\"'").string)
        assertEquals("a\"b\nc\\d", eval(""" $*'"a\"b\nc\\d"' """).string)
        assertEquals(true, eval("$*'true'").boolean)
        assertEquals(false, eval("$*'false'").boolean)
        assertEquals(FluoriteNull, eval("$*'null'"))
        assertEquals("[1;2;3]", eval("&$*'[1,2,3]'").string)
        assertEquals("{a:1;b:2}", eval("&$*'{\"a\":1,\"b\":2}'").string)
    }

    @Test
    fun exceptionTest() = runTest {
        // !! でオブジェクトをスローするとFluoriteExceptionになって出てくる
        try {
            eval("!!'a'")
            fail()
        } catch (e: FluoriteException) {
            assertEquals("a", e.value.string)
        }

        assertEquals("b", eval("!!'a' !? 'b'").string) // !? で例外をキャッチできる
        assertEquals("b", eval("1 + [2 + !!'a'] !? 'b'").string) // !! は深い階層にあってもよい
        assertEquals("a", eval("!!'a' !? (e => e)").string) // => でスローされた値を受け取れる
        assertEquals(1, eval("a := 1; 1 !? (a = 2); a").int) // !? の右辺は実行されなければ評価自体が行われない
    }

    @Test
    fun accessTest() = runTest {
        assertEquals("b", eval(" 'abc'.1 ").string) // 文字列に数値アクセスするとそのインデックスの文字を得る
        assertEquals(FluoriteNull, eval(" 'abc'.(-1) ")) // 負のインデックスは無効
        assertEquals(FluoriteNull, eval(" 'abc'.3 ")) // 文字列の範囲外にアクセスすると NULL が返る
        assertEquals("c", eval(" 'abc'.(1 + 1) ").string) // キーを ( ) で囲むと式で参照できる

        assertEquals(20, eval(" [10, 20, 30].1 ").int) // 配列に数値アクセスするとそのインデックスの要素を得る
        assertEquals(FluoriteNull, eval(" [10, 20, 30].(-1) ")) // 負のインデックスは無効
        assertEquals(FluoriteNull, eval(" [10, 20, 30].3 ")) // 配列の範囲外にアクセスすると NULL が返る
        assertEquals(30, eval(" [10, 20, 30].(1 + 1) ").int) // キーを ( ) で囲むと式で参照できる

        assertEquals(10, eval(" {a: 10; b: 20}.a ").int) // オブジェクトに識別子アクセスするとその要素を得る
        assertEquals(10, eval(" {a: 10; b: 20}.'a' ").int) // キーは文字列リテラルでもよい
        assertEquals(FluoriteNull, eval(" {a: 10; b: 20}.c ")) // 存在しない要素にアクセスすると NULL が返る
        assertEquals(20, eval(" 'b' | a => {a: 10; b: 20}.(a) ").int) // キーを ( ) で囲むと式で参照できる
    }

    @Test
    fun bracketsAccessTest() = runTest {
        assertEquals("[a;1],[b;2],[c;3]", eval("{a: 1; b: 2; c: 3}()").stream()) // object() でエントリーのストリームにする
        assertEquals(2, eval("{a: 1; b: 2; c: 3}('b')").int) // object(key) で要素を得る
        assertEquals("3,3,1,2", eval("{a: 1; b: 2; c: 3}('c', 'c', 'a', 'b')").stream()) // object(keys) で要素のストリームを得る

        assertEquals("a,b,c", eval("'abc'()").stream()) // string() で文字のストリームにする
        assertEquals("b", eval("'abc'(1)").string) // string(index) で文字を得る
        assertEquals("c", eval("'abc'(-1)").string) // 負のインデックスは後ろから数える
        assertEquals("c,c,a,b", eval("'abc'(2, 2, 0, 1)").stream()) // string(indices) で文字のストリームを得る
    }

    @Test
    fun invokeTest() = runTest {
        assertEquals(123, eval("(a -> a + 23)(100)").int) // function() で関数を呼び出せる
        assertEquals(123, eval("(a -> a + 23)::`_()`(100)").int) // INVOKEメソッドでも関数を呼び出せる
        assertEquals(123, eval("{`_()`: this, a, b -> a + b + 3}{}(100; 20)").int) // INVOKEメソッドを定義したオブジェクトも関数として呼び出せる
        assertEquals(123, eval("{`_()`: this, a, b -> a + b + 3}{}[100](20)").int) // INVOKEメソッドを定義したオブジェクトも部分適用できる
        assertEquals(123, eval("{`_()`: {`_()`: this2, this1, a, b -> a + b}{}}{}(100; 23)").int) // INVOKEの多重追跡
    }

    @Test
    fun powTest() = runTest {
        assertEquals(16.0, eval("4 ^ 2").double, 0.00001) // ^ でべき乗ができる、べき乗すると常に浮動小数点数になる

        // ^ は右優先結合
        assertEquals("PowerGetter[LiteralGetter[1];PowerGetter[LiteralGetter[2];LiteralGetter[3]]]", parse("1 ^ 2 ^ 3"))
        assertEquals(256.0, eval("2 ^ 2 ^ 3").double, 0.00001)
        assertEquals(64.0, eval("(2 ^ 2) ^ 3").double, 0.00001)

        // ^ は乗算よりも優先される
        assertEquals("TimesGetter[TimesGetter[LiteralGetter[1];PowerGetter[LiteralGetter[2];LiteralGetter[3]]];LiteralGetter[4]]", parse("1 * 2 ^ 3 * 4"))
        assertEquals(280.0, eval("5 * 2 ^ 3 * 7").double, 0.00001)

        // ^ は前置演算子よりも優先される
        assertEquals("ToNegativeNumberGetter[PowerGetter[LiteralGetter[1];LiteralGetter[2]]]", parse("- 1 ^ 2"))
        assertEquals(-16.0, eval("- 4 ^ 2").double, 0.00001)

        // ^ の右に前置演算子があってもよい
        assertEquals(0.0625, eval("4 ^ - 2").double, 0.00001)

        assertEquals(432.0, eval("- 2 * - 2 ^ - - 3 * - 3 ^ - - 2 * - 3").double, 0.00001) // 複合的なテスト
    }

    @Test
    fun timesTest() = runTest {
        assertEquals(6, eval("2 * 3").int) // * で乗算ができる

        // どちらかが浮動小数点数なら結果も浮動小数点数になる
        assertEquals(6.0, eval("2.0 * 3").double, 0.001)
        assertEquals(6.0, eval("2 * 3.0").double, 0.001)
        assertEquals(6.0, eval("2.0 * 3.0").double, 0.001)

        assertEquals("abcabcabc", eval("'abc' * 3").string) // 文字列の乗算は繰り返す
        assertEquals("[1;2;3;1;2;3;1;2;3]", eval("[1; 2; 3] * 3").array()) // 配列の乗算は繰り返す
    }

    @Test
    fun modTest() = runTest {
        assertEquals(1, eval("10 % 3").int) // % で余りを得る
        assertEquals(0.25, eval("1.75 % 0.5").double) // 浮動小数点数でもよい
        assertEquals(0.5, eval("2 % 0.75").double) // 右側だけが浮動小数点数でもよい
        assertEquals(0.25, eval("10.25 % 5").double) // 左側だけが浮動小数点数でもよい

        // 負の余りは正になるまで割る数を足したものの余りと同じ（-1 + 3 = 2） % 3
        // そのため同じ余りがループする
        assertEquals("[0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0]", eval("&[-10 .. 10 | _ % 5]").string)

        assertEquals(false, eval("10 %% 3").boolean) // %% は割り切れる場合にTRUE
        assertEquals(true, eval("10 %% 2").boolean) // %% は割り切れない場合にFALSE
        assertEquals(true, eval("-3 %% 3").boolean) // %% も負に対応
        assertEquals(true, eval("10.0 %% 2.0").boolean) // 浮動小数点数として表現されていてもよい
        assertEquals(true, eval("1.75 %% 0.25").boolean) // 2進数で割り切れるのであれば小数でもよい
        assertEquals(true, eval("25 %% 0.25").boolean) // 右側だけが浮動小数点数でもよい
        assertEquals(true, eval("25.0 %% 5").boolean) // 左側だけが浮動小数点数でもよい
    }

    @Test
    fun stringConcatenateTest() = runTest {
        assertEquals("ab", eval(" 'a' & 'b' ").string) // & で文字列の連結ができる
        assertEquals("12", eval(" 1 & 2 ").string) // 文字列に変換する
    }

    @Test
    fun rangeTest() = runTest {
        assertEquals("1,2,3,4", eval("1 .. 4").stream()) // .. でその範囲をイテレートするストリームを得る
        assertEquals("0,1,2,3", eval("0 .. 4 - 1").stream()) // 項は0や四則演算等でもよい
        assertEquals("1", eval("1 .. 1").stream()) // 範囲が1つの要素の場合はその要素のみのストリームを返す
        assertEquals("-1,0,1", eval("-1 .. 1").stream()) // 項は前置演算子がついていたり、負の値でもよいし、正負をまたいでもよい
        assertEquals("[1;2;3;4],[1;2;3;4]", eval("a := 1 .. 4; [a], [a]").stream()) // 範囲ストリームは再利用できる
        assertEquals("4,3,2,1", eval("4 .. 1").stream()) // 下降も可能

        assertEquals("1,2,3", eval("1 ~ 4").stream()) // 半開区間演算子は終端を含まない
        assertEquals("", eval("1 ~ 1").stream()) // 範囲が一つの場合は空ストリームになる
        assertEquals("[1;2;3],[1;2;3]", eval("a := 1 ~ 4; [a], [a]").stream()) // 再利用のテスト
        assertEquals("", eval("4 ~ 1").stream()) // 右辺が左辺より小さい場合は空ストリームになる
    }

    @Test
    fun compareTest() = runTest {
        fun String.f() = this.replace(" ", "")

        // 比較ができる
        assertEquals("[FALSE;FALSE;TRUE ]".f(), eval("[0 >  1; 1 >  1; 2 >  1]").array())
        assertEquals("[TRUE ;FALSE;FALSE]".f(), eval("[0 <  1; 1 <  1; 2 <  1]").array())
        assertEquals("[FALSE;TRUE ;TRUE ]".f(), eval("[0 >= 1; 1 >= 1; 2 >= 1]").array())
        assertEquals("[TRUE ;TRUE ;FALSE]".f(), eval("[0 <= 1; 1 <= 1; 2 <= 1]").array())

        // 浮動小数でもよい
        assertEquals("[FALSE;FALSE;FALSE]".f(), eval("[1 >  1.0; 1.0 >  1; 1.0 >  1.0]").array())
        assertEquals("[FALSE;FALSE;FALSE]".f(), eval("[1 <  1.0; 1.0 <  1; 1.0 <  1.0]").array())
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), eval("[1 >= 1.0; 1.0 >= 1; 1.0 >= 1.0]").array())
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), eval("[1 <= 1.0; 1.0 <= 1; 1.0 <= 1.0]").array())

        // 比較のオーバーライド
        """
            LengthComparing := {
                `_<=>_`: this, other -> $#this.string <=> $#other.string
            }
            z := LengthComparing{string: "z"}
            a := LengthComparing{string: "a"}
            aaa := LengthComparing{string: "aaa"}
            [
                z > a,
                z < a,
                z >= a,
                z <= a,
                z > aaa,
                z < aaa,
                z >= aaa,
                z <= aaa,
            ]
        """.let { assertEquals("[FALSE;FALSE;TRUE;TRUE;FALSE;TRUE;FALSE;TRUE]", eval(it).array()) }
    }

    @Test
    fun containsTest() = runTest {
        // string @ string で部分一致
        assertEquals(true, eval("'abc' @ '---abc---'").boolean)
        assertEquals(false, eval("'123' @ '---abc---'").boolean)

        // value @ array で要素が含まれているかどうか
        assertEquals(true, eval("30 @ [10, 20, 30]").boolean)
        assertEquals(false, eval("40 @ [10, 20, 30]").boolean)

        // key @ object で要素が含まれているかどうか
        assertEquals(true, eval("'a' @ {a: 10; b: 20}").boolean)
        assertEquals(false, eval("'c' @ {a: 10; b: 20}").boolean)

        // CONTAINSメソッドで上書きできる
        assertEquals(true, eval("'abc' @ {`_@_`: this, value -> value @ '---abc---'}{}").boolean)
        assertEquals(false, eval("'123' @ {`_@_`: this, value -> value @ '---abc---'}{}").boolean)
    }

    @Test
    fun andOrTest() = runTest {
        // && は左辺がTRUEの場合に右辺を、 || は左辺がFALSEの場合に右辺を返す
        assertEquals(0, eval("0 && 2").int)
        assertEquals(2, eval("1 && 2").int)
        assertEquals(2, eval("0 || 2").int)
        assertEquals(1, eval("1 || 2").int)

        // 評価されない右辺は副作用も発生させない
        assertEquals(1, eval("a := 1; 1 || (a = 2); a").int)
        assertEquals(1, eval("a := 1; 0 && (a = 2); a").int)

        // 結合優先度のテスト
        assertEquals(true, eval("1 < 2 && 1 < 2").boolean)
        assertEquals(false, eval("1 < 2 && 2 < 1").boolean)
        assertEquals(false, eval("2 < 1 && 1 < 2").boolean)
        assertEquals(false, eval("2 < 1 && 2 < 1").boolean)
        assertEquals(true, eval("1 < 2 || 1 < 2").boolean)
        assertEquals(true, eval("1 < 2 || 2 < 1").boolean)
        assertEquals(true, eval("2 < 1 || 1 < 2").boolean)
        assertEquals(false, eval("2 < 1 || 2 < 1").boolean)
        assertEquals(1, eval("0 && 0 || 1").int)
        assertEquals(0, eval("0 && (0 || 1)").int)
        assertEquals(1, eval("1 || 0 && 0").int)
        assertEquals(0, eval("(1 || 0) && 0").int)
    }

    @Test
    fun conditionTest() = runTest {
        // ? : で条件分岐ができる
        assertEquals(1, eval("TRUE ? 1 : 2").int)
        assertEquals(2, eval("FALSE ? 1 : 2").int)

        // ? : を入れ子にすると右側が優先的にくっつく
        assertEquals(1, eval("TRUE ? TRUE ? 1 : 2 : TRUE ? 3 : 4").int)
        assertEquals(2, eval("TRUE ? FALSE ? 1 : 2 : FALSE ? 3 : 4").int)
        assertEquals(3, eval("FALSE ? TRUE ? 1 : 2 : TRUE ? 3 : 4").int)
        assertEquals(4, eval("FALSE ? FALSE ? 1 : 2 : FALSE ? 3 : 4").int)

        assertEquals(1, eval("1 ?: 2").int) // ?: の左辺が非NULLの場合、左辺を得る
        assertEquals(2, eval("NULL ?: 2").int) // ?: の左辺がNULLの場合、右辺を得る
        assertEquals(false, eval("FALSE ?: 2").boolean) // FALSEは非NULLである

        // 三項演算子とエルビス演算子は混ぜて書ける
        assertEquals(1, eval("TRUE ? 1 ?: 2 : 3 ?: 4").int)
        assertEquals(2, eval("TRUE ? NULL ?: 2 : NULL ?: 4").int)
        assertEquals(3, eval("FALSE ? 1 ?: 2 : 3 ?: 4").int)
        assertEquals(4, eval("FALSE ? NULL ?: 2 : NULL ?: 4").int)

        assertEquals(4, eval("FALSE ? 1 : NULL ?: FALSE ? 3 : 4").int) // == FALSE ? 1 : (NULL ?: (FALSE ? 3 : 4))

        // 条件項は `?_` で論理値に変換される
        assertEquals(1, eval("{`?_`: _ -> TRUE}{} ? 1 : 2").int)
        assertEquals(2, eval("{`?_`: _ -> FALSE}{} ? 1 : 2").int)

        // 評価されない項は副作用も起こさない
        assertEquals(2, eval("a := 1; TRUE ? (a = 2) : 0; a").int)
        assertEquals(1, eval("a := 1; FALSE ? (a = 2) : 0; a").int)
        assertEquals(1, eval("a := 1; TRUE ? 0 : (a = 2); a").int)
        assertEquals(2, eval("a := 1; FALSE ? 0 : (a = 2); a").int)
        assertEquals(2, eval("a := 1; NULL ?: (a = 2); a").int)
        assertEquals(1, eval("a := 1; 0 ?: (a = 2); a").int)
    }

    @Test
    fun pipeTest() = runTest {
        // パイプ
        assertEquals("10,20,30", eval("1 .. 3 | _ * 10").stream()) // 左辺のストリームを変換する
        assertEquals(10, eval("1 | _ * 10").int) // 左辺が非ストリームなら、出力をストリームで梱包しない
        assertEquals("", eval("(,) | _ * 10").stream()) // 左辺が空ストリームなら、出力も空ストリームになる

        // 実行パイプ
        assertEquals("1:2:3", eval(""" 1, 2, 3 >> JOIN[":"] """).string) // >> で右辺の関数に左辺を適用する
        assertEquals(10.0, eval("100 >> SQRT").double, 0.001) // 右辺は非ストリーム用の関数でもよい
        assertEquals(20, eval("10 >> x -> x * 2").int) // 右辺はラムダでもよい

        // 左実行パイプ
        assertEquals("1:2:3", eval(""" JOIN[":"] << 1, 2, 3 """).string) // << は左右が逆になっただけだが、結合優先度が代入と同等
        assertEquals(2.0, eval(""" SQRT << SQRT << 16 """).double, 0.001) // << を並べると、右から左に実行される

        // パイプの連結
        assertEquals("55:33:11", eval(""" JOIN[":"] << 1 .. 5 >> FILTER[x -> !(x %% 2)] | _ & _ >> REVERSE """).string)

        // パイプと代入系演算子は相互に右優先結合だが、パイプ同士の連結部分だけは左優先結合になる
        assertEquals("1-21-2", eval("x := 0; f := s -> s >> SPLIT[','] >> JOIN['-'] | x = _ | _ * 2; f('1,2'); x").string)

        // パイプと引数指定演算子との組み合わせ
        assertEquals("10-20-30", eval(""" 1 .. 3 | x => x * 10 >> JOIN["-"] """).string)

        // パイプを多段にしても前の段の引数が見える
        assertEquals("14,15,16,24,25,26,34,35,36", eval("1 .. 3 | x => 4 .. 6 | y => x & y").stream())

        // パイプと実行パイプの組み合わせ
        assertEquals(4, eval("1 | _ + 2 | _ * 3 >> SQRT | _ + 5 | _ + 8 >> SQRT >> FLOOR").int)
        assertEquals("18:19:28:29", eval("f := () -> '1-2' >> SPLIT['-'] | x => 8, 9 | y => x & y >> a -> JOIN(':'; a); f()").string)
    }

    @Test
    fun streamTest() = runTest {
        assertEquals("1,2,3", eval("1, 2, 3").stream()) // , でストリームが作れる
        assertEquals("1,2,3", eval(", , 1, 2, , , 3, , ").stream()) // , は無駄に大量にあってもよい
        assertEquals("1,2,3,4,5,6,7,8,9", eval("(1, 2), 3, ((4 .. 6), 7, (8, 9))").stream()) // ストリームを結合すると自動的に平坦になる
        assertEquals("", eval(",").stream()) // 単体の , で空ストリームになる
        assertEquals("1", eval("1,").stream()) // 値に , を付けると単独でストリームになる
    }

    @Test
    fun builtInClassTest() = runTest {
        // 各クラスのtrue判定
        assertEquals(true, eval("1 ?= VALUE").boolean)
        assertEquals(true, eval("NULL ?= NULL_CLASS").boolean)
        assertEquals(true, eval("1 ?= INT").boolean)
        assertEquals(true, eval("1.2 ?= DOUBLE").boolean)
        assertEquals(true, eval("TRUE ?= BOOLEAN").boolean)
        assertEquals(true, eval("'a' ?= STRING").boolean)
        assertEquals(true, eval("[1] ?= ARRAY").boolean)
        assertEquals(true, eval("{a: 1} ?= OBJECT").boolean)
        assertEquals(true, eval("(a -> 1) ?= FUNCTION").boolean)
        assertEquals(true, eval("(1, 2) ?= STREAM").boolean)

        // falseテスト
        assertEquals(false, eval("'10' ?= INT").boolean)
        assertEquals(false, eval("10 ?= STRING").boolean)
        assertEquals(false, eval("(1, 2) ?= ARRAY").boolean)
        assertEquals(false, eval("1.2 ?= INT").boolean)
        assertEquals(false, eval("1 ?= DOUBLE").boolean)
    }

    @Test
    fun variableTest() = runTest {
        assertEquals(10, eval("a := 10; a").int) // := で変数を定義できる
        assertEquals(20, eval("a := 10; a = 20; a").int) // = で既存の変数に代入できる
        assertEquals(10, eval("a := 10; (a := 20; a = 30); a").int) // 変数は ( ) の外部に伝搬しない
    }

    @Test
    fun lambdaTest() = runTest {
        assertEquals(10, eval("((a) -> a)(10)").int) // (a) -> b で関数を作り、 f(a) で実行する
        assertEquals(12, eval("((a; b) -> a * b)(3; 4)").int) // ; で引数を複数取れる
        assertEquals(12, eval("((a, b) -> a * b)(3; 4)").int) // ラムダ引数は , で区切ってもよい
        assertEquals(10, eval("(() -> 10)()").int) // () で引数を無しにできる
        assertEquals(10, eval("(a -> a)(10)").int) // 引数がある場合、 ( ) は省略してもよい
        assertEquals(12, eval("(a, b -> a * b)(3; 4)").int) // 引数が複数の場合も ( ) を省略できる
        assertEquals(12, eval("((, a, , b, ) -> a * b)(3; 4)").int) // 区切り文字を余計に書いてもよい
        assertEquals(10, eval("(, -> 10)()").int) // , 1個でも引数無しを表せる

        assertEquals("[1;2;3;4]", eval("(s -> &[s])(1, 2, 3, 4)").string) // 引数で , を使うとストリームを渡せる

        assertEquals("[1;2;3;4;5]", eval("(() -> &__)(1; 2; 3; 4; 5)").string) // __ で引数を配列で受け取れる
        assertEquals("[4;5;6]", eval("(() -> &[__.1])(1 .. 3; 4 .. 6; 7 .. 9)").string) // 引数列ではストリームを展開しない

        assertEquals(120, eval("f := n -> n == 0 ? 1 : n * f(n - 1); f(5)").int) // 再帰関数の例
        assertEquals(120, eval("(f -> f(f))(f -> n -> n == 0 ? 1 : n * f(f)(n - 1))(5)").int) // 複雑なラムダ計算の例

        // 同じ関数を再帰的に2度呼び出した場合に、関数のフレームが分離されているかどうかのテスト
        assertEquals("[2;1;2]", eval("f := n -> n == 1 ? 1 : [n, f(1), n]; f(2)").array())
    }

    @Test
    fun methodTest() = runTest {
        assertEquals(10, eval("{method: () -> 10}{}::method()").int) // a::b() でaのbを呼び出せる
        assertEquals(10, eval("{method: this -> this.a}{a: 10}::method()").int) // メソッド関数は最初の引数にthisを受け取る
        assertEquals(20, eval("{method: this, b -> this.a * b}{a: 10}::method(2)").int) // 2個目以降の引数にメソッド呼び出し時の引数を受け取る

        assertEquals("10", eval("10::`&_`()").string) // 組み込みメソッドの呼び出し

        assertEquals(10, eval("A := {m: _ -> _.v}; a := A {v: 10}; a::m()").int) // メソッドの継承

        assertEquals("{&_:1}", eval("&{`&_`: 1}").string) // オブジェクトキーがメソッド名と衝突する場合でもオーバーライドしない

        assertEquals(6, eval("mul := a, b -> a * b; 2::(mul)(3)").int) // 関数へのメソッド呼び出し
        assertEquals(6, eval("2::(a, b -> a * b)(3)").int) // ラムダ式を使った関数へのメソッド呼び出し
    }

    @Test
    fun methodReferenceTest() = runTest {
        assertEquals(123, eval("({m: this, y -> this.x + y + 3}{x: 100}::m)(20)").int) // メソッド参照

        // メソッド参照を使った左実行パイプ
        """
            out := []
            out::push << "1"
            out::push << "a"
            out
        """.let { assertEquals("[1;a]", eval(it).array()) }

        assertEquals(6, eval("mul := a, b -> a * b; f := 2::(mul); f(3)").int) // 関数へのメソッド参照
    }

    @Test
    fun bindTest() = runTest {
        assertEquals("12", eval("(a, b -> a & b)[1](2)").string) // [ ] で関数に部分適用できる
        assertEquals("12", eval("(a, b -> a & b)[1; 2]()").string) // [ ] の中に複数の引数があってもよい
        assertEquals("12", eval("(a, b -> a & b)[](1; 2)").string) // [ ] の中が空でもよい
        assertEquals("12", eval("(a, b -> a & b)[1][2]()").string) // [ ] を連続して書いてもよい
        assertEquals("12", eval("{m: _, a, b -> a & b}{}::m[1](2)").string) // メソッド呼び出しにも使用できる
    }

    @Test
    fun nullSafeTest() = runTest {
        assertEquals("1,NULL,3", eval("{v:1},NULL,{v:3}|_?.v").stream()) // Null安全要素アクセス
        assertEquals("1,NULL,3", eval("{m:_->1}{},NULL,{m:_->3}{}|_?::m()").stream()) // Null安全メソッド呼び出し
        assertEquals("1,NULL,3", eval("{m:_->1}{},NULL,{m:_->3}{}|_?::m[]()").stream()) // Null安全メソッド部分適用
        assertEquals("1,NULL,3", eval("{m:_->1}{},NULL,{m:_->3}{}|(_?::m)()").stream()) // Null安全メソッド参照
        assertEquals("1,NULL,3", eval("m:=_->_.x;{x:1},NULL,{x:3}|_?::(m)()").stream()) // 関数へのNull安全メソッド呼び出し
        assertEquals("1,NULL,3", eval("m:=_->_.x;{x:1},NULL,{x:3}|_?::(m)[]()").stream()) // 関数へのNull安全メソッド部分適用
        assertEquals("1,NULL,3", eval("m:=_->_.x;{x:1},NULL,{x:3}|(_?::(m))()").stream()) // 関数へのNull安全メソッド参照
    }

    @Test
    fun runnerTest() = runTest {
        assertEquals(55, eval("x := 0; 1 .. 10 | x = x + _; x").int) // runner部分がストリームの式だった場合、イテレーションはする
    }

    @Test
    fun rootTest() = runTest {
        assertEquals(10, eval("10").int) // 式を書ける
        assertEquals(20, eval("10; 20").int) // ; で区切ると左は式文になり、右が使われる
        assertEquals(20, eval("10\n20").int) // 改行で区切ってもよい
        assertEquals(FluoriteNull, eval("10;")) // 式を省略した場合、NULLになる
        assertEquals(10, eval("10\n").int) // 式の前後に余計な改行があっても無視される
        assertEquals(FluoriteNull, eval("10;\n")) // ; の後に改行があった場合もNULLになる
        assertEquals(30, eval("10; 20; 30").int) // ; が複数あってもよい
        assertEquals(20, eval("; 10; ; 20").int) // ; の左は省略されていてもよい
        assertEquals(20, eval("\n\n;;\n\n10\n\n;;\n\n;;\n\n20\n\n").int) // 改行と;が無駄に大量にあってもよい
        assertEquals(FluoriteNull, eval("")) // 何も書かない場合、NULLになる
        assertEquals(FluoriteNull, eval(" \t\n ")) // 空白を書いても何もないのと同じになる
    }

    @Test
    fun mathTest() = runTest {
        assertEquals(3.141592653589793, eval("MATH.PI").double, 0.001)
        assertEquals(2.718281828459045, eval("MATH.E").double, 0.001)

        assertEquals(3.141592653589793, eval("PI").double, 0.001)

        // SQRT
        assertEquals(10.0, eval("SQRT(100)").double, 0.001)

        // SIN
        assertEquals(0.0, eval("SIN(0)").double, 0.001)
        assertEquals(1.0, eval("SIN(PI / 2)").double, 0.001)

        // COS
        assertEquals(1.0, eval("COS(0)").double, 0.001)
        assertEquals(-1.0, eval("COS(PI)").double, 0.001)

        // TAN
        assertEquals(1.0, eval("TAN(PI / 4)").double, 0.001)

        // LOG (自然対数)
        assertEquals(1.0, eval("LOG(MATH.E)").double, 0.001)
    }

    @Test
    fun arrayFunctionTest() = runTest {
        assertEquals("[1;2;3]", eval("TO_ARRAY(1, 2, 3)").array()) // ARRAY関数はストリームを配列にする
        assertEquals("[100]", eval("TO_ARRAY(100)").array()) // ストリームでなくてもよい
        assertEquals("[10;20;30]", eval("1 .. 3 | _ * 10 >> TO_ARRAY").array()) // ARRAY関数はパイプ演算子と組み合わせて使うと便利
    }

    @Test
    fun objectFunctionTest() = runTest {
        assertEquals("{a:1;b:2;c:3}", eval("TO_OBJECT((a: 1), (b: 2), (c: 3))").obj) // OBJECT関数はストリームをオブジェクトにする
        assertEquals("{a:100}", eval("TO_OBJECT(a: 100)").obj) // ストリームでなくてもよい
        assertEquals("{1:10;2:20;3:30}", eval("1 .. 3 | ((_): _ * 10) >> TO_OBJECT").obj) // OBJECT関数はパイプ演算子と組み合わせて使うと便利
    }

    @Test
    fun floorFunctionTest() = runTest {
        assertEquals(10, eval("FLOOR(10.1)").int) // FLOOR関数は小数点以下を切り捨てて内部的な型をINTEGERにする
        assertEquals(10, eval("FLOOR(10)").int) // 整数はそのまま
        assertEquals(-11, eval("FLOOR(-10.1)").int) // 負の数も値が小さくなるように切り捨てる
    }

    @Test
    fun divFunctionTest() = runTest {
        assertEquals(3, eval("DIV(10; 3)").int) // DIV関数は小数点以下を絶対値の小さい方に切り捨てる

        // 負の場合は符号だけが変わる
        assertEquals(3, eval("DIV(10; 3)").int)
        assertEquals(-3, eval("DIV(10; -3)").int)
        assertEquals(-3, eval("DIV(-10; 3)").int)
        assertEquals(3, eval("DIV(-10; -3)").int)

        // 浮動小数点の場合も整数化する
        assertEquals(3, eval("DIV(10; 3)").int)
        assertEquals(3.0, eval("DIV(10; 3.0)").double)
        assertEquals(3.0, eval("DIV(10.0; 3)").double)
        assertEquals(3.0, eval("DIV(10.0; 3.0)").double)
        assertEquals(-3, eval("DIV(-10; 3)").int)
        assertEquals(-3.0, eval("DIV(-10; 3.0)").double)
        assertEquals(-3.0, eval("DIV(-10.0; 3)").double)
        assertEquals(-3.0, eval("DIV(-10.0; 3.0)").double)
    }

    @Test
    fun randomFunctionTest() = runTest {
        val random = eval("RAND")

        repeat(100) {
            val d = random.invoke(arrayOf()).double
            assertTrue(d >= 0.0 && d < 1.0)
        }
        repeat(100) {
            val i = random.invoke(arrayOf(FluoriteInt(4))).int
            assertTrue(i >= 0 && i < 4)
        }
        repeat(100) {
            val i = random.invoke(arrayOf(FluoriteInt(4), FluoriteInt(10))).int
            assertTrue(i >= 4 && i < 10)
        }
    }

    @Test
    fun jsonFunctionTest() = runTest {
        // JSON
        assertEquals("""{"a":[1,2.5,"3",true,false,null]}""", eval(""" {a: [1, 2.5, "3", TRUE, FALSE, NULL]} >> JSON """).string) // JSON で値をJson文字列に変換する
        assertEquals("1", eval("1 >> JSON").string) // プリミティブを直接指定できる
        assertEquals("[\n  1,\n  [\n    2,\n    3\n  ],\n  4\n]", eval(""" [1, [2, 3], 4] >> JSON[indent: "  "] """).string) // indentを指定できる
        assertEquals("[1],[2],[3]", eval("[1], [2], [3] >> JSON").stream()) // ストリームを指定するとJsonのストリームになる

        // JSOND
        assertEquals("""{a:[1;2.5;3;TRUE;FALSE;NULL]}""", eval(""" '{"a":[1,2.5,"3",true,false,null]}' >> JSOND """).obj) // JSOND でJson文字列を値に変換する
        assertEquals(1, eval(""" "1" >> JSOND """).int) // プリミティブを直接指定できる
        assertEquals("[1],[2],[3]", eval(""" "[1]", "[2]", "[3]" >> JSOND """).stream()) // Jsonのストリームを指定するとストリームになる
    }

    @Test
    fun joinSplitTest() = runTest {
        // JOIN
        assertEquals("a|b|c", eval(""" JOIN("|"; "a", "b", "c") """).string) // JOIN で文字列を結合できる
        assertEquals("abc", eval(""" JOIN(""; "a", "b", "c") """).string) // セパレータは空文字でもよい
        assertEquals("a123b123c", eval(""" JOIN("123"; "a", "b", "c") """).string) // セパレータは複数文字でもよい
        assertEquals("a|b", eval(""" JOIN("|"; "a", "b") """).string) // ストリームは2要素でもよい
        assertEquals("a", eval(""" JOIN("|"; "a",) """).string) // ストリームは1要素でもよい
        assertEquals("", eval(""" JOIN("|"; ,) """).string) // ストリームは空でもよい
        assertEquals("a", eval(""" JOIN("|"; "a") """).string) // ストリームは非ストリームでもよい
        assertEquals("10|[20]|30", eval(""" JOIN("|"; 10, [20], {`&_`: _ -> 30}{}) """).string) // ストリームは文字列化される
        assertEquals("a1b1c", eval(""" JOIN(1; "a", "b", "c") """).string) // セパレータも文字列化される
        assertEquals("a|b|c", eval(""" JOIN["|"]("a", "b", "c") """).string) // 部分適用を使用した例
        assertEquals("a,b,c", eval(""" JOIN("a", "b", "c") """).string) // 引数を省略した場合はカンマ区切りになる

        // SPLIT
        assertEquals("a,b,c", eval(""" SPLIT("|"; "a|b|c") """).stream()) // SPLIT で文字列を分割できる
        assertEquals("a,b,c", eval(""" SPLIT(""; "abc") """).stream()) // セパレータは空文字でもよい
        assertEquals("a,b,c", eval(""" SPLIT("123"; "a123b123c") """).stream()) // セパレータは複数文字でもよい
        assertEquals("a,b", eval(""" SPLIT("|"; "a|b") """).stream()) // 文字列は2要素でもよい
        assertEquals("a", eval(""" SPLIT("|"; "a") """).stream()) // 文字列は1要素でもよい
        assertEquals("", eval(""" SPLIT("|"; "") """).stream()) // 文字列は空でもよい
        assertEquals("1,2,3", eval(""" SPLIT("0"; 10203) """).stream()) // 文字列は文字列化される
        assertEquals("a,b,c", eval(""" SPLIT(1; "a1b1c") """).stream()) // セパレータは文字列化される
        assertEquals("a,b,c", eval(""" SPLIT["|"]("a|b|c") """).stream()) // 部分適用を使用した例
        assertEquals("a,b,c", eval(""" SPLIT("a,b,c") """).stream()) // 引数を省略した場合はカンマ区切りになる

        // パイプ連携
        assertEquals("10ABC20ABC30", eval(""" "10abc20abc30" >> SPLIT["abc"] >> JOIN["ABC"] """).string)
    }

    @Test
    fun csvTest() = runTest {
        assertEquals("""a,b""", eval(""" ["a","b"] >> CSV """).string) // CSV で配列を文字列に変換できる
        assertEquals("""["a","b"]""", eval(""" "a,b" >> CSVD >> JSON """).string) // CSVD で文字列を配列に変換できる

        // ストリームは各要素が変換される
        assertEquals("""a,b,c,d""", eval(""" ["a","b"],["c","d"] >> CSV """).stream())
        assertEquals("""["a","b"],["c","d"]""", eval(""" "a,b","c,d" >> CSVD >> JSON """).stream())

        // 空文字列は空文字列を1個含む配列になる
        assertEquals("", eval(""" [""] >> CSV """).string)
        assertEquals("""[""]""", eval(""" "" >> CSVD >> JSON """).string)

        // 区切り文字を含むセルはクォートされる
        assertEquals("\"a,b\"", eval(""" ["a,b"] >> CSV """).string)
        assertEquals("""["a,b"]""", eval(""" "\"a,b\"" >> CSVD >> JSON """).string)

        // クォートを含むセルはクォートされ、クォートが2重になる
        assertEquals("\"a\"\"b\"", eval(""" ["a\"b"] >> CSV """).string)
        assertEquals("""["a\"b"]""", eval(""" "\"a\"\"b\"" >> CSVD >> JSON """).string)

        // 前後に半角空白やタブがあるセルはクォートされる
        assertEquals("\" a \",\"\tb\t\"", eval(""" [" a ","\tb\t"] >> CSV """).string)
        assertEquals("""[" a ","\tb\t"]""", eval(""" "\" a \",\"\tb\t\"" >> CSVD >> JSON """).string)

        // 改行を含むセルはクォートされる
        assertEquals("\"a\r\n\",\"\nb\"", eval(""" ["a\r\n","\nb"] >> CSV """).string)
        assertEquals("""["a\r\n","\nb"]""", eval(""" "\"a\r\n\",\"\nb\"" >> CSVD >> JSON """).string)

        // 区切り文字とクォート文字の指定
        assertEquals("%a|%|%%%b%", eval(""" ["a|","%b"] >> CSV[separator: "|"; quote: "%"] """).string)
        assertEquals("""["a|","%b"]""", eval(""" "%a|%|%%%b%" >> CSVD[separator: "|"; quote: "%"] >> JSON """).string)

        // CSVDのフォーマット
        assertEquals("""["a","","b"]""", eval(""" "a,,b" >> CSVD >> JSON """).string) // 空のセクションは空文字列になる
        assertEquals("""["","a","b"]""", eval(""" ",a,b" >> CSVD >> JSON """).string) // 先頭のカンマの前は空文字列になる
        assertEquals("""["a","b",""]""", eval(""" "a,b," >> CSVD >> JSON """).string) // 末尾のカンマの後は空文字列になる
        assertEquals("""["","a","","c",""]""", eval(""" " , a , , c , " >> CSVD >> JSON """).string) // 余計な空白はトリムされる

        assertEquals("""["","a","","b",""]""", eval(""" " \t a \t \t b \t " >> CSVD[separator: "\t"] >> JSON """).string) // 区切り文字がタブの場合、タブを空白文字扱いしない
        assertEquals("""["","a","","b",""]""", eval(""" "\t \ta\t \t \tb\t \t" >> CSVD[separator: " "] >> JSON """).string) // 区切り文字が半角空白の場合、半角空白を空白文字扱いしない
    }

    @Test
    fun chunkTest() = runTest {
        assertEquals("[1;2],[3;4]", eval("CHUNK(2; 1, 2, 3, 4)").stream()) // CHUNK でストリームを分割する
        assertEquals("[1;2],[3;4],[5]", eval("CHUNK(2; 1, 2, 3, 4, 5)").stream()) // 要素が余る場合、余った部分だけの配列を生成する
        assertEquals("[1;2]", eval("CHUNK(2; 1, 2)").stream()) // 全体の要素数が一致している場合、その配列になる
        assertEquals("[1;2]", eval("CHUNK(4; 1, 2)").stream()) // 全体の要素数が足りない場合、その配列になる
        assertEquals("[1]", eval("CHUNK(2; 1)").stream()) // 第2引数が非ストリームの場合でもストリームの場合と同様に動作する
        assertEquals("", eval("CHUNK(2; ,)").stream()) // 空ストリームの場合、空ストリームになる
    }

    @Test
    fun takeTest() = runTest {
        assertEquals("1,2", eval("TAKE(2; 1, 2, 3)").stream()) // TAKE で先頭を取得
        assertEquals("1,2", eval("TAKE(2; 1, 2)").stream()) // 要素が丁度の場合はそのまま返す
        assertEquals("1", eval("TAKE(2; 1)").stream()) // 要素が足りない場合はある分だけ返す
        assertEquals("", eval("TAKE(0; 1, 2)").stream()) // 0個取得の場合は空ストリームになる
        assertEquals("", eval("TAKE(2; ,)").stream()) // 空ストリームの場合、空ストリームになる

        assertEquals("2,3", eval("TAKER(2; 1, 2, 3)").stream()) // TAKER で末尾を取得
        assertEquals("1,2", eval("TAKER(2; 1, 2)").stream()) // 要素が丁度の場合はそのまま返す
        assertEquals("1", eval("TAKER(2; 1)").stream()) // 要素が足りない場合はある分だけ返す
        assertEquals("", eval("TAKER(0; 1, 2)").stream()) // 0個取得の場合は空ストリームになる
        assertEquals("", eval("TAKER(2; ,)").stream()) // 空ストリームの場合、空ストリームになる

        assertEquals("3", eval("DROP(2; 1, 2, 3)").stream()) // DROP で先頭を破棄
        assertEquals("", eval("DROP(2; 1, 2)").stream()) // 要素が丁度の場合は空ストリームになる
        assertEquals("", eval("DROP(2; 1)").stream()) // 要素が足りない場合は空ストリームになる
        assertEquals("1,2", eval("DROP(0; 1, 2)").stream()) // 0個破棄の場合は元のストリームになる
        assertEquals("", eval("DROP(2; ,)").stream()) // 空ストリームの場合、空ストリームになる

        assertEquals("1", eval("DROPR(2; 1, 2, 3)").stream()) // DROPR で末尾を破棄
        assertEquals("", eval("DROPR(2; 1, 2)").stream()) // 要素が丁度の場合は空ストリームになる
        assertEquals("", eval("DROPR(2; 1)").stream()) // 要素が足りない場合は空ストリームになる
        assertEquals("1,2", eval("DROPR(0; 1, 2)").stream()) // 0個破棄の場合は元のストリームになる
        assertEquals("", eval("DROPR(2; ,)").stream()) // 空ストリームの場合、空ストリームになる
    }

    @Test
    fun filterTest() = runTest {
        assertEquals("2,4", eval("1 .. 5 >> FILTER [ x => x %% 2 ]").stream()) // FILTER で条件を満たす要素のみを抽出する
    }

    @Test
    fun keysValuesTest() = runTest {
        assertEquals("a,b,c", eval("KEYS({a: 1; b: 2; c: 3})").stream()) // KEYS でオブジェクトのキーを得る
        assertEquals("1,2,3", eval("VALUES({a: 1; b: 2; c: 3})").stream()) // VALUES でオブジェクトの値を得る
    }

    @Test
    fun sumFunctionTest() = runTest {
        assertEquals(0, eval("SUM(,)").int) // 引数がない場合は0
        assertEquals(1, eval("SUM(1)").int) // 引数が1つの場合はそのまま
        assertEquals(3, eval("SUM(1, 2)").int) // 引数が2つ以上の場合は合計
    }

    @Test
    fun countFunctionTest() = runTest {
        assertEquals(0, eval("COUNT(,)").int) // 空ストリームなら0
        assertEquals(1, eval("COUNT(1)").int) // 非ストリームなら1
        assertEquals(2, eval("COUNT(1, 2)").int) // 複数要素なら個数
    }

    @Test
    fun reverseTest() = runTest {
        assertEquals("3,2,1", eval("REVERSE(1, 2, 3)").stream()) // REVERSE でストリームを逆順にする
        assertEquals("3:2:1", eval(" '1-2-3' >> SPLIT['-'] >> REVERSE >> JOIN[':'] ").string) // REVERSE はパイプと組み合わせて使うと便利
    }

    @Test
    fun distinctTest() = runTest {
        assertEquals("1,2,3,0", eval("1, 2, 3, 3, 3, 2, 1, 0 >> DISTINCT").stream()) // DISTINCT で重複を除去する
        assertEquals(1, eval("1 >> DISTINCT").int) // 非ストリームの場合、それがそのまま出てくる
        assertEquals("", eval(", >> DISTINCT").stream()) // 空ストリームの場合、空ストリームになる
    }

    @Test
    fun minMaxTest() = runTest {
        assertEquals(1.0, eval("MIN(1.0, 2.0, 3.0)").double) // MIN で最小値を得る
        assertEquals(FluoriteNull, eval("MIN(,)")) // 空ストリームの場合、NULL
        assertEquals(3.0, eval("MAX(1.0, 2.0, 3.0)").double) // MAX で最大値を得る
        assertEquals(FluoriteNull, eval("MAX(,)")) // 空ストリームの場合、NULL
    }

    @Test
    fun tryRunnerTest() = runTest {
        assertEquals("end", eval("(1 .. 3 | !!'error') !? 'ignore'; 'end'").string) // パイプRunnerの中でエラーが発生してもキャッチできる
    }

    @Test
    fun objectAssignmentTest() = runTest {
        assertEquals("{a:1;b:9}", eval("o := {a: 1; b: 2}; o.b = 9; o").obj) // オブジェクトのフィールドに代入できる
        assertEquals("{a:1;b:2;c:9}", eval("o := {a: 1; b: 2}; o.c = 9; o").obj) // 存在しないフィールドに代入すると新規追加される
    }

    @Test
    fun subString() = runTest {
        assertEquals("abc", eval("'abc'[]").string) // 文字列そのものを返す
        assertEquals("b", eval("'abc'[1]").string) // 単一インデックスによる部分文字列の取得
        assertEquals("b", eval("'abc'['0.95']").string) // インデックスは数値化し、四捨五入される
        assertEquals("NULL", eval("'abc'[3]").string) // 範囲外のインデックスは NULL が返る
        assertEquals("c", eval("'abc'[-1]").string) // 負のインデックスは後ろから数える
        assertEquals("ccab", eval("'abc'[2, 2, 0, 1]").string) // インデックスのストリームは要素のストリームを返す

        assertEquals("bcd", eval("'abcde'[1 .. 3]").string) // 範囲指定による部分配列の取得
    }

    @Test
    fun identifier() = runTest {
        assertEquals(123, eval("abc := 123; abc").int) // 英数字

        assertEquals(123, eval("_ := 123; _").int) // アンダースコア1個
        assertEquals(123, eval("__ := 123; __").int) // アンダースコア2個

        assertEquals(123, eval("あ := 123; あ").int) // 全角文字
        assertEquals(123, eval("亜 := 123; 亜").int) // 漢字
        assertEquals(123, eval("아 := 123; 아").int) // ハングル文字
        assertEquals(123, eval("√ := 123; √").int) // 全角記号
        assertEquals(123, eval("　:=123;　").int) // 全角空白
        assertEquals(123, eval("\uD83C\uDF70 := 123; \uD83C\uDF70").int) // 絵文字 🍰
        assertEquals(123, eval("surströmming := 123; surströmming").int) // ラテン文字とマルチバイト文字の混在
    }

    @Test
    fun quotedIdentifier() = runTest {
        assertEquals(123, eval("abc := 123; `abc`").int) // 識別子とクォート識別子は同じ
        assertEquals(123, eval("`a` := 123; a").int) // 逆でもよい

        assertEquals(123, eval("{`#abc`: 123}.`#abc`").int) // 記号を含むクォート識別子

        assertEquals(123, eval("(`a` -> a)(123)").int) // ラムダ引数のクォート識別子
        assertEquals(123, eval("123 | `a` => a").int) // パイプ引数のクォート識別子
        assertEquals(123, eval("{`abc`: 123}.abc").int) // エントリーキーのクォート識別子
        assertEquals(123, eval("{abc: 123}.`abc`").int) // プロパティアクセスのクォート識別子
        assertEquals(123, eval("{abc: this -> 123}{}::`abc`()").int) // メソッドのクォート識別子
        assertEquals(123, eval("""`\u3042` := 123; あ""").int) // 文字参照
    }

    @Test
    fun mount() = runTest {
        assertEquals(123, eval("@{a: 123}; a").int) // 値のマウント
        assertEquals(123, eval("@{add: a, b -> a + b}; add(100; 23)").int) // 関数のマウント
        assertEquals(123, eval("@{a: () -> 123}; @{b: () -> a()}; b()").int) // マウントの統合
        assertEquals(123, eval("@{a: 100}; @{a: 123}; a").int) // マウントのオーバーライド
        assertEquals(123, eval("a := 123; @{a: 100}; a").int) // 変数はマウントに優先する
        assertEquals(123, eval("@{a: 123}; (@{a: 100};); a").int) // マウントはスコープに制限される
        assertEquals(123, eval("@{a: 123}; b := () -> a; @{a: 100}; b()").int) // マウントはそれより上には影響しない
        assertEquals(123, eval("@{a: 123}; m := {}; @m; m.a = 100; a").int) // マウントに使ったオブジェクトを改変しても影響しない
    }

    @Test
    fun reduce() = runTest {
        assertEquals(10, eval("1 .. 4 >> REDUCE[a, b -> a + b]").int) // ストリームの集約を行う REDUCE 関数
        assertEquals(123, eval("123 >> REDUCE[a, b -> a + b]").int) // ストリームでない場合、その値がそのまま帰ってくる
        assertEquals(123, eval("123, >> REDUCE[a, b -> a + b]").int) // 長さが1のストリームでもその値がそのまま帰ってくる
        assertEquals(FluoriteNull, eval(", >> REDUCE[a, b -> a + b]")) // 長さが0のストリームはNULLになる
    }

    @Test
    fun pushPopUnshiftShift() = runTest {
        assertEquals("[1;2;3]", eval("a := [1, 2]; a::push(3); a").array())
        assertEquals("[1;2;3;4]", eval("a := [1, 2]; a::push(3, 4); a").array())
        assertEquals("[1]", eval("a := [1, 2]; a::pop(); a").array())
        assertEquals("[3;1;2]", eval("a := [1, 2]; a::unshift(3); a").array())
        assertEquals("[3;4;1;2]", eval("a := [1, 2]; a::unshift(3, 4); a").array())
        assertEquals("[2]", eval("a := [1, 2]; a::shift(); a").array())
    }

    @Test
    fun extensionMethod() = runTest {

        // 変数による拡張関数
        """
            `::m` := (INT): this -> "V"
            100::m()
        """.let { assertEquals("V", eval(it).string) }

        // マウントによる拡張関数
        """
            @{`::m`: (INT): this -> "M"}
            100::m()
        """.let { assertEquals("M", eval(it).string) }


        // 配列にすることでオーバーロードできる
        """
            `::m` := [
                (INT): this -> "Vi"
                (STRING): this -> "Vs"
            ]
            [
                100::m()
                "abc"::m()
            ]
        """.let { assertEquals("[Vi;Vs]", eval(it).array()) }

        // マウントのオーバーロード
        """
            @{`::m`: [
                (INT): this -> "Mi"
                (STRING): this -> "Ms"
            ]}
            [
                100::m()
                "abc"::m()
            ]
        """.let { assertEquals("[Mi;Ms]", eval(it).array()) }


        // 変数による拡張関数はシャドーイングする
        """
            Obj := {m: this -> "I"}
            `::m` := (Obj): this -> "V"
            `::m` := (INT): this -> "V"
            Obj{}::m()
        """.let { assertEquals("I", eval(it).string) }

        // マウントによる拡張関数はマージされる
        """
            Obj := {}
            @{`::m`: (Obj): this -> "M"}
            @{`::m`: (INT): this -> "M"}
            Obj{}::m()
        """.let { assertEquals("M", eval(it).string) }


        // 変数による拡張関数はインスタンスメソッドを上書きする
        """
            Obj := {m: this -> "I"}
            `::m` := (Obj): this -> "V"
            Obj{}::m()
        """.let { assertEquals("V", eval(it).string) }

        // マウントによる拡張関数はインスタンスメソッドに上書きされる
        """
            Obj := {m: this -> "I"}
            @{`::m`: (Obj): this -> "M"}
            Obj{}::m()
        """.let { assertEquals("I", eval(it).string) }

        // 変数による拡張関数はマウントによる拡張関数を上書きする
        """
            Obj := {}
            @{`::m`: (Obj): this -> "M"}
            `::m` := (Obj): this -> "V"
            Obj{}::m()
        """.let { assertEquals("V", eval(it).string) }

    }

    @Test
    fun plus() = runTest {
        """
            Obj := {
                `_+_`: this, other -> this.x + other.x
            }
            Obj{x: 100} + Obj{x: 23}
        """.let { assertEquals(123, eval(it).int) }
    }

    @Test
    fun objectPlus() = runTest {
        assertEquals("{a:1;b:2}", eval("{a: 1} + {b: 2}").obj) // + でマージできる
        assertEquals("{b:2}", eval("{} + {b: 2}").obj) // 左辺が空でもよい
        assertEquals("{a:1}", eval("{a: 1} + {}").obj) // 右辺が空でもよい
        assertEquals("{}", eval("{} + {}").obj) // 両方空の場合空になる
        assertEquals("{a:5;b:2}", eval("{a: 1; b: 2} + {a: 5}").obj) // 既にある場合は右が優先される
    }

    @Test
    fun infixFunction() = runTest {
        assertEquals(5, eval("add := a, b -> a + b; 2 add 3").int) // identifierを使った中置関数呼び出し
        assertEquals(5, eval("add := a, b -> a + b; 2 `add` 3").int) // 引用符付きでもよい
        assertEquals(true, eval("lt := a, b -> a < b; 2  lt 3").boolean) // 論理値を返す中置換数
        assertEquals(false, eval("lt := a, b -> a < b; 2 !lt 3").boolean) // 否定中置換数
    }

    @Test
    fun spaceship() = runTest {
        // 数値
        assertEquals(-1, eval("1 <=> 2").int) // <=> は左辺が小さい場合は-1
        assertEquals(0, eval("1 <=> 1").int) // 等しい場合は0
        assertEquals(1, eval("2 <=> 1").int) // 左辺が大きい場合は1

        // 文字列
        assertEquals(-1, eval(" 'a' <=> 'b' ").int) // <=> は左辺が小さい場合は-1
        assertEquals(0, eval(" 'a' <=> 'a' ").int) // 等しい場合は0
        assertEquals(1, eval(" 'b' <=> 'a' ").int) // 左辺が大きい場合は1
        assertEquals(true, eval(" 'aa' > 'a' ").boolean) // 末尾に付け足した場合は大きい扱い

        // 宇宙船演算子のオーバーライド
        """
            LengthComparing := {
                `_<=>_`: this, other -> $#this.string <=> $#other.string
            }
            z := LengthComparing{string: "z"}
            a := LengthComparing{string: "a"}
            aaa := LengthComparing{string: "aaa"}
            [
                z <=> a,
                z <=> aaa,
            ]
        """.let { assertEquals("[0;-1]", eval(it).array()) }
    }

    @Test
    fun sort() = runTest {
        assertEquals("1,2,3", eval("3, 1, 2 >> SORT").stream()) // SORT でストリームをソートできる
        assertEquals("3,2,1", eval("3, 1, 2 >> SORTR").stream()) // SORTR で降順にソートする

        assertEquals("21,32,13", eval("13, 21, 32 >> SORT[a, b -> a % 10 <=> b % 10]").stream()) // 2引数の関数を指定して比較をカスタマイズできる

        assertEquals("21,32,13", eval("13, 21, 32 >> SORT[by: _ -> _ % 10]").stream()) // byでソートキーを指定できる
    }

    @Test
    fun sleep() = runTest {
        // runTestを使うとdelayが即終了するので待機時間のテストは行わない

        assertEquals(FluoriteNull, eval("SLEEP(1000)")) // SLEEP で一定時間待つ
    }

    @Test
    fun fallbackMethod() = runTest {
        val evaluator = Evaluator()
        evaluator.defineMounts(createCommonMounts())

        // _::_ でフォールバックメソッドを定義する
        """
            Obj := {
                `_::_`: this, method ->
                    method == "apple"  ? (() -> "Fallback apple") :
                    method == "banana" ? (x, y, z -> "Fallback", this, method, x, y, z, __ >> JOIN[" "]) :
                    method == "cherry" ? (() -> !!"ERROR") :
                    method == "durian" ? (() -> "Fallback durian") :
                                         NULL
                cherry: this -> "Method cherry"
            }
            @{
                `::durian`: (Obj): this -> !!"ERROR"
                `::elderberry`: (Obj): this -> "Mount elderberry"
            }
            obj := Obj{item: 123}
        """.let { evaluator.run(it) }

        assertEquals("Fallback apple", evaluator.get("obj::apple()").string) // 存在しないメソッドが呼び出された場合、フォールバックメソッドが呼ばれる
        assertEquals("Fallback {item:123} banana 1 2 3 [1;2;3]", evaluator.get("obj::banana(1; 2; 3)").string) // 引数は委譲された関数の方にバラで渡される
        assertEquals("Method cherry", evaluator.get("obj::cherry()").string) // メソッドが定義されている場合はフォールバックしない
        assertEquals("Fallback durian", evaluator.get("obj::durian()").string) // フォールバックメソッドはマウントによる拡張関数に優先する
        assertEquals("Mount elderberry", evaluator.get("obj::elderberry()").string) // フォールバックメソッドがNULLを返した場合、メソッドが定義されていない扱いになる
    }

    @Test
    fun streamMethod() = runTest {
        // ストリームの通常メソッド呼び出しは各要素のメソッド呼び出しの結合になる
        assertEquals("caa,aca,aac", eval(" 'baa', 'aba', 'aab'  | _::replace('b'; 'c')").stream())
        assertEquals("caa,aca,aac", eval("('baa', 'aba', 'aab')    ::replace('b'; 'c')").stream())
    }

    @Test
    fun generate() = runTest {
        // GENERATE で関数からストリームを生成する
        """
            [GENERATE(yield -> (
                yield << 1
                yield << 2
                yield << 3
            ))]
        """.let { assertEquals("[1;2;3]", eval(it).array()) }

        // yield関数がストリームを返した場合、その副作用は1度だけ実行される
        """
            [GENERATE(yield -> (
                1 .. 3 | yield << _
            ))]
        """.let { assertEquals("[1;2;3]", eval(it).array()) }
    }

    @Test
    fun arrowInvoke() = runTest {
        val evaluator = Evaluator()
        evaluator.defineMounts(createCommonMounts())

        // _::_ でフォールバックメソッドを定義する
        """
            register := listener -> listener(23)

            Obj := {
                register: this, listener -> listener(this.x + 3)
            }
            obj := Obj{x: 20}
        """.let { evaluator.run(it) }

        assertEquals(123, evaluator.get("register ( event => 100 + event )").int) // クロージャ付き関数呼び出し
        assertEquals(123, evaluator.get("register [ event => 100 + event ]()").int) // クロージャ付き関数の部分適用
        assertEquals(123, evaluator.get("obj::register ( event => 100 + event )").int) // クロージャ付きメソッド呼び出し
        assertEquals(123, evaluator.get("obj::register [ event => 100 + event ]()").int) // クロージャ付きメソッドの部分適用

        assertEquals(123, evaluator.get("register ( event => 9; 9; 9; 100 + event )").int) // クロージャは ; を文の区切りとして解釈する
    }

    @Test
    fun callFunction() = runTest {
        assertEquals(6, eval("CALL(a, b -> a * b; [2; 3])").int) // 関数の呼び出し
        assertEquals(123, eval("CALL(() -> 123; [])").int) // 空の引数
        assertEquals(6, eval("CALL({m: a, b -> a.v * b}{v: 2}::m; [3])").int) // メソッド参照の呼び出し
    }

    @Test
    fun firstLastTest() = runTest {
        // FIRST
        assertEquals(4, eval("FIRST(4, 5, 6)").int)
        assertEquals(4, eval("FIRST(4)").int)
        assertEquals(FluoriteNull, eval("FIRST(,)"))

        // LAST
        assertEquals(6, eval("LAST(4, 5, 6)").int)
        assertEquals(6, eval("LAST(6)").int)
        assertEquals(FluoriteNull, eval("LAST(,)"))
    }

    @Test
    fun group() = runTest {
        assertEquals("[1;[14]],[2;[25]]", eval("14, 25 >> GROUP[by: _ -> _.&.0]").stream()) // GROUPでグループのストリームになる
        assertEquals("[1;[14]]", eval("14 >> GROUP[by: _ -> _.&.0]").stream()) // 要素が1個でもよい
        assertEquals("", eval(", >> GROUP[by: _ -> _.&.0]").stream()) // 要素が0個でもよい
        assertEquals("[1;[14;15]]", eval("14, 15 >> GROUP[by: _ -> _.&.0]").stream()) // すべてが同じグループになってもよい
        assertEquals("[1;[14]],[2;[25]],[3;[36]]", eval("14, 25, 36 >> GROUP[by: _ -> _.&.0]").stream()) // 3要素でもよい
        assertEquals("[1;[14;15]],[3;[36]]", eval("14, 15, 36 >> GROUP[by: _ -> _.&.0]").stream()) // 部分的にグループ化されてもよい
    }

    @Test
    fun shuffleTest() = runTest {
        assertEquals("1,2,3", eval("1, 2, 3 >> SHUFFLE >> SORT").stream()) // SHUFFLEでシャッフルする
        assertEquals("1", eval("1, >> SHUFFLE").stream()) // 1要素のストリームはその要素だけのストリームを返す
        assertEquals(1, eval("1 >> SHUFFLE").int) // 非ストリームはその要素を返す
        assertEquals("", eval(", >> SHUFFLE").stream()) // 空ストリームは空ストリームを返す
    }

    @Test
    fun setCallTest() = runTest {

        // 代入呼び出しができる
        """
            value := NULL
            function := new -> value = new
            function() = 123
            value
        """.let { assertEquals(123, eval(it).int) }

        // 代入値は引数列の最後に受け取る
        """
            value := NULL
            function := a, new -> value = a + new
            function(100) = 23
            value
        """.let { assertEquals(123, eval(it).int) }

        // メソッドのオーバーライドが可能
        """
            value := NULL
            function := {
                `_()=_`: this, new -> value = new
            }{}
            function() = 123
            value
        """.let { assertEquals(123, eval(it).int) }

    }

    @Test
    fun returnTest() = runTest {

        // label !! value でreturnできる
        """
            (
                label !! 123
                456
            ) !: label
        """.let { assertEquals(123, eval(it).int) }

        // !! の結合優先度は左から見るとリテラル系と同等
        // なので前置単項すらそのままつけれる
        // 右から見ると , 以下 !: 以上
        assertEquals("1,2,3", eval("$# label !! 1, 2, 3 !: label").stream())

        // 同名のラベルが複数存在した場合、最も内側のラベルを出る
        """
            (
                (
                    label !! 1
                    2
                ) !: label
                3
            ) !: label
        """.let { assertEquals(3, eval(it).int) }

        // !: はラムダ右辺よりも結合優先度が高い
        """
            prime_only := x -> (
                x == 1 && fail !! "!1"
                (x != 2 && x %% 2) && fail !! "!2n"
                (x != 3 && x %% 3) && fail !! "!3n"
                (x != 5 && x %% 5) && fail !! "!5n"
                x
            ) !: fail
            1 .. 10 | prime_only(_)
        """.let { assertEquals("!1,2,3,!2n,5,!2n,7,!2n,!3n,!2n", eval(it).stream()) }

        // ラムダの中から外に !! 出来る
        """
            run := block -> block()
            run ( =>
                return !! 123
                456
            ) !: return
        """.let { assertEquals(123, eval(it).int) }

        // ラムダの中から外に !! 出来る
        """
            (
                1 .. 50 | (
                    _ % 2 != 0 && next !! NULL // 2で割り切れない！
                    _ % 3 != 0 && next !! NULL // 3で割り切れない！
                    _ % 5 != 0 && next !! NULL // 5で割り切れない！
                    found !! _                 // 2でも3でも5でも割り切れる
                ) !: next
                NULL
            ) !: found
        """.let { assertEquals(30, eval(it).int) }

    }

    @Test
    fun objectSetCallTest() = runTest {
        // オブジェクトの代入呼び出しでキーが作られる
        """
            obj := {}
            obj("a") = 123
            obj.a
        """.let { assertEquals(123, eval(it).int) }

        // 既存キーを上書きできる
        """
            obj := {a: 789}
            obj("a") = 456
            obj("a") = 123
            obj.a
        """.let { assertEquals(123, eval(it).int) }
    }
}
