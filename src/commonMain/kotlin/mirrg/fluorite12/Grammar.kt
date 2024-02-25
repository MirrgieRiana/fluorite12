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
    val circumflex by regexToken("^")
    val underscore by regexToken("_")

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

    val identifier: Parser<Node> by (uAlphabet or lAlphabet or underscore) * zeroOrMore(uAlphabet or lAlphabet or underscore or zero or nonZero) map {
        val tokens = listOf(it.t1, *it.t2.toTypedArray())
        IdentifierNode(tokens, tokens.joinToString("") { t -> t.text })
    }

    val float: Parser<Node> by oneOrMore(zero or nonZero) * period * oneOrMore(zero or nonZero) map {
        val tokens = it.t1 + it.t2 + it.t3
        FloatNode(tokens, tokens.text)
    }

    val integer: Parser<Node> by oneOrMore(zero or nonZero) map { IntegerNode(it, it.joinToString("") { t -> t.text }) }

    val rawStringCharacter by OrCombinator(
        -NotParser(sQuote) * AnyParser map { Pair(listOf(it), it.text) }, // ' 以外の文字
        sQuote * sQuote map { Pair(listOf(it.t1, it.t2), "'") } // '
    )
    val rawStringContent by zeroOrMore(rawStringCharacter) map { LiteralStringContent(it.flatMap { t -> t.first }, it.joinToString("") { t -> t.second }) }
    val rawString by sQuote * rawStringContent * sQuote map { RawStringNode(it.t1, it.t2, it.t3) }

    val templateStringCharacter by OrCombinator(
        -NotParser(dQuote or dollar or bSlash) * AnyParser map { Pair(listOf(it), it.text) }, // 通常文字
        bSlash * (dQuote or dollar or bSlash) map { Pair(listOf(it.t1, it.t2), it.t2.text) }, // エスケープされた記号
        bSlash * (lT) map { Pair(listOf(it.t1, it.t2), "\t") },
        bSlash * (lR) map { Pair(listOf(it.t1, it.t2), "\r") },
        bSlash * (lN) map { Pair(listOf(it.t1, it.t2), "\n") },
    )
    val templateStringContent by OrCombinator(
        oneOrMore(templateStringCharacter) map { LiteralStringContent(it.flatMap { t -> t.first }, it.joinToString("") { t -> t.second }) },
        dollar * cachedParser { factor } map { NodeStringContent(listOf(it.t1), it.t2, listOf()) },
    )
    val templateString by dQuote * zeroOrMore(templateStringContent) * dQuote map { TemplateStringNode(it.t1, it.t2, it.t3) }

    val embeddedStringCharacter by OrCombinator(
        -NotParser(less * percent) * AnyParser map { Pair(listOf(it), it.text) }, // 通常文字
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
    val factor: Parser<Node> by identifier or float or integer or rawString or templateString or embeddedString or round or square or curly

    val rightOperator: Parser<(Node) -> Node> by OrCombinator(
        -s * lRound * -b * optional(cachedParser { expression } * -b) * rRound map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lSquare * -b * optional(cachedParser { expression } * -b) * rSquare map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lCurly * -b * optional(cachedParser { expression } * -b) * rCurly map { { main -> RightBracketNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -b * +period * -b * factor map { { main -> InfixNode(main, it.t1, it.t2) } },
        -b * +(colon * colon) * -b * factor map { { main -> InfixNode(main, it.t1, it.t2) } },
    )
    val right: Parser<Node> by factor * zeroOrMore(rightOperator) map { it.t2.fold(it.t1) { node, f -> f(node) } }
    val leftOperator: Parser<(Node) -> Node> by OrCombinator(
        +plus map { { main -> LeftNode(it, main) } },
        +minus map { { main -> LeftNode(it, main) } },
        +question map { { main -> LeftNode(it, main) } },
        +exclamation map { { main -> LeftNode(it, main) } },
        +ampersand map { { main -> LeftNode(it, main) } },
        +(dollar * sharp) map { { main -> LeftNode(it, main) } },
        +(dollar * ampersand) map { { main -> LeftNode(it, main) } },
        +(dollar * asterisk) map { { main -> LeftNode(it, main) } },
    )
    val left: Parser<Node> by zeroOrMore(leftOperator) * right map { it.t1.foldRight(it.t2) { f, node -> f(node) } }

    val mul: Parser<Node> by leftAssociative(left, -s * (+asterisk or +slash) * -b, ::infixNode)
    val add: Parser<Node> by leftAssociative(mul, -s * (+plus or +(minus * -NotParser(greater))) * -b, ::infixNode)
    val range: Parser<Node> by leftAssociative(add, -s * +(period * period) * -b, ::infixNode)
    val comparisonOperator: Parser<List<TokenMatch>> by OrCombinator(
        +(equal * equal), // ==
        +(exclamation * equal), // !=
        +greater, // >
        +(greater * equal), // >=
        +less, // <
        +(less * equal), // <=
        +(question * equal), // ?=
    )
    val comparison: Parser<Node> by range * zeroOrMore(-s * comparisonOperator * -b * range) map {
        if (it.t2.isNotEmpty()) {
            ComparisonNode(listOf(it.t1, *it.t2.map { t -> t.t2 }.toTypedArray()), it.t2.map { t -> t.t1 })
        } else {
            it.t1
        }
    }
    val condition: Parser<Node> by OrCombinator(
        comparison * -s * question * -b * cachedParser { condition } * -s * (colon * -NotParser(colon)) * -b * cachedParser { condition } map { ConditionNode(it.t1, it.t2, it.t3, it.t4, it.t5) },
        comparison * -s * +(question * colon) * -b * cachedParser { condition } map { InfixNode(it.t1, it.t2, it.t3) },
        comparison,
    )

    val commasPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        (condition * -b or (EmptyParser map { EmptyNode })) * comma * (-b * cachedParser { commasPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        condition map { Pair(listOf(it), listOf()) },
    )
    val commas: Parser<Node> by commasPart map { if (it.first.size == 1) it.first.first() else CommaNode(it.first, it.second) }
    val assignationOperator: Parser<List<TokenMatch>> by OrCombinator(
        +(equal * -NotParser(greater)),
        +(colon * -NotParser(equal or colon)),
        +(colon * equal),
        +(minus * greater),
        +(equal * greater),
    )
    val assignation: Parser<Node> by rightAssociative(commas, -s * assignationOperator * -b, ::infixNode)
    val stream: Parser<Node> by leftAssociative(assignation, -s * +pipe * -b, ::infixNode)

    val semicolonsPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        stream * -s * br * -b * cachedParser { semicolonsPart } map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        (stream * -s or (EmptyParser map { EmptyNode })) * semicolon * (-b * cachedParser { semicolonsPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        stream map { Pair(listOf(it), listOf()) },
    )
    val semicolons: Parser<Node> by semicolonsPart map { if (it.first.size == 1) it.first.first() else SemicolonNode(it.first, it.second) }
    val expression: Parser<Node> by semicolons

    override val rootParser: Parser<Node> by -b * expression * -b map { RootNode(it) }
}

@JvmName("unaryPlus1")
private operator fun <T> Parser<T>.unaryPlus() = this map { listOf(it) }

@JvmName("unaryPlus2")
private operator fun <T> Parser<Tuple2<T, T>>.unaryPlus() = this map { listOf(it.t1, it.t2) }

private fun bracketNode(it: Tuple3<TokenMatch, Node?, TokenMatch>) = BracketNode(it.t1, it.t2 ?: EmptyNode, it.t3)

private fun infixNode(left: Node, operator: List<TokenMatch>, right: Node) = InfixNode(left, operator, right)
