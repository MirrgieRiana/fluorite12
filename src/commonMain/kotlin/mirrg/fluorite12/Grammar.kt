package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.rightAssociative
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.EmptyParser
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2
import com.github.h0tk3y.betterParse.utils.Tuple3
import kotlin.jvm.JvmName

/** このクラスの同一インスタンスによって異なるソース文字列をパースする際には、その前に[clearCache]を呼び出す必要があります。 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class Fluorite12Grammar : Grammar<Node>() {
    private val onClearCache = mutableListOf<() -> Unit>() // 最初に無いといけない

    private fun <T> cachedParser(block: () -> Parser<T>): Parser<T> {
        val cachedParser = CachedParser(parser(block))
        onClearCache += { cachedParser.clear() }
        return cachedParser
    }

    fun clearCache() {
        onClearCache.forEach {
            it()
        }
    }

    val tab by literalToken("\t")
    val br by regexToken("""\n|\r\n?""".toRegex())

    val space by literalToken(" ")
    val exclamation by literalToken("!")
    val dQuote by literalToken("\"")
    val sharp by literalToken("#")
    val dollar by literalToken("$")
    val percent by literalToken("%")
    val ampersand by literalToken("&")
    val sQuote by literalToken("'")
    val lRound by literalToken("(")
    val rRound by literalToken(")")
    val asterisk by literalToken("*")
    val plus by literalToken("+")
    val comma by literalToken(",")
    val minus by literalToken("-")
    val period by literalToken(".")
    val slash by literalToken("/")

    val zero by literalToken("0")
    val nonZero by regexToken("""[1-9]""".toRegex())
    val colon by literalToken(":")
    val semicolon by literalToken(";")
    val greater by literalToken(">")
    val equal by literalToken("=")
    val less by literalToken("<")
    val question by literalToken("?")

    val atSign by literalToken("@")
    val uA by literalToken("A")
    val uB by literalToken("B")
    val uC by literalToken("C")
    val uD by literalToken("D")
    val uE by literalToken("E")
    val uF by literalToken("F")
    val uG by literalToken("G")
    val uH by literalToken("H")
    val uI by literalToken("I")
    val uJ by literalToken("J")
    val uK by literalToken("K")
    val uL by literalToken("L")
    val uM by literalToken("M")
    val uN by literalToken("N")
    val uO by literalToken("O")

    val uP by literalToken("P")
    val uQ by literalToken("Q")
    val uR by literalToken("R")
    val uS by literalToken("S")
    val uT by literalToken("T")
    val uU by literalToken("U")
    val uV by literalToken("V")
    val uW by literalToken("W")
    val uX by literalToken("X")
    val uY by literalToken("Y")
    val uZ by literalToken("Z")
    val lSquare by literalToken("[")
    val bSlash by literalToken("\\")
    val rSquare by literalToken("]")
    val circumflex by literalToken("^")
    val underscore by literalToken("_")

    val bQuote by literalToken("`")
    val lA by literalToken("a")
    val lB by literalToken("b")
    val lC by literalToken("c")
    val lD by literalToken("d")
    val lE by literalToken("e")
    val lF by literalToken("f")
    val lG by literalToken("g")
    val lH by literalToken("h")
    val lI by literalToken("i")
    val lJ by literalToken("j")
    val lK by literalToken("k")
    val lL by literalToken("l")
    val lM by literalToken("m")
    val lN by literalToken("n")
    val lO by literalToken("o")

    val lP by literalToken("p")
    val lQ by literalToken("q")
    val lR by literalToken("r")
    val lS by literalToken("s")
    val lT by literalToken("t")
    val lU by literalToken("u")
    val lV by literalToken("v")
    val lW by literalToken("w")
    val lX by literalToken("x")
    val lY by literalToken("y")
    val lZ by literalToken("z")
    val lCurly by literalToken("{")
    val pipe by literalToken("|")
    val rCurly by literalToken("}")
    val tilde by literalToken("~")

    val other by regexToken(""".""".toRegex()) // \r\n\t以外の制御文字、DEL、すべての2バイト文字、サロゲートペアの片方


    val s by zeroOrMore(space or tab)
    val b by zeroOrMore(space or tab) * zeroOrMore(br * zeroOrMore(space or tab))
    val uAlphabet by uA or uB or uC or uD or uE or uF or uG or uH or uI or uJ or uK or uL or uM or uN or uO or uP or uQ or uR or uS or uT or uU or uV or uW or uX or uY or uZ
    val lAlphabet by lA or lB or lC or lD or lE or lF or lG or lH or lI or lJ or lK or lL or lM or lN or lO or lP or lQ or lR or lS or lT or lU or lV or lW or lX or lY or lZ
    val number by zero or nonZero

    val identifier: Parser<Node> by (uAlphabet or lAlphabet or underscore) * zeroOrMore(uAlphabet or lAlphabet or underscore or number) map {
        val tokens = listOf(it.t1, *it.t2.toTypedArray())
        IdentifierNode(tokens, tokens.joinToString("") { t -> t.text })
    }

    val float: Parser<Node> by oneOrMore(number) * period * oneOrMore(number) map {
        val tokens = it.t1 + it.t2 + it.t3
        FloatNode(tokens, tokens.text)
    }

    val integer: Parser<Node> by oneOrMore(number) map { IntegerNode(it, it.joinToString("") { t -> t.text }) }

    val hexadecimalCharacter by number or lA or uA or lB or uB or lC or uC or lD or uD or lE or uE or lF or uF
    val hexadecimal: Parser<Node> by uH * sharp * oneOrMore(hexadecimalCharacter) map { HexadecimalNode(listOf(it.t1, it.t2, *it.t3.toTypedArray()), it.t3.joinToString("") { t -> t.text }) }

    val rawStringCharacter by OrCombinator(
        -NotParser(sQuote or br) * AnyParser map { Pair(listOf(it), it.text) }, // ' 以外の文字
        br map { Pair(listOf(it), "\n") }, // 改行
        sQuote * sQuote map { Pair(listOf(it.t1, it.t2), "'") } // '
    )
    val rawStringContent by zeroOrMore(rawStringCharacter) map { LiteralStringContent(it.flatMap { t -> t.first }, it.joinToString("") { t -> t.second }) }
    val rawString by sQuote * rawStringContent * sQuote map { RawStringNode(it.t1, it.t2, it.t3) }

    val templateStringCharacter by OrCombinator(
        -NotParser(dQuote or br or dollar or bSlash) * AnyParser map { Pair(listOf(it), it.text) }, // 通常文字
        br map { Pair(listOf(it), "\n") }, // 改行
        bSlash * (dQuote or dollar or bSlash) map { Pair(listOf(it.t1, it.t2), it.t2.text) }, // エスケープされた記号
        bSlash * (lT) map { Pair(listOf(it.t1, it.t2), "\t") },
        bSlash * (lR) map { Pair(listOf(it.t1, it.t2), "\r") },
        bSlash * (lN) map { Pair(listOf(it.t1, it.t2), "\n") },
    )
    val formatterFlag by OrCombinator(
        minus map { Pair(it, FormatterFlag.LEFT_ALIGNED) },
        plus map { Pair(it, FormatterFlag.SIGNED) },
        space map { Pair(it, FormatterFlag.SPACE_FOR_SIGN) },
        zero map { Pair(it, FormatterFlag.LEADING_ZEROS) },
    )
    val formatterConversion by OrCombinator(
        lD map { Pair(it, FormatterConversion.DECIMAL) },
        lX map { Pair(it, FormatterConversion.HEXADECIMAL) },
        lF map { Pair(it, FormatterConversion.FLOAT) },
        lS map { Pair(it, FormatterConversion.STRING) },
    )
    val templateStringContent by OrCombinator(
        oneOrMore(templateStringCharacter) map { LiteralStringContent(it.flatMap { t -> t.first }, it.joinToString("") { t -> t.second }) },
        dollar * cachedParser { factor } map { NodeStringContent(listOf(it.t1), it.t2, listOf()) },
        dollar * percent * zeroOrMore(formatterFlag) * zeroOrMore(number) * optional(period * oneOrMore(number)) * formatterConversion * cachedParser { round or square or curly } map { parameters ->
            val left = mutableListOf(parameters.t1, parameters.t2)

            val flags = parameters.t3.map { it.second }.toSet()
            left += parameters.t3.map { it.first }

            val width = if (parameters.t4.isNotEmpty()) parameters.t4.joinToString("") { it.text }.toInt() else null
            left += parameters.t4

            val precision = parameters.t5?.t2?.joinToString("") { a -> a.text }?.toInt()
            left += parameters.t5?.let { listOf(it.t1) + it.t2 } ?: listOf()

            val conversion = parameters.t6.second
            left += parameters.t6.first

            FormattedStringContent(left, Formatter(left.joinToString("") { it.text }, flags, width, precision, conversion), parameters.t7)
        },
    )
    val templateString by dQuote * zeroOrMore(templateStringContent) * dQuote map { TemplateStringNode(it.t1, it.t2, it.t3) }

    val embeddedStringCharacter by OrCombinator(
        -NotParser(less * percent or br) * AnyParser map { Pair(listOf(it), it.text) }, // 通常文字
        br map { Pair(listOf(it), "\n") }, // 改行
        less * percent * percent map { Pair(listOf(it.t1, it.t2, it.t3), "<%") }, // <%% で <% になる
    )
    val embeddedStringContent by OrCombinator(
        oneOrMore(embeddedStringCharacter) map { LiteralStringContent(it.flatMap { t -> t.first }, it.joinToString("") { t -> t.second }) },
        less * percent * equal * -b * cachedParser { expression } * -b * percent * greater map { NodeStringContent(listOf(it.t1, it.t2, it.t3), it.t4, listOf(it.t5, it.t6)) }, // <%= expression %>
    )
    val embeddedString by percent * greater * zeroOrMore(embeddedStringContent) * less * percent map { EmbeddedStringNode(listOf(it.t1, it.t2), it.t3, listOf(it.t4, it.t5)) } // %>string<%

    val round: Parser<Node> by lRound * -b * optional(cachedParser { expression } * -b) * rRound map ::bracketNode
    val square: Parser<Node> by lSquare * -b * optional(cachedParser { expression } * -b) * rSquare map ::bracketNode
    val curly: Parser<Node> by lCurly * -b * optional(cachedParser { expression } * -b) * rCurly map ::bracketNode
    val factor: Parser<Node> by hexadecimal or identifier or float or integer or rawString or templateString or embeddedString or round or square or curly

    val rightOperator: Parser<(Node) -> Node> by OrCombinator(
        -s * lRound * -b * optional(cachedParser { expression } * -b) * rRound map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lSquare * -b * optional(cachedParser { expression } * -b) * rSquare map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lCurly * -b * optional(cachedParser { expression } * -b) * rCurly map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },

        -b * +period * -b * factor map { { main -> InfixNode(main, it.t1, it.t2) } },
        -b * +(colon * colon) * -b * factor map { { main -> InfixNode(main, it.t1, it.t2) } },

        -b * +(period * plus) map { { main -> RightNode(main, it) } },
        -b * +(period * minus) map { { main -> RightNode(main, it) } },
        -b * +(period * question) map { { main -> RightNode(main, it) } },
        -b * +(period * exclamation * exclamation) map { { main -> RightNode(main, it) } },
        -b * +(period * exclamation) map { { main -> RightNode(main, it) } },
        -b * +(period * ampersand) map { { main -> RightNode(main, it) } },
        -b * +(period * dollar * sharp) map { { main -> RightNode(main, it) } },
        -b * +(period * dollar * ampersand) map { { main -> RightNode(main, it) } },
        -b * +(period * dollar * asterisk) map { { main -> RightNode(main, it) } },
    )
    val right: Parser<Node> by factor * zeroOrMore(rightOperator) map { it.t2.fold(it.t1) { node, f -> f(node) } }
    val leftOperator: Parser<(Node) -> Node> by OrCombinator(
        +(plus) map { { main -> LeftNode(it, main) } },
        +(minus) map { { main -> LeftNode(it, main) } },
        +(question) map { { main -> LeftNode(it, main) } },
        +(exclamation * exclamation) map { { main -> LeftNode(it, main) } },
        +(exclamation) map { { main -> LeftNode(it, main) } },
        +(ampersand) map { { main -> LeftNode(it, main) } },
        +(dollar * sharp) map { { main -> LeftNode(it, main) } },
        +(dollar * ampersand) map { { main -> LeftNode(it, main) } },
        +(dollar * asterisk) map { { main -> LeftNode(it, main) } },
    )
    val left: Parser<Node> by zeroOrMore(leftOperator) * right map { it.t1.foldRight(it.t2) { f, node -> f(node) } }

    val mul: Parser<Node> by leftAssociative(left, -s * (+asterisk or +slash or +(percent * percent) or +percent) * -b, ::infixNode)
    val add: Parser<Node> by leftAssociative(mul, -s * (+plus or +(minus * -NotParser(greater)) or +(ampersand * -NotParser(ampersand))) * -b, ::infixNode)
    val range: Parser<Node> by leftAssociative(add, -s * +(period * period) * -b, ::infixNode)
    val comparisonOperator: Parser<List<TokenMatch>> by OrCombinator(
        +(equal * equal), // ==
        +(exclamation * equal), // !=
        +(greater * equal), // >=
        +greater, // >
        +(less * equal), // <=
        +less, // <
        +(question * equal), // ?=
        +atSign, // @
    )
    val comparison: Parser<Node> by range * zeroOrMore(-s * comparisonOperator * -b * range) map {
        if (it.t2.isNotEmpty()) {
            ComparisonNode(listOf(it.t1, *it.t2.map { t -> t.t2 }.toTypedArray()), it.t2.map { t -> t.t1 })
        } else {
            it.t1
        }
    }
    val and: Parser<Node> by leftAssociative(comparison, -s * +(ampersand * ampersand) * -b, ::infixNode)
    val or: Parser<Node> by leftAssociative(and, -s * +(pipe * pipe) * -b, ::infixNode)
    val condition: Parser<Node> by OrCombinator(
        or * -s * question * -b * cachedParser { condition } * -s * (colon * -NotParser(colon)) * -b * cachedParser { condition } map { ConditionNode(it.t1, it.t2, it.t3, it.t4, it.t5) },
        or * -s * +(question * colon) * -b * cachedParser { condition } map { InfixNode(it.t1, it.t2, it.t3) },
        or,
    )

    val commasPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        (condition * -b or (EmptyParser map { EmptyNode })) * comma * (-b * cachedParser { commasPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        condition map { Pair(listOf(it), listOf()) },
    )
    val commas: Parser<Node> by commasPart map { if (it.first.size == 1) it.first.first() else CommaNode(it.first, it.second) }
    val assignationOperator: Parser<List<TokenMatch>> by OrCombinator(
        +(equal * -NotParser(greater)), // =
        +(colon * -NotParser(equal or colon)), // :
        +(colon * equal), // :=
        +(minus * greater), // ->
        +(equal * greater), // =>
        +(exclamation * question), // !?
    )
    val assignation: Parser<Node> by rightAssociative(commas, -s * assignationOperator * -b, ::infixNode)
    val streamOperator: Parser<List<TokenMatch>> by OrCombinator(
        +pipe,
        +(question * pipe),
        +(exclamation * pipe),
        +(greater * greater),
        +(less * less),
    )
    val stream: Parser<Node> by leftAssociative(assignation, -s * streamOperator * -b, ::infixNode)

    val semicolonsPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        stream * -s * br * -b * cachedParser { semicolonsPart } map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        (stream * -s or (EmptyParser map { EmptyNode })) * semicolon * (-b * cachedParser { semicolonsPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        stream map { Pair(listOf(it), listOf()) },
    )
    val semicolons: Parser<Node> by semicolonsPart map { if (it.first.size == 1) it.first.first() else SemicolonNode(it.first, it.second) }
    val expression: Parser<Node> by semicolons

    override val rootParser: Parser<Node> by -b * optional(expression * -b) map { RootNode(it ?: EmptyNode) }
}

@JvmName("unaryPlus1")
private operator fun Parser<TokenMatch>.unaryPlus() = this map { listOf(it) }

@JvmName("unaryPlus2")
private operator fun Parser<Tuple2<TokenMatch, TokenMatch>>.unaryPlus() = this map { listOf(it.t1, it.t2) }

@JvmName("unaryPlus3")
private operator fun Parser<Tuple3<TokenMatch, TokenMatch, TokenMatch>>.unaryPlus() = this map { listOf(it.t1, it.t2, it.t3) }

private fun bracketNode(it: Tuple3<TokenMatch, Node?, TokenMatch>) = BracketNode(it.t1, it.t2 ?: EmptyNode, it.t3)

private fun infixNode(left: Node, operator: List<TokenMatch>, right: Node) = InfixNode(left, operator, right)
