package mirrg.fluorite12

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mirrg.fluorite12.operations.FluoriteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class Fluorite12Test {

    @Test
    fun cacheTest() = runTest {
        // メモ化が行われているか
        assertEquals("a", run(""" "$( "$( "$( "$( "$( "$( "$( "$( "$( "$( "a" )" )" )" )" )" )" )" )" )" )" """).string)
        assertEquals("a", run(""" %><%= %><%= %><%= %><%= %><%= %><%= %>a<% %><% %><% %><% %><% %><% %><% """).string)
        assertEquals(1, run("(((((((((1)))))))))").int)
        assertEquals("[[[[[[[[[1]]]]]]]]]", run("&[[[[[[[[[1]]]]]]]]]").string)
        assertEquals("{a:{a:{a:{a:{a:{a:{a:{a:1}}}}}}}}", run("&{a:{a:{a:{a:{a:{a:{a:{a:1}}}}}}}}").string)
        assertEquals(1, run("a := x -> x; a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(a(1))))))))))))))))").int)
        assertEquals(1, run("1?1?1?1?1?1?1?1?1?1?1?1?1?1?1?1?1:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0").int)
        assertEquals(1, run("0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:0?0:1").int)
        assertEquals(1, run("NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:NULL?:1").int)
        assertEquals("[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]", run("&[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1]").string)
        assertEquals("[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]", run("&[0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;1]").string)
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
        """.let { assertEquals("[1;2;3]", run(it).array()) }

        // 行コメントの次の行に中置演算子としても解釈可能な前置演算子があったとしても、その行を結合しない
        """
            [
                1
                2 # comment
                -3
            ]
        """.let { assertEquals("[1;2;-3]", run(it).array()) }

        assertEquals(1, run("1 # comment").int) // 行コメントの後に改行が無くてもよい

    }

    @Test
    fun builtInConstantTest() = runTest {
        assertEquals(FluoriteNull, run("NULL"))
        assertEquals(true, run("TRUE").boolean)
        assertEquals(false, run("FALSE").boolean)
    }

    @Test
    fun numberTest() = runTest {
        assertEquals(1, run("1").int) // 整数を記述できる
        assertEquals(0, run("0").int) // 0も普通に書ける
        assertEquals(100, run("00100").int) // 整数は先頭に余計な 0 があっても10進数として扱われる

        assertEquals(0x10, run("H#10").int) // H# で16進数を記述できる
        assertEquals(-0x10, run("-H#10").int) // 負の16進数
        assertEquals(0xabcdef, run("H#abcdef").int) // 英字の16進数
        assertEquals(0xABCDEF, run("H#ABCDEF").int) // 大文字の英字の16進数

        assertEquals(1.1, run("1.1").double, 0.001) // 小数を記述できる
        assertEquals(0.0, run("0.0").double) // 0.0も普通に書ける
        assertEquals(1.0, run("1.0").double) // .0 を付けると浮動小数点数で整数値を得る
        assertEquals(100.0, run("00100.00").double) // 小数も先頭と末尾に余計な 0 があっても10進数として扱われる

        assertEquals(-10, run("-10").int) // 負の整数が書ける
        assertEquals(-1.1, run("-1.1").double, 0.001) // 負の小数が書ける
    }

    @Test
    fun rawStringTest() = runTest {
        assertEquals("abcABC123", run(" 'abcABC123' ").string) // ' で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%& ()*+,-./""", run(""" ' !"#$%& ()*+,-./' """).string) // ' はエスケープが必要
        assertEquals("""0123456789:;<=>?""", run(""" '0123456789:;<=>?' """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", run(""" '@ABCDEFGHIJKLMNO' """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", run(""" 'PQRSTUVWXYZ[\]^_' """).string) // \ すらもエスケープ不要
        assertEquals("""`abcdefghijklmno""", run(""" '`abcdefghijklmno' """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, run(""" 'pqrstuvwxyz{|}~ ' """).string)

        assertEquals("あ", run(" 'あ' ").string) // マルチバイト文字
        assertEquals("㎡", run(" '㎡' ").string) // MS932
        assertEquals("🍰", run(" '🍰' ").string) // サロゲートペア

        assertEquals(" ' ", run(" ' '' ' ").string) // '' が ' になる

        assertEquals("\n \n \n", run(" '\n \r \r\n' ").string) // 改行は \n に統一される
    }

    @Test
    fun templateStringTest() = runTest {
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
        assertEquals("10", run(""" "$(2 * 5)" """).string) // 式の埋め込み
        assertEquals(" abc ", run(""" " $( "abc" ) " """).string) // 入れ子状の埋め込み

        assertEquals("\n \n \n", run(""" "${"\n \r \r\n"}" """).string) // 改行は \n に統一される
    }

    @Test
    fun formatTest() = runTest {
        // %-+ 09d  空白埋め  0埋め  左揃え  符号表示  符号余白  変換

        // 整数
        run {
            val s = """123456, 12345, 123, 0, -123, -1234, -12345, -123456"""
            assertEquals("[123456;12345;123;0;-123;-1234;-12345;-123456]", run(""" [$s | "$%d(_)"] """).array()) // %d で整数
            assertEquals("[123456;12345;  123;    0; -123;-1234;-12345;-123456]", run(""" [$s | "$%5d(_)"] """).array()) // 空白埋め
            assertEquals("[123456;12345;00123;00000;-0123;-1234;-12345;-123456]", run(""" [$s | "$%05d(_)"] """).array()) // 0埋め
            assertEquals("[123456;12345;123  ;0    ;-123 ;-1234;-12345;-123456]", run(""" [$s | "$%-5d(_)"] """).array()) // 左揃え空白埋め
            assertEquals("[+123456;+12345;+123;+0;-123;-1234;-12345;-123456]", run(""" [$s | "$%+d(_)"] """).array()) // 符号表示
            assertEquals("[ 123456; 12345; 123; 0;-123;-1234;-12345;-123456]", run(""" [$s | "$% d(_)"] """).array()) // 符号余白
            assertEquals("[+123456;+12345; +123;   +0; -123;-1234;-12345;-123456]", run(""" [$s | "$%+5d(_)"] """).array()) // 符号表示 空白埋め
            assertEquals("[ 123456; 12345;  123;    0; -123;-1234;-12345;-123456]", run(""" [$s | "$% 5d(_)"] """).array()) // 符号余白 空白埋め
            assertEquals("[+123456;+12345;+0123;+0000;-0123;-1234;-12345;-123456]", run(""" [$s | "$%+05d(_)"] """).array()) // 符号表示 0埋め
            assertEquals("[ 123456; 12345; 0123; 0000;-0123;-1234;-12345;-123456]", run(""" [$s | "$% 05d(_)"] """).array()) // 符号余白 0埋め
        }

        // 16進数
        run {
            val s = """H#abcdef, H#abcde, H#abc, H#0, -H#abc, -H#abcd, -H#abcde, -H#abcdef"""
            assertEquals("[abcdef;abcde;abc;0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%x(_)"] """).array()) // %x で16進数
            assertEquals("[abcdef;abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%5x(_)"] """).array()) // 空白埋め
            assertEquals("[abcdef;abcde;00abc;00000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%05x(_)"] """).array()) // 0埋め
            assertEquals("[abcdef;abcde;abc  ;0    ;-abc ;-abcd;-abcde;-abcdef]", run(""" [$s | "$%-5x(_)"] """).array()) // 左揃え空白埋め
            assertEquals("[+abcdef;+abcde;+abc;+0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+x(_)"] """).array()) // 符号表示
            assertEquals("[ abcdef; abcde; abc; 0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% x(_)"] """).array()) // 符号余白
            assertEquals("[+abcdef;+abcde; +abc;   +0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+5x(_)"] """).array()) // 符号表示 空白埋め
            assertEquals("[ abcdef; abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% 5x(_)"] """).array()) // 符号余白 空白埋め
            assertEquals("[+abcdef;+abcde;+0abc;+0000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+05x(_)"] """).array()) // 符号表示 0埋め
            assertEquals("[ abcdef; abcde; 0abc; 0000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% 05x(_)"] """).array()) // 符号余白 0埋め
        }

        // 小数
        run {
            assertEquals("1.5", run(""" "$%f(1.5)" """).string) // 小数の埋め込み
            assertEquals("-1.5", run(""" "$%f(-1.5)" """).string) // 負数
            assertEquals("0", run(""" "$%f(0.0)" """).string) // 0
            assertEquals("1", run(""" "$%f(1.0)" """).string) // 整数値は小数点以降が省略される
            assertEquals("0.5", run(""" "$%f(0.5)" """).string) // 小数点の前は省略されない

            assertEquals("1.111", run(""" "$%.3f(1.111222)" """).string) // 小数の切り詰め
            assertEquals("1.111", run(""" "$%.3f(1.111777)" """).string) // 四捨五入はしない
            assertEquals("-1.111", run(""" "$%.3f(-1.111777)" """).string) // 常に絶対値が小さい方に丸められる
            assertEquals("1.500", run(""" "$%.3f(1.5)" """).string) // 小数の埋め合わせ
            assertEquals("1.111", run(""" "$%.3f(1.111)" """).string) // 精度が丁度

            // 小数点以下0桁の場合、小数点も消える
            assertEquals("1", run(""" "$%.0f(1.5)" """).string)
            assertEquals("1", run(""" "$%.0f(1.0)" """).string)

            assertEquals("  1.5", run(""" "$%5f(1.5)" """).string) // 空白埋め指定は全体の文字数に作用する
            assertEquals(" -1.5", run(""" "$%5f(-1.5)" """).string) // 負の空白埋め
            assertEquals("12345.5", run(""" "$%5f(12345.5)" """).string) // 空白埋めは文字数を切り詰めない
            assertEquals("1.5  ", run(""" "$%-5f(1.5)" """).string) // 左詰め
            assertEquals("001.5", run(""" "$%05f(1.5)" """).string) // 0埋め
            assertEquals("+01.5", run(""" "$%+05f(1.5)" """).string) // +を表示
            assertEquals(" 01.5", run(""" "$% 05f(1.5)" """).string) // 符号用余白
            assertEquals("1.000", run(""" "$%.3f(1.0)" """).string) // もともと小数点が含まれず、精度が1以上

            assertEquals("-01.5", run(""" "$%05f(-1.5)" """).string) // 負の0埋めは符号を先に書く
            assertEquals(" 01.5", run(""" "$% 05f(1.5)" """).string) // 0埋めでも符号用の余白は空白を書く

            // 小数点なし左詰め0埋めは数学的に矛盾した挙動を示す
            assertEquals("10000", run(""" "$%-05.0f(1.0)" """).string)
            assertEquals("10000", run(""" "$%-05.0f(1.5)" """).string)

            assertEquals("  1.123", run(""" "$%7.3f(1.123456)" """).string) // 空白埋めかつ精度指定
        }

        // 文字列
        run {
            val s = """ "", "abcd", "abcde", "abcdef" """
            assertEquals("[;abcd;abcde;abcdef]", run(""" [$s | "$%s(_)"] """).array()) // %s で文字列
            assertEquals("[     ; abcd;abcde;abcdef]", run(""" [$s | "$%5s(_)"] """).array()) // 空白埋め
            assertEquals("[     ;abcd ;abcde;abcdef]", run(""" [$s | "$%-5s(_)"] """).array()) // 左揃え空白埋め
        }
    }

    @Test
    fun embeddedStringTest() = runTest {
        assertEquals("abcABC123", run(" %>abcABC123<% ").string) // %> <% で囲うと文字列になる

        // ASCII文字のテスト
        assertEquals(""" !"#$%&'()*+,-./""", run(""" %> !"#$%&'()*+,-./<% """).string) // すべての文字はエスケープ不要
        assertEquals("""0123456789:;<=>?""", run(""" %>0123456789:;<=>?<% """).string)
        assertEquals("""@ABCDEFGHIJKLMNO""", run(""" %>@ABCDEFGHIJKLMNO<% """).string)
        assertEquals("""PQRSTUVWXYZ[\]^_""", run(""" %>PQRSTUVWXYZ[\]^_<% """).string) // \ もエスケープ不要
        assertEquals("""`abcdefghijklmno""", run(""" %>`abcdefghijklmno<% """).string)
        assertEquals("""pqrstuvwxyz{|}~ """, run(""" %>pqrstuvwxyz{|}~ <% """).string)

        assertEquals("あ", run(" %>あ<% ").string) // マルチバイト文字
        assertEquals("㎡", run(" %>㎡<% ").string) // MS932
        assertEquals("🍰", run(" %>🍰<% ").string) // サロゲートペア

        assertEquals(" <% ", run(" %> <%% <%").string) // <%% で <% になる

        assertEquals(" 10 ", run(" %> <%= 1 < 2 ? 10 : 100 %> <% ").string) // 式の埋め込み
        assertEquals(" abc ", run(" %> <%= %>abc<% %> <% ").string) // 入れ子状の埋め込み

        assertEquals("_30_10_10", run(" a := 10; b := %>_<%= a := 20; a = 30; a %>_<%= a %>_<%; b & a ").string) // スコープを作る

        assertEquals("\n \n \n", run(" %>\n \r \r\n<% ").string) // 改行は \n に統一される
    }

    @Test
    fun bracketsTest() = runTest {
        assertEquals(1, run("(1)").int) // ( ) で囲うと中身をそのまま得られる
        assertEquals(FluoriteNull, run("()")) // () でNULLになる
        assertEquals("", run("(,)").stream()) // (,) で空ストリームになる
    }

    @Test
    fun objectTest() = runTest {
        assertEquals("{a:1}", run(""" {"a": 1} """).obj) // { } でオブジェクトを作れる
        assertEquals("{a:1}", run("{a: 1}").obj) // キーの " は省略できる
        assertEquals("{1:2}", run("{1: 2}").obj) // キーは数値でもよい
        assertEquals("{1:2}", run("1 | a => {(a): 2}").obj) // キーに ( ) を付けると変数を参照できる
        assertEquals("{a:1;b:2}", run("{a: 1; b: 2}").obj) // エントリーは ; で区切ることができる
        assertEquals("{a:1;b:2}", run("{; ; a: 1; ; b: 2; ; }").obj) // ; は無駄に大量にあってもよい
        assertEquals("{}", run("{; }").obj) // ; しかなくてもよい
        assertEquals("{a:1;b:2}", run("{(a: 1), (b: 2)}").obj) // エントリーのストリームでもよい
        assertEquals("{a:1;b:2;c:3}", run("{(a: 1), (b: 2); c: 3}").obj) // エントリーのストリームとエントリーが混在してもよい
        assertEquals("{1:2;2:4;3:6}", run("{1 .. 3 | a => (a): a * 2}").obj) // エントリー列を返す式でもよい
        assertEquals("{}", run(""" {} """).obj) // 空でもよい

        assertEquals(true, run(""" A := {}; a := A {}; a ?= A """).boolean) // 親クラスを取るオブジェクト
    }

    @Test
    fun toNumberTest() = runTest {
        // + で数値になる

        assertEquals(0, run("+NULL").int) // NULLは0

        // 文字列の数値化
        assertEquals(123, run("+'123'").int)
        assertEquals(123.456, run("+'123.456'").double, 0.001)

        assertEquals(1, run("+TRUE").int) // TRUEは1
        assertEquals(0, run("+FALSE").int) // FALSEは0

        assertEquals(1, run("+1").int) // 整数はそのまま
        assertEquals(1.0, run("+1.0").double, 0.001) // 小数もそのまま

        assertEquals(55, run("+(1 .. 10)").int) // ストリームは各要素の合計


        assertEquals(123, run("+{TO_NUMBER: this -> 123}{}").int) // オブジェクトの数値化はTO_NUMBERメソッドでオーバーライドできる
    }

    @Test
    fun toBooleanTest() = runTest {
        // ? で論理値になる

        assertEquals(false, run("?NULL").boolean) // NULLはFALSE

        assertEquals(false, run("?FALSE").boolean) // FALSEはFALSE

        assertEquals(true, run("?1").boolean) // 0以外であればTRUE
        assertEquals(false, run("?0").boolean) // 0はFALSE
        assertEquals(true, run("?-1").boolean) // 負の数もTRUE

        assertEquals(true, run("?1.0").boolean) // 0.0以外であればTRUE
        assertEquals(false, run("?0.0").boolean) // 0.0はFALSE
        assertEquals(true, run("?-1.0").boolean) // 負の数もTRUE

        assertEquals(true, run("?TRUE").boolean) // TRUEはTRUE
        assertEquals(false, run("?FALSE").boolean) // FALSEはFALSE

        assertEquals(true, run("?'0'").boolean) // '' 以外であればTRUE
        assertEquals(false, run("?''").boolean) // '' はFALSE
        assertEquals(true, run("?'FALSE'").boolean) // 'FALSE' もTRUE
        assertEquals(true, run("?'false'").boolean) // 'false' もTRUE

        assertEquals(false, run("?(FALSE, FALSE, FALSE)").boolean) // ストリームは各要素のORを取る
        assertEquals(true, run("?(FALSE, TRUE, FALSE)").boolean) // 1個でもTRUEがあればTRUE
        assertEquals(false, run("?(,)").boolean) // 空ストリームはFALSE


        assertEquals(false, run("!TRUE").boolean) // TRUEはFALSE
        assertEquals(true, run("!FALSE").boolean) // FALSEはTRUE
        assertEquals(true, run("!0").boolean) // ! も論理値に自動変換される


        assertEquals(true, run("?[]").boolean) // 配列の論理値化は常にTRUE
        assertEquals(true, run("?{}").boolean) // オブジェクトの論理値化は常にTRUE
        assertEquals(false, run("?{TO_BOOLEAN: this -> FALSE}{}").boolean) // ただしTO_BOOLEANメソッドでオーバーライドできる
        assertEquals(true, run("TRUE::TO_BOOLEAN()").boolean) // 論理値に対してもTO_BOOLEANが呼び出せる
    }

    @Test
    fun rightTest() {
        assertEquals("ToNumber[Literal[100]]", parse("100.+"))

        assertEquals(parse("+100"), parse("100.+"))
        assertEquals(parse("-100"), parse("100.-"))
        assertEquals(parse("?100"), parse("100.?"))
        assertEquals(parse("!!100"), parse("100.!!"))
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
        assertEquals("NULL", run("&NULL").string)
        assertEquals("10", run("&10").string)
        assertEquals("TRUE", run("&TRUE").string)
        assertEquals("FALSE", run("&FALSE").string)
        assertEquals("abc", run("&'abc'").string)
        assertEquals("[1;2;3]", run("&[1, 2, 3]").string)
        assertEquals("{a:1;b:2}", run("&{a: 1; b: 2}").string)

        assertEquals("10", run("&{TO_STRING: this -> &this.a}{a: 10}").string) // 文字列化のオーバーライド

        assertEquals("[1;2;3]", run("&[1;2;3]").string) // 配列の文字列化
        assertEquals("{a:1;b:2}", run("&{a:1;b:2}").string) // オブジェクトの文字列化
    }

    @Test
    fun jsonTest() = runTest {
        // $& でFluoriteValueがjson文字列になる
        assertEquals("10", run("$&10").string) // トップレベルがJsonArrayやJsonObjectでなくてもよい
        assertEquals("10.5", run("$&10.5").string)
        assertEquals("\"abc\"", run("$&'abc'").string)
        assertEquals(""""a\"b\nc\\d"""", run(""" $&"a\"b\nc\\d" """).string)
        assertEquals("true", run("$&TRUE").string)
        assertEquals("false", run("$&FALSE").string)
        assertEquals("null", run("$&NULL").string)
        assertEquals("[1,2,3]", run("$&[1; 2; 3]").string)
        assertEquals("{\"a\":1,\"b\":2}", run("$&{a: 1; b: 2}").string)

        // $* でjson文字列がFluoriteValueになる
        assertEquals(10, run("$*'10'").int)
        assertEquals(10.5, run("$*'10.5'").double, 0.001)
        assertEquals("abc", run("$*'\"abc\"'").string)
        assertEquals("a\"b\nc\\d", run(""" $*'"a\"b\nc\\d"' """).string)
        assertEquals(true, run("$*'true'").boolean)
        assertEquals(false, run("$*'false'").boolean)
        assertEquals(FluoriteNull, run("$*'null'"))
        assertEquals("[1;2;3]", run("&$*'[1,2,3]'").string)
        assertEquals("{a:1;b:2}", run("&$*'{\"a\":1,\"b\":2}'").string)
    }

    @Test
    fun exceptionTest() = runTest {
        // !! でオブジェクトをスローするとFluoriteExceptionになって出てくる
        try {
            run("!!'a'")
            fail()
        } catch (e: FluoriteException) {
            assertEquals("a", e.value.string)
        }

        assertEquals("b", run("!!'a' !? 'b'").string) // !? で例外をキャッチできる
        assertEquals("b", run("1 + [2 + !!'a'] !? 'b'").string) // !! は深い階層にあってもよい
        assertEquals("a", run("!!'a' !? _").string) // _ で例外オブジェクトを受け取れる
        assertEquals("a", run("!!'a' !? e => e").string) // => で例外オブジェクトを受け取る変数を指定できる
        assertEquals("a", run("t := () -> !!'a'; c := f -> f() !? e => () -> e; c(t)()").string) // ラムダ演算子と同じ結合優先度
        assertEquals(1, run("a := 1; 1 !? a = 2; a").int) // !? の右辺は実行されなければ副作用が出ない
    }

    @Test
    fun accessTest() = runTest {
        assertEquals("b", run(" 'abc'.1 ").string) // 文字列に数値アクセスするとそのインデックスの文字を得る
        assertEquals(FluoriteNull, run(" 'abc'.(-1) ")) // 負のインデックスは無効
        assertEquals(FluoriteNull, run(" 'abc'.3 ")) // 文字列の範囲外にアクセスすると NULL が返る
        assertEquals("c", run(" 'abc'.(1 + 1) ").string) // キーを ( ) で囲むと式で参照できる

        assertEquals(20, run(" [10, 20, 30].1 ").int) // 配列に数値アクセスするとそのインデックスの要素を得る
        assertEquals(FluoriteNull, run(" [10, 20, 30].(-1) ")) // 負のインデックスは無効
        assertEquals(FluoriteNull, run(" [10, 20, 30].3 ")) // 配列の範囲外にアクセスすると NULL が返る
        assertEquals(30, run(" [10, 20, 30].(1 + 1) ").int) // キーを ( ) で囲むと式で参照できる

        assertEquals(10, run(" {a: 10; b: 20}.a ").int) // オブジェクトに識別子アクセスするとその要素を得る
        assertEquals(10, run(" {a: 10; b: 20}.'a' ").int) // キーは文字列リテラルでもよい
        assertEquals(FluoriteNull, run(" {a: 10; b: 20}.c ")) // 存在しない要素にアクセスすると NULL が返る
        assertEquals(20, run(" 'b' | a => {a: 10; b: 20}.(a) ").int) // キーを ( ) で囲むと式で参照できる
    }

    @Test
    fun bracketsAccessTest() = runTest {
        assertEquals("[a;1],[b;2],[c;3]", run("{a: 1; b: 2; c: 3}()").stream()) // object() でエントリーのストリームにする
        assertEquals(2, run("{a: 1; b: 2; c: 3}('b')").int) // object(key) で要素を得る
        assertEquals("3,3,1,2", run("{a: 1; b: 2; c: 3}('c', 'c', 'a', 'b')").stream()) // object(keys) で要素のストリームを得る

        assertEquals("a,b,c", run("'abc'()").stream()) // string() で文字のストリームにする
        assertEquals("b", run("'abc'(1)").string) // string(index) で文字を得る
        assertEquals("c", run("'abc'(-1)").string) // 負のインデックスは後ろから数える
        assertEquals("c,c,a,b", run("'abc'(2, 2, 0, 1)").stream()) // string(indices) で文字のストリームを得る
    }

    @Test
    fun invokeTest() = runTest {
        assertEquals(123, run("(a -> a + 23)(100)").int) // function() で関数を呼び出せる
        assertEquals(123, run("(a -> a + 23)::INVOKE(100)").int) // INVOKEメソッドでも関数を呼び出せる
        assertEquals(123, run("{INVOKE: this, a, b -> a + b + 3}{}(100; 20)").int) // INVOKEメソッドを定義したオブジェクトも関数として呼び出せる
        assertEquals(123, run("{INVOKE: this, a, b -> a + b + 3}{}[100](20)").int) // INVOKEメソッドを定義したオブジェクトも部分適用できる
        assertEquals(123, run("{INVOKE: {INVOKE: this2, this1, a, b -> a + b}{}}{}(100; 23)").int) // INVOKEの多重追跡
    }

    @Test
    fun powTest() = runTest {
        assertEquals(16.0, run("4 ^ 2").double, 0.00001) // ^ でべき乗ができる、べき乗すると常に浮動小数点数になる

        // ^ は右優先結合
        assertEquals("Power[Literal[1];Power[Literal[2];Literal[3]]]", parse("1 ^ 2 ^ 3"))
        assertEquals(256.0, run("2 ^ 2 ^ 3").double, 0.00001)
        assertEquals(64.0, run("(2 ^ 2) ^ 3").double, 0.00001)

        // ^ は乗算よりも優先される
        assertEquals("Times[Times[Literal[1];Power[Literal[2];Literal[3]]];Literal[4]]", parse("1 * 2 ^ 3 * 4"))
        assertEquals(280.0, run("5 * 2 ^ 3 * 7").double, 0.00001)

        // ^ は前置演算子よりも優先される
        assertEquals("ToNegativeNumber[Power[Literal[1];Literal[2]]]", parse("- 1 ^ 2"))
        assertEquals(-16.0, run("- 4 ^ 2").double, 0.00001)

        // ^ の右に前置演算子があってもよい
        assertEquals(0.0625, run("4 ^ - 2").double, 0.00001)

        assertEquals(432.0, run("- 2 * - 2 ^ - - 3 * - 3 ^ - - 2 * - 3").double, 0.00001) // 複合的なテスト
    }

    @Test
    fun timesTest() = runTest {
        assertEquals(6, run("2 * 3").int) // * で乗算ができる

        // どちらかが浮動小数点数なら結果も浮動小数点数になる
        assertEquals(6.0, run("2.0 * 3").double, 0.001)
        assertEquals(6.0, run("2 * 3.0").double, 0.001)
        assertEquals(6.0, run("2.0 * 3.0").double, 0.001)

        assertEquals("abcabcabc", run("'abc' * 3").string) // 文字列の乗算は繰り返す
        assertEquals("[1;2;3;1;2;3;1;2;3]", run("[1; 2; 3] * 3").array()) // 配列の乗算は繰り返す
    }

    @Test
    fun modTest() = runTest {
        assertEquals(1, run("10 % 3").int) // % で余りを得る
        assertEquals(0.25, run("1.75 % 0.5").double) // 浮動小数点数でもよい
        assertEquals(0.5, run("2 % 0.75").double) // 右側だけが浮動小数点数でもよい
        assertEquals(0.25, run("10.25 % 5").double) // 左側だけが浮動小数点数でもよい

        // 負の余りは正になるまで割る数を足したものの余りと同じ（-1 + 3 = 2） % 3
        // そのため同じ余りがループする
        assertEquals("[0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0]", run("&[-10 .. 10 | _ % 5]").string)

        assertEquals(false, run("10 %% 3").boolean) // %% は割り切れる場合にTRUE
        assertEquals(true, run("10 %% 2").boolean) // %% は割り切れない場合にFALSE
        assertEquals(true, run("-3 %% 3").boolean) // %% も負に対応
        assertEquals(true, run("10.0 %% 2.0").boolean) // 浮動小数点数として表現されていてもよい
        assertEquals(true, run("1.75 %% 0.25").boolean) // 2進数で割り切れるのであれば小数でもよい
        assertEquals(true, run("25 %% 0.25").boolean) // 右側だけが浮動小数点数でもよい
        assertEquals(true, run("25.0 %% 5").boolean) // 左側だけが浮動小数点数でもよい
    }

    @Test
    fun stringConcatenateTest() = runTest {
        assertEquals("ab", run(" 'a' & 'b' ").string) // & で文字列の連結ができる
        assertEquals("12", run(" 1 & 2 ").string) // 文字列に変換する
    }

    @Test
    fun rangeTest() = runTest {
        assertEquals("1,2,3,4", run("1 .. 4").stream()) // .. でその範囲をイテレートするストリームを得る
        assertEquals("0,1,2,3", run("0 .. 4 - 1").stream()) // 項は0や四則演算等でもよい
        assertEquals("1", run("1 .. 1").stream()) // 範囲が1つの要素の場合はその要素のみのストリームを返す
        assertEquals("-1,0,1", run("-1 .. 1").stream()) // 項は前置演算子がついていたり、負の値でもよいし、正負をまたいでもよい
        assertEquals("[1;2;3;4],[1;2;3;4]", run("a := 1 .. 4; [a], [a]").stream()) // 範囲ストリームは再利用できる
        assertEquals("4,3,2,1", run("4 .. 1").stream()) // 下降も可能

        assertEquals("1,2,3", run("1 ~ 4").stream()) // 半開区間演算子は終端を含まない
        assertEquals("", run("1 ~ 1").stream()) // 範囲が一つの場合は空ストリームになる
        assertEquals("[1;2;3],[1;2;3]", run("a := 1 ~ 4; [a], [a]").stream()) // 再利用のテスト
        assertEquals("", run("4 ~ 1").stream()) // 右辺が左辺より小さい場合は空ストリームになる
    }

    @Test
    fun compareTest() = runTest {
        fun String.f() = this.replace(" ", "")

        // 比較ができる
        assertEquals("[FALSE;FALSE;TRUE ]".f(), run("[0 >  1; 1 >  1; 2 >  1]").array())
        assertEquals("[TRUE ;FALSE;FALSE]".f(), run("[0 <  1; 1 <  1; 2 <  1]").array())
        assertEquals("[FALSE;TRUE ;TRUE ]".f(), run("[0 >= 1; 1 >= 1; 2 >= 1]").array())
        assertEquals("[TRUE ;TRUE ;FALSE]".f(), run("[0 <= 1; 1 <= 1; 2 <= 1]").array())

        // 浮動小数でもよい
        assertEquals("[FALSE;FALSE;FALSE]".f(), run("[1 >  1.0; 1.0 >  1; 1.0 >  1.0]").array())
        assertEquals("[FALSE;FALSE;FALSE]".f(), run("[1 <  1.0; 1.0 <  1; 1.0 <  1.0]").array())
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), run("[1 >= 1.0; 1.0 >= 1; 1.0 >= 1.0]").array())
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), run("[1 <= 1.0; 1.0 <= 1; 1.0 <= 1.0]").array())
    }

    @Test
    fun containsTest() = runTest {
        // string @ string で部分一致
        assertEquals(true, run("'abc' @ '---abc---'").boolean)
        assertEquals(false, run("'123' @ '---abc---'").boolean)

        // value @ array で要素が含まれているかどうか
        assertEquals(true, run("30 @ [10, 20, 30]").boolean)
        assertEquals(false, run("40 @ [10, 20, 30]").boolean)

        // key @ object で要素が含まれているかどうか
        assertEquals(true, run("'a' @ {a: 10; b: 20}").boolean)
        assertEquals(false, run("'c' @ {a: 10; b: 20}").boolean)

        // CONTAINSメソッドで上書きできる
        assertEquals(true, run("'abc' @ {CONTAINS: this, value -> value @ '---abc---'}{}").boolean)
        assertEquals(false, run("'123' @ {CONTAINS: this, value -> value @ '---abc---'}{}").boolean)
    }

    @Test
    fun andOrTest() = runTest {
        // && は左辺がTRUEの場合に右辺を、 || は左辺がFALSEの場合に右辺を返す
        assertEquals(0, run("0 && 2").int)
        assertEquals(2, run("1 && 2").int)
        assertEquals(2, run("0 || 2").int)
        assertEquals(1, run("1 || 2").int)

        // 評価されない右辺は副作用も発生させない
        assertEquals(1, run("a := 1; 1 || (a = 2); a").int)
        assertEquals(1, run("a := 1; 0 && (a = 2); a").int)

        // 結合優先度のテスト
        assertEquals(true, run("1 < 2 && 1 < 2").boolean)
        assertEquals(false, run("1 < 2 && 2 < 1").boolean)
        assertEquals(false, run("2 < 1 && 1 < 2").boolean)
        assertEquals(false, run("2 < 1 && 2 < 1").boolean)
        assertEquals(true, run("1 < 2 || 1 < 2").boolean)
        assertEquals(true, run("1 < 2 || 2 < 1").boolean)
        assertEquals(true, run("2 < 1 || 1 < 2").boolean)
        assertEquals(false, run("2 < 1 || 2 < 1").boolean)
        assertEquals(1, run("0 && 0 || 1").int)
        assertEquals(0, run("0 && (0 || 1)").int)
        assertEquals(1, run("1 || 0 && 0").int)
        assertEquals(0, run("(1 || 0) && 0").int)
    }

    @Test
    fun conditionTest() = runTest {
        // ? : で条件分岐ができる
        assertEquals(1, run("TRUE ? 1 : 2").int)
        assertEquals(2, run("FALSE ? 1 : 2").int)

        // ? : を入れ子にすると右側が優先的にくっつく
        assertEquals(1, run("TRUE ? TRUE ? 1 : 2 : TRUE ? 3 : 4").int)
        assertEquals(2, run("TRUE ? FALSE ? 1 : 2 : FALSE ? 3 : 4").int)
        assertEquals(3, run("FALSE ? TRUE ? 1 : 2 : TRUE ? 3 : 4").int)
        assertEquals(4, run("FALSE ? FALSE ? 1 : 2 : FALSE ? 3 : 4").int)

        assertEquals(1, run("1 ?: 2").int) // ?: の左辺が非NULLの場合、左辺を得る
        assertEquals(2, run("NULL ?: 2").int) // ?: の左辺がNULLの場合、右辺を得る
        assertEquals(false, run("FALSE ?: 2").boolean) // FALSEは非NULLである

        // 三項演算子とエルビス演算子は混ぜて書ける
        assertEquals(1, run("TRUE ? 1 ?: 2 : 3 ?: 4").int)
        assertEquals(2, run("TRUE ? NULL ?: 2 : NULL ?: 4").int)
        assertEquals(3, run("FALSE ? 1 ?: 2 : 3 ?: 4").int)
        assertEquals(4, run("FALSE ? NULL ?: 2 : NULL ?: 4").int)

        assertEquals(4, run("FALSE ? 1 : NULL ?: FALSE ? 3 : 4").int) // == FALSE ? 1 : (NULL ?: (FALSE ? 3 : 4))

        // 条件項はTO_BOOLEANで論理値に変換される
        assertEquals(1, run("{TO_BOOLEAN: _ -> TRUE}{} ? 1 : 2").int)
        assertEquals(2, run("{TO_BOOLEAN: _ -> FALSE}{} ? 1 : 2").int)

        // 評価されない項は副作用も起こさない
        assertEquals(2, run("a := 1; TRUE ? (a = 2) : 0; a").int)
        assertEquals(1, run("a := 1; FALSE ? (a = 2) : 0; a").int)
        assertEquals(1, run("a := 1; TRUE ? 0 : (a = 2); a").int)
        assertEquals(2, run("a := 1; FALSE ? 0 : (a = 2); a").int)
        assertEquals(2, run("a := 1; NULL ?: (a = 2); a").int)
        assertEquals(1, run("a := 1; 0 ?: (a = 2); a").int)
    }

    @Test
    fun pipeTest() = runTest {
        // パイプ
        assertEquals("10,20,30", run("1 .. 3 | _ * 10").stream()) // 左辺のストリームを変換する
        assertEquals(10, run("1 | _ * 10").int) // 左辺が非ストリームなら、出力をストリームで梱包しない
        assertEquals("", run("(,) | _ * 10").stream()) // 左辺が空ストリームなら、出力も空ストリームになる

        // 実行パイプ
        assertEquals("1:2:3", run(""" 1, 2, 3 >> JOIN[":"] """).string) // >> で右辺の関数に左辺を適用する
        assertEquals(10.0, run("100 >> SQRT").double, 0.001) // 右辺は非ストリーム用の関数でもよい
        assertEquals(20, run("10 >> x -> x * 2").int) // 右辺はラムダでもよい

        // 左実行パイプ
        assertEquals("1:2:3", run(""" JOIN[":"] << 1, 2, 3 """).string) // << は左右が逆になっただけだが、結合優先度が代入と同等
        assertEquals(2.0, run(""" SQRT << SQRT << 16 """).double, 0.001) // << を並べると、右から左に実行される

        // パイプの連結
        assertEquals("55:33:11", run(""" JOIN[":"] << 1 .. 5 | _ %% 2 ? (,) : _ | _ & _ >> REVERSE """).string) // TODO FILTER [_ %% 2]

        // パイプと代入系演算子は相互に右優先結合だが、パイプ同士の連結部分だけは左優先結合になる
        assertEquals("1-21-2", run("x := 0; f := s -> s >> SPLIT[','] >> JOIN['-'] | x = _ | _ * 2; f('1,2'); x").string)

        // パイプと引数指定演算子との組み合わせ
        assertEquals("10-20-30", run(""" 1 .. 3 | x => x * 10 >> JOIN["-"] """).string)

        // パイプを多段にしても前の段の引数が見える
        assertEquals("14,15,16,24,25,26,34,35,36", run("1 .. 3 | x => 4 .. 6 | y => x & y").stream())

        // パイプと実行パイプの組み合わせ
        assertEquals(4, run("1 | _ + 2 | _ * 3 >> SQRT | _ + 5 | _ + 8 >> SQRT >> FLOOR").int)
        assertEquals("18:19:28:29", run("f := () -> '1-2' >> SPLIT['-'] | x => 8, 9 | y => x & y >> a -> JOIN(':'; a); f()").string)
    }

    @Test
    fun streamTest() = runTest {
        assertEquals("1,2,3", run("1, 2, 3").stream()) // , でストリームが作れる
        assertEquals("1,2,3", run(", , 1, 2, , , 3, , ").stream()) // , は無駄に大量にあってもよい
        assertEquals("1,2,3,4,5,6,7,8,9", run("(1, 2), 3, ((4 .. 6), 7, (8, 9))").stream()) // ストリームを結合すると自動的に平坦になる
        assertEquals("", run(",").stream()) // 単体の , で空ストリームになる
        assertEquals("1", run("1,").stream()) // 値に , を付けると単独でストリームになる
    }

    @Test
    fun builtInClassTest() = runTest {
        // 各クラスのtrue判定
        assertEquals(true, run("1 ?= VALUE_CLASS").boolean)
        assertEquals(true, run("NULL ?= NULL_CLASS").boolean)
        assertEquals(true, run("1 ?= INT_CLASS").boolean)
        assertEquals(true, run("1.2 ?= DOUBLE_CLASS").boolean)
        assertEquals(true, run("TRUE ?= BOOLEAN_CLASS").boolean)
        assertEquals(true, run("'a' ?= STRING_CLASS").boolean)
        assertEquals(true, run("[1] ?= ARRAY_CLASS").boolean)
        assertEquals(true, run("{a: 1} ?= OBJECT_CLASS").boolean)
        assertEquals(true, run("(a -> 1) ?= FUNCTION_CLASS").boolean)
        assertEquals(true, run("(1, 2) ?= STREAM_CLASS").boolean)

        // falseテスト
        assertEquals(false, run("'10' ?= INT_CLASS").boolean)
        assertEquals(false, run("10 ?= STRING_CLASS").boolean)
        assertEquals(false, run("(1, 2) ?= ARRAY_CLASS").boolean)
        assertEquals(false, run("1.2 ?= INT_CLASS").boolean)
        assertEquals(false, run("1 ?= DOUBLE_CLASS").boolean)
    }

    @Test
    fun variableTest() = runTest {
        assertEquals(10, run("a := 10; a").int) // := で変数を定義できる
        assertEquals(20, run("a := 10; a = 20; a").int) // = で既存の変数に代入できる
        assertEquals(10, run("a := 10; (a := 20; a = 30); a").int) // 変数は ( ) の外部に伝搬しない
    }

    @Test
    fun lambdaTest() = runTest {
        assertEquals(10, run("((a) -> a)(10)").int) // (a) -> b で関数を作り、 f(a) で実行する
        assertEquals(12, run("((a; b) -> a * b)(3; 4)").int) // ; で引数を複数取れる
        assertEquals(12, run("((a, b) -> a * b)(3; 4)").int) // ラムダ引数は , で区切ってもよい
        assertEquals(10, run("(() -> 10)()").int) // () で引数を無しにできる
        assertEquals(10, run("(a -> a)(10)").int) // 引数がある場合、 ( ) は省略してもよい
        assertEquals(12, run("(a, b -> a * b)(3; 4)").int) // 引数が複数の場合も ( ) を省略できる

        assertEquals("[1;2;3;4]", run("(s -> &[s])(1, 2, 3, 4)").string) // 引数で , を使うとストリームを渡せる

        assertEquals("[1;2;3;4;5]", run("(() -> &__)(1; 2; 3; 4; 5)").string) // __ で引数を配列で受け取れる
        assertEquals("[4;5;6]", run("(() -> &[__.1])(1 .. 3; 4 .. 6; 7 .. 9)").string) // 引数列ではストリームを展開しない

        assertEquals(120, run("f := n -> n == 0 ? 1 : n * f(n - 1); f(5)").int) // 再帰関数の例
        assertEquals(120, run("(f -> f(f))(f -> n -> n == 0 ? 1 : n * f(f)(n - 1))(5)").int) // 複雑なラムダ計算の例

        // 同じ関数を再帰的に2度呼び出した場合に、関数のフレームが分離されているかどうかのテスト
        assertEquals("[2;1;2]", run("f := n -> n == 1 ? 1 : [n, f(1), n]; f(2)").array())
    }

    @Test
    fun methodTest() = runTest {
        assertEquals(10, run("{method: () -> 10}{}::method()").int) // a::b() でaのbを呼び出せる
        assertEquals(10, run("{method: this -> this.a}{a: 10}::method()").int) // メソッド関数は最初の引数にthisを受け取る
        assertEquals(20, run("{method: this, b -> this.a * b}{a: 10}::method(2)").int) // 2個目以降の引数にメソッド呼び出し時の引数を受け取る

        assertEquals("10", run("10::TO_STRING()").string) // 組み込みメソッドの呼び出し

        assertEquals(10, run("A := {m: _ -> _.v}; a := A {v: 10}; a::m()").int) // メソッドの継承

        assertEquals("{TO_STRING:1}", run("&{TO_STRING: 1}").string) // オブジェクトキーがメソッド名と衝突する場合でもオーバーライドしない
    }

    @Test
    fun bindTest() = runTest {
        assertEquals("12", run("(a, b -> a & b)[1](2)").string) // [ ] で関数に部分適用できる
        assertEquals("12", run("(a, b -> a & b)[1; 2]()").string) // [ ] の中に複数の引数があってもよい
        assertEquals("12", run("(a, b -> a & b)[](1; 2)").string) // [ ] の中が空でもよい
        assertEquals("12", run("(a, b -> a & b)[1][2]()").string) // [ ] を連続して書いてもよい
        assertEquals("12", run("{m: _, a, b -> a & b}{}::m[1](2)").string) // メソッド呼び出しにも使用できる
    }

    @Test
    fun runnerTest() = runTest {
        assertEquals(55, run("x := 0; 1 .. 10 | x = x + _; x").int) // runner部分がストリームの式だった場合、イテレーションはする
    }

    @Test
    fun rootTest() = runTest {
        assertEquals(10, run("10").int) // 式を書ける
        assertEquals(20, run("10; 20").int) // ; で区切ると左は式文になり、右が使われる
        assertEquals(20, run("10\n20").int) // 改行で区切ってもよい
        assertEquals(FluoriteNull, run("10;")) // 式を省略した場合、NULLになる
        assertEquals(10, run("10\n").int) // 式の前後に余計な改行があっても無視される
        assertEquals(FluoriteNull, run("10;\n")) // ; の後に改行があった場合もNULLになる
        assertEquals(30, run("10; 20; 30").int) // ; が複数あってもよい
        assertEquals(20, run("; 10; ; 20").int) // ; の左は省略されていてもよい
        assertEquals(20, run("\n\n;;\n\n10\n\n;;\n\n;;\n\n20\n\n").int) // 改行と;が無駄に大量にあってもよい
        assertEquals(FluoriteNull, run("")) // 何も書かない場合、NULLになる
        assertEquals(FluoriteNull, run(" \t\n ")) // 空白を書いても何もないのと同じになる
    }

    @Test
    fun mathTest() = runTest {
        assertEquals(10.0, run("SQRT(100)").double, 0.001)
    }

    @Test
    fun arrayFunctionTest() = runTest {
        assertEquals("[1;2;3]", run("ARRAY(1, 2, 3)").array()) // ARRAY関数はストリームを配列にする
        assertEquals("[100]", run("ARRAY(100)").array()) // ストリームでなくてもよい
        assertEquals("[10;20;30]", run("1 .. 3 | _ * 10 >> ARRAY").array()) // ARRAY関数はパイプ演算子と組み合わせて使うと便利
    }

    @Test
    fun objectFunctionTest() = runTest {
        assertEquals("{a:1;b:2;c:3}", run("OBJECT((a: 1), (b: 2), (c: 3))").obj) // OBJECT関数はストリームをオブジェクトにする
        assertEquals("{a:100}", run("OBJECT(a: 100)").obj) // ストリームでなくてもよい
        assertEquals("{1:10;2:20;3:30}", run("1 .. 3 | ((_): _ * 10) >> OBJECT").obj) // OBJECT関数はパイプ演算子と組み合わせて使うと便利
    }

    @Test
    fun floorFunctionTest() = runTest {
        assertEquals(10, run("FLOOR(10.1)").int) // FLOOR関数は小数点以下を切り捨てて内部的な型をINTEGERにする
        assertEquals(10, run("FLOOR(10)").int) // 整数はそのまま
        assertEquals(-11, run("FLOOR(-10.1)").int) // 負の数も値が小さくなるように切り捨てる
    }

    @Test
    fun joinSplitTest() = runTest {
        // JOIN
        assertEquals("a|b|c", run(""" JOIN("|"; "a", "b", "c") """).string) // JOIN で文字列を結合できる
        assertEquals("abc", run(""" JOIN(""; "a", "b", "c") """).string) // セパレータは空文字でもよい
        assertEquals("a123b123c", run(""" JOIN("123"; "a", "b", "c") """).string) // セパレータは複数文字でもよい
        assertEquals("a|b", run(""" JOIN("|"; "a", "b") """).string) // ストリームは2要素でもよい
        assertEquals("a", run(""" JOIN("|"; "a",) """).string) // ストリームは1要素でもよい
        assertEquals("", run(""" JOIN("|"; ,) """).string) // ストリームは空でもよい
        assertEquals("a", run(""" JOIN("|"; "a") """).string) // ストリームは非ストリームでもよい
        assertEquals("10|[20]|30", run(""" JOIN("|"; 10, [20], {TO_STRING: _ -> 30}{}) """).string) // ストリームは文字列化される
        assertEquals("a1b1c", run(""" JOIN(1; "a", "b", "c") """).string) // セパレータも文字列化される
        assertEquals("a|b|c", run(""" JOIN["|"]("a", "b", "c") """).string) // 部分適用を使用した例

        // SPLIT
        assertEquals("a,b,c", run(""" SPLIT("|"; "a|b|c") """).stream()) // SPLIT で文字列を分割できる
        assertEquals("a,b,c", run(""" SPLIT(""; "abc") """).stream()) // セパレータは空文字でもよい
        assertEquals("a,b,c", run(""" SPLIT("123"; "a123b123c") """).stream()) // セパレータは複数文字でもよい
        assertEquals("a,b", run(""" SPLIT("|"; "a|b") """).stream()) // 文字列は2要素でもよい
        assertEquals("a", run(""" SPLIT("|"; "a") """).stream()) // 文字列は1要素でもよい
        assertEquals("", run(""" SPLIT("|"; "") """).stream()) // 文字列は空でもよい
        assertEquals("1,2,3", run(""" SPLIT("0"; 10203) """).stream()) // 文字列は文字列化される
        assertEquals("a,b,c", run(""" SPLIT(1; "a1b1c") """).stream()) // セパレータは文字列化される
        assertEquals("a,b,c", run(""" SPLIT["|"]("a|b|c") """).stream()) // 部分適用を使用した例

        // パイプ連携
        assertEquals("10ABC20ABC30", run(""" "10abc20abc30" >> SPLIT["abc"] >> JOIN["ABC"] """).string)
    }

    @Test
    fun keysValuesTest() = runTest {
        assertEquals("a,b,c", run("KEYS({a: 1; b: 2; c: 3})").stream()) // KEYS でオブジェクトのキーを得る
        assertEquals("1,2,3", run("VALUES({a: 1; b: 2; c: 3})").stream()) // VALUES でオブジェクトの値を得る
    }

    @Test
    fun sumFunctionTest() = runTest {
        assertEquals(0, run("SUM(,)").int) // 引数がない場合は0
        assertEquals(1, run("SUM(1)").int) // 引数が1つの場合はそのまま
        assertEquals(3, run("SUM(1, 2)").int) // 引数が2つ以上の場合は合計
    }

    @Test
    fun reverseTest() = runTest {
        assertEquals("3,2,1", run("REVERSE(1, 2, 3)").stream()) // REVERSE でストリームを逆順にする
        assertEquals("3:2:1", run(" '1-2-3' >> SPLIT['-'] >> REVERSE >> JOIN[':'] ").string) // REVERSE はパイプと組み合わせて使うと便利
    }

    @Test
    fun minMaxTest() = runTest {
        assertEquals(1.0, run("MIN(1.0, 2.0, 3.0)").double) // MIN で最小値を得る
        assertEquals(FluoriteNull, run("MIN(,)")) // 空ストリームの場合、NULL
        assertEquals(3.0, run("MAX(1.0, 2.0, 3.0)").double) // MAX で最大値を得る
        assertEquals(FluoriteNull, run("MAX(,)")) // 空ストリームの場合、NULL
    }

    @Test
    fun tryRunnerTest() = runTest {
        assertEquals("end", run("(1 .. 3 | !!'error') !? 'ignore'; 'end'").string) // パイプRunnerの中でエラーが発生してもキャッチできる
    }

    @Test
    fun objectAssignmentTest() = runTest {
        assertEquals("{a:1;b:9}", run("o := {a: 1; b: 2}; o.b = 9; o").obj) // オブジェクトのフィールドに代入できる
        assertEquals("{a:1;b:2;c:9}", run("o := {a: 1; b: 2}; o.c = 9; o").obj) // 存在しないフィールドに代入すると新規追加される
    }

}
