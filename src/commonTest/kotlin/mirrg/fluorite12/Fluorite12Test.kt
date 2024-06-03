package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
            assertEquals("[123456;12345;123;0;-123;-1234;-12345;-123456]", run(""" [$s | "$%d(_)"] """).array) // %d で整数
            assertEquals("[123456;12345;  123;    0; -123;-1234;-12345;-123456]", run(""" [$s | "$%5d(_)"] """).array) // 空白埋め
            assertEquals("[123456;12345;00123;00000;-0123;-1234;-12345;-123456]", run(""" [$s | "$%05d(_)"] """).array) // 0埋め
            assertEquals("[123456;12345;123  ;0    ;-123 ;-1234;-12345;-123456]", run(""" [$s | "$%-5d(_)"] """).array) // 左揃え空白埋め
            assertEquals("[+123456;+12345;+123;+0;-123;-1234;-12345;-123456]", run(""" [$s | "$%+d(_)"] """).array) // 符号表示
            assertEquals("[ 123456; 12345; 123; 0;-123;-1234;-12345;-123456]", run(""" [$s | "$% d(_)"] """).array) // 符号余白
            assertEquals("[+123456;+12345; +123;   +0; -123;-1234;-12345;-123456]", run(""" [$s | "$%+5d(_)"] """).array) // 符号表示 空白埋め
            assertEquals("[ 123456; 12345;  123;    0; -123;-1234;-12345;-123456]", run(""" [$s | "$% 5d(_)"] """).array) // 符号余白 空白埋め
            assertEquals("[+123456;+12345;+0123;+0000;-0123;-1234;-12345;-123456]", run(""" [$s | "$%+05d(_)"] """).array) // 符号表示 0埋め
            assertEquals("[ 123456; 12345; 0123; 0000;-0123;-1234;-12345;-123456]", run(""" [$s | "$% 05d(_)"] """).array) // 符号余白 0埋め
        }

        // 16進数
        run {
            val s = """H#abcdef, H#abcde, H#abc, H#0, -H#abc, -H#abcd, -H#abcde, -H#abcdef"""
            assertEquals("[abcdef;abcde;abc;0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%x(_)"] """).array) // %x で16進数
            assertEquals("[abcdef;abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%5x(_)"] """).array) // 空白埋め
            assertEquals("[abcdef;abcde;00abc;00000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%05x(_)"] """).array) // 0埋め
            assertEquals("[abcdef;abcde;abc  ;0    ;-abc ;-abcd;-abcde;-abcdef]", run(""" [$s | "$%-5x(_)"] """).array) // 左揃え空白埋め
            assertEquals("[+abcdef;+abcde;+abc;+0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+x(_)"] """).array) // 符号表示
            assertEquals("[ abcdef; abcde; abc; 0;-abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% x(_)"] """).array) // 符号余白
            assertEquals("[+abcdef;+abcde; +abc;   +0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+5x(_)"] """).array) // 符号表示 空白埋め
            assertEquals("[ abcdef; abcde;  abc;    0; -abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% 5x(_)"] """).array) // 符号余白 空白埋め
            assertEquals("[+abcdef;+abcde;+0abc;+0000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$%+05x(_)"] """).array) // 符号表示 0埋め
            assertEquals("[ abcdef; abcde; 0abc; 0000;-0abc;-abcd;-abcde;-abcdef]", run(""" [$s | "$% 05x(_)"] """).array) // 符号余白 0埋め
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
            assertEquals("[;abcd;abcde;abcdef]", run(""" [$s | "$%s(_)"] """).array) // %s で文字列
            assertEquals("[     ; abcd;abcde;abcdef]", run(""" [$s | "$%5s(_)"] """).array) // 空白埋め
            assertEquals("[     ;abcd ;abcde;abcdef]", run(""" [$s | "$%-5s(_)"] """).array) // 左揃え空白埋め
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
    fun bracketTest() = runTest {
        assertEquals(1, run("(1)").int) // ( ) で囲うと中身をそのまま得られる
        assertEquals(FluoriteNull, run("()")) // () でNULLになる
        assertEquals("", run("(,)").stream()) // (,) で空ストリームになる
    }

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
        assertEquals(FluoriteNull, run(" 'abc'.3 ")) // 文字列の範囲外にアクセスすると NULL が返る
        assertEquals("c", run(" 'abc'.(1 + 1) ").string) // キーを ( ) で囲むと式で参照できる

        assertEquals(20, run(" [10, 20, 30].1 ").int) // 配列に数値アクセスするとそのインデックスの要素を得る
        assertEquals(FluoriteNull, run(" [10, 20, 30].3 ")) // 配列の範囲外にアクセスすると NULL が返る
        assertEquals(30, run(" [10, 20, 30].(1 + 1) ").int) // キーを ( ) で囲むと式で参照できる

        assertEquals(10, run(" {a: 10; b: 20}.a ").int) // オブジェクトに識別子アクセスするとその要素を得る
        assertEquals(10, run(" {a: 10; b: 20}.'a' ").int) // キーは文字列リテラルでもよい
        assertEquals(FluoriteNull, run(" {a: 10; b: 20}.c ")) // 存在しない要素にアクセスすると NULL が返る
        assertEquals(20, run(" 'b' | a => {a: 10; b: 20}.(a) ").int) // キーを ( ) で囲むと式で参照できる
    }

    @Test
    fun bracketAccessTest() = runTest {
        assertEquals("1,2,3", run("[1; 2; 3][]").stream()) // array[] でストリームにする
        assertEquals("[a;1],[b;2],[c;3]", run("{a: 1; b: 2; c: 3}[]").stream()) // object[] でエントリーのストリームにする
    }

    @Test
    fun modTest() = runTest {
        assertEquals(1, run("10 % 3").int) // % で余りを得る

        // 負の余りは正になるまで割る数を足したものの余りと同じ（-1 + 3 = 2） % 3
        // そのため同じ余りがループする
        assertEquals("[0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0;1;2;3;4;0]", run("&[-10 .. 10 | _ % 5]").string)

        assertEquals(false, run("10 %% 3").boolean) // %% は割り切れる場合にTRUE
        assertEquals(true, run("10 %% 2").boolean) // %% は割り切れない場合にFALSE
        assertEquals(true, run("-3 %% 3").boolean) // %% も負に対応
    }

    @Test
    fun stringConcatenateTest() = runTest {
        assertEquals("ab", run(" 'a' & 'b' ").string) // & で文字列の連結ができる
        assertEquals("12", run(" 1 & 2 ").string) // 文字列に変換する
    }

    @Test
    fun rangeTest() = runTest {
        assertEquals("[1;2;3;4]", run("&[1 .. 4]").string) // .. でその範囲をイテレートするストリームを得る
        assertEquals("[0;1;2;3]", run("&[0 .. 4 - 1]").string) // 項は0や四則演算等でもよい
        assertEquals("[-1;0;1]", run("&[-1 .. 1]").string) // 項は0や四則演算等でもよい
        assertEquals("[[1;2;3;4];[1;2;3;4]]", run("a := 1 .. 4; &[[a]; [a]]").string) // 範囲ストリームは再利用できる
    }

    @Test
    fun compareTest() = runTest {
        fun String.f() = this.replace(" ", "")

        // 比較ができる
        assertEquals("[FALSE;FALSE;TRUE ]".f(), run("[0 >  1; 1 >  1; 2 >  1]").array)
        assertEquals("[TRUE ;FALSE;FALSE]".f(), run("[0 <  1; 1 <  1; 2 <  1]").array)
        assertEquals("[FALSE;TRUE ;TRUE ]".f(), run("[0 >= 1; 1 >= 1; 2 >= 1]").array)
        assertEquals("[TRUE ;TRUE ;FALSE]".f(), run("[0 <= 1; 1 <= 1; 2 <= 1]").array)

        // 浮動小数でもよい
        assertEquals("[FALSE;FALSE;FALSE]".f(), run("[1 >  1.0; 1.0 >  1; 1.0 >  1.0]").array)
        assertEquals("[FALSE;FALSE;FALSE]".f(), run("[1 <  1.0; 1.0 <  1; 1.0 <  1.0]").array)
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), run("[1 >= 1.0; 1.0 >= 1; 1.0 >= 1.0]").array)
        assertEquals("[TRUE ;TRUE ;TRUE ]".f(), run("[1 <= 1.0; 1.0 <= 1; 1.0 <= 1.0]").array)
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

        // フィルターパイプ
        assertEquals("2,4", run("1 .. 5 ?| _ %% 2").stream()) // 左辺のストリームをフィルタする
        assertEquals("1,3,5", run("1 .. 5 !| _ %% 2").stream()) // 否定フィルタパイプ
        assertEquals("5", run("1 .. 5 ?| _ %% 5").stream()) // 1件しかマッチしない場合でもストリームを返す
        assertEquals("", run("1 .. 5 ?| _ %% 7").stream()) // 何もマッチしない場合は空ストリームを返す
        assertEquals(5, run("5 ?| _ %% 5").int) // 左辺が非ストリームの場合、マッチした場合はそれをそのまま返す
        assertEquals("", run("5 ?| _ %% 7").stream()) // 左辺が非ストリームの場合でも、マッチしなかった場合は空ストリームを返す

        // 実行パイプ
        assertEquals("1:2:3", run(""" 1, 2, 3 >> JOIN[":"] """).string) // >> で右辺の関数に左辺を適用する
        assertEquals(10.0, run("100 >> SQRT").double, 0.001) // 右辺は非ストリーム用の関数でもよい
        assertEquals(20, run("10 >> x -> x * 2").int) // 右辺はラムダでもよい
        assertEquals("1:2:3", run(""" JOIN[":"] << 1, 2, 3 """).string) // << は左右が逆になっただけ

        // パイプの連結
        assertEquals("11;33;55", run(""" SPLIT[":"] << "1:2:3:4:5" !| +_ %% 2 | _ & _ >> JOIN[";"] """).string)
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
}

private fun parse(src: String): String {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    frame.defineCommonBuiltinConstants()
    val getter = frame.compileToGetter(parseResult.value)
    return getter.code
}

private suspend fun run(src: String): FluoriteValue {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runners = frame.defineCommonBuiltinConstants()
    val getter = frame.compileToGetter(parseResult.value)
    val env = Environment(null, frame.nextVariableIndex)
    runners.forEach {
        it.evaluate(env)
    }
    return getter.evaluate(env)
}

private val FluoriteValue.int get() = (this as FluoriteInt).value
private val FluoriteValue.double get() = (this as FluoriteDouble).value
private val FluoriteValue.boolean get() = (this as FluoriteBoolean).value
private val FluoriteValue.string get() = (this as FluoriteString).value
private val FluoriteValue.obj get() = (this as FluoriteObject).toString()
private val FluoriteValue.array get() = (this as FluoriteArray).toString()
private suspend fun FluoriteValue.stream() = flow { (this@stream as FluoriteStream).flowProvider(this) }.toList(mutableListOf()).joinToString(",") { "$it" }
