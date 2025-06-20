package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
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

    val at by literalToken("@")
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


    val lineComment by (sharp or slash * slash) * zeroOrMore(-NotParser(br) * AnyParser)

    val blockCommentContent: Parser<List<TokenMatch>> by OrCombinator(
        cachedParser { blockComment },
        -NotParser(asterisk * slash) * AnyParser map { listOf(it) },
    )
    val blockComment: Parser<List<TokenMatch>> by slash * asterisk * zeroOrMore(blockCommentContent) * asterisk * slash map { listOf(it.t1, it.t2, *it.t3.flatten().toTypedArray(), it.t4) }

    val s by zeroOrMore(space or tab or lineComment or blockComment)
    val b by zeroOrMore(space or tab or lineComment or blockComment) * zeroOrMore(br * zeroOrMore(space or tab or lineComment or blockComment))
    val uAlphabet by uA or uB or uC or uD or uE or uF or uG or uH or uI or uJ or uK or uL or uM or uN or uO or uP or uQ or uR or uS or uT or uU or uV or uW or uX or uY or uZ
    val lAlphabet by lA or lB or lC or lD or lE or lF or lG or lH or lI or lJ or lK or lL or lM or lN or lO or lP or lQ or lR or lS or lT or lU or lV or lW or lX or lY or lZ
    val number by zero or nonZero
    val hexadecimalNumber by number or lA or uA or lB or uB or lC or uC or lD or uD or lE or uE or lF or uF

    val identifier: Parser<IdentifierNode> by (uAlphabet or lAlphabet or underscore or other) * zeroOrMore(uAlphabet or lAlphabet or underscore or other or number) map {
        IdentifierNode(
            listOf(),
            listOf(it.t1, *it.t2.toTypedArray()),
            listOf(it.t1, *it.t2.toTypedArray()).joinToString("") { t -> t.text },
            listOf(),
        )
    }

    val quotedIdentifierContent: Parser<Pair<List<TokenMatch>, String>> by OrCombinator(
        -NotParser(bQuote or bSlash or br) * AnyParser map { Pair(listOf(it), it.text) }, // ' \ 改行以外の文字
        br map { Pair(listOf(it), "\n") }, // 改行
        bSlash * lU * hexadecimalNumber * hexadecimalNumber * hexadecimalNumber * hexadecimalNumber map {
            val ch = "${it.t3.text}${it.t4.text}${it.t5.text}${it.t6.text}".toInt(16).toChar()
            Pair(listOf(it.t1, it.t2, it.t3, it.t4, it.t5, it.t6), ch.toString())
        }, // 文字参照
        bSlash * -NotParser(br) * AnyParser map { Pair(listOf(it.t1, it.t2), it.t2.text) }, // エスケープされた改行以外の文字
        bSlash * br map { Pair(listOf(it.t1, it.t2), "\n") }, // エスケープされた改行
    )
    val quotedIdentifier: Parser<IdentifierNode> by bQuote * zeroOrMore(quotedIdentifierContent) * bQuote map {
        IdentifierNode(
            listOf(it.t1),
            it.t2.flatMap { pair -> pair.first },
            it.t2.joinToString("") { pair -> pair.second },
            listOf(it.t3),
        )
    }

    val float: Parser<Node> by oneOrMore(number) * period * oneOrMore(number) map {
        val tokens = it.t1 + it.t2 + it.t3
        FloatNode(tokens, tokens.text)
    }

    val integer: Parser<Node> by oneOrMore(number) map { IntegerNode(it, it.joinToString("") { t -> t.text }) }

    val hexadecimal: Parser<Node> by uH * sharp * oneOrMore(hexadecimalNumber) map { HexadecimalNode(listOf(it.t1, it.t2, *it.t3.toTypedArray()), it.t3.joinToString("") { t -> t.text }) }

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
        bSlash * lU * hexadecimalNumber * hexadecimalNumber * hexadecimalNumber * hexadecimalNumber map {
            val ch = "${it.t3.text}${it.t4.text}${it.t5.text}${it.t6.text}".toInt(16).toChar()
            Pair(listOf(it.t1, it.t2, it.t3, it.t4, it.t5, it.t6), ch.toString())
        }, // 文字参照
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
        dollar * percent * zeroOrMore(formatterFlag) * zeroOrMore(number) * optional(period * oneOrMore(number)) * formatterConversion * cachedParser { brackets } map { parameters ->
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

    val arrowRound: Parser<Node> by lRound * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rRound map { BracketsLiteralArrowedRoundNode(it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) }
    val arrowSquare: Parser<Node> by lSquare * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rSquare map { BracketsLiteralArrowedSquareNode(it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) }
    val arrowCurly: Parser<Node> by lCurly * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rCurly map { BracketsLiteralArrowedCurlyNode(it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) }
    val round: Parser<Node> by lRound * -b * optional(cachedParser { expression } * -b) * rRound map { BracketsLiteralSimpleRoundNode(it.t1, it.t2 ?: EmptyNode, it.t3) }
    val square: Parser<Node> by lSquare * -b * optional(cachedParser { expression } * -b) * rSquare map { BracketsLiteralSimpleSquareNode(it.t1, it.t2 ?: EmptyNode, it.t3) }
    val curly: Parser<Node> by lCurly * -b * optional(cachedParser { expression } * -b) * rCurly map { BracketsLiteralSimpleCurlyNode(it.t1, it.t2 ?: EmptyNode, it.t3) }
    val brackets by arrowRound or arrowSquare or arrowCurly or round or square or curly
    val nonFloatFactor: Parser<Node> by hexadecimal or identifier or quotedIdentifier or integer or rawString or templateString or embeddedString or brackets
    val factor: Parser<Node> by hexadecimal or identifier or quotedIdentifier or float or integer or rawString or templateString or embeddedString or brackets

    val unaryOperator: Parser<(List<TokenMatch>, Node, Side) -> Node> by OrCombinator(
        +plus map { { prefix, main, side -> UnaryPlusNode(prefix + it, main, side) } },
        +minus map { { prefix, main, side -> UnaryMinusNode(prefix + it, main, side) } },
        +question map { { prefix, main, side -> UnaryQuestionNode(prefix + it, main, side) } },
        +(exclamation * exclamation) map { { prefix, main, side -> UnaryExclamationExclamationNode(prefix + it, main, side) } },
        +exclamation map { { prefix, main, side -> UnaryExclamationNode(prefix + it, main, side) } },
        +ampersand map { { prefix, main, side -> UnaryAmpersandNode(prefix + it, main, side) } },
        +(dollar * sharp) map { { prefix, main, side -> UnaryDollarSharpNode(prefix + it, main, side) } },
        +(dollar * ampersand) map { { prefix, main, side -> UnaryDollarAmpersandNode(prefix + it, main, side) } },
        +(dollar * asterisk) map { { prefix, main, side -> UnaryDollarAsteriskNode(prefix + it, main, side) } },
        +at map { { prefix, main, side -> UnaryAtNode(prefix + it, main, side) } },
    )
    val rightOperator: Parser<(Node) -> Node> by OrCombinator(
        -s * lRound * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rRound map { { main -> BracketsRightArrowedRoundNode(main, it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) } },
        -s * lSquare * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rSquare map { { main -> BracketsRightArrowedSquareNode(main, it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) } },
        -s * lCurly * -b * optional(cachedParser { commas } * -b) * +(equal * greater) * -b * optional(cachedParser { expression } * -b) * rCurly map { { main -> BracketsRightArrowedCurlyNode(main, it.t1, it.t2 ?: EmptyNode, it.t3, it.t4 ?: EmptyNode, it.t5) } },
        -s * lRound * -b * optional(cachedParser { expression } * -b) * rRound map { { main -> BracketsRightSimpleRoundNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lSquare * -b * optional(cachedParser { expression } * -b) * rSquare map { { main -> BracketsRightSimpleSquareNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },
        -s * lCurly * -b * optional(cachedParser { expression } * -b) * rCurly map { { main -> BracketsRightSimpleCurlyNode(main, it.t1, it.t2 ?: EmptyNode, it.t3) } },

        -b * +period * -b * nonFloatFactor map { { main -> InfixPeriodNode(main, it.t1, it.t2) } },
        -b * +(question * period) * -b * nonFloatFactor map { { main -> InfixQuestionPeriodNode(main, it.t1, it.t2) } },
        -b * +(colon * colon) * -b * nonFloatFactor map { { main -> InfixColonColonNode(main, it.t1, it.t2) } },
        -b * +(question * colon * colon) * -b * nonFloatFactor map { { main -> InfixQuestionColonColonNode(main, it.t1, it.t2) } },

        -b * +period * unaryOperator map { { main -> it.t2(it.t1, main, Side.RIGHT) } },
    )
    val right: Parser<Node> by factor * zeroOrMore(rightOperator) map { it.t2.fold(it.t1) { node, f -> f(node) } }
    val pow: Parser<Node> by right * optional(-s * +circumflex * -b * cachedParser { left }) map {
        val right = it.t2
        if (right != null) {
            InfixCircumflexNode(it.t1, right.t1, right.t2)
        } else {
            it.t1
        }
    }
    val left: Parser<Node> by zeroOrMore(unaryOperator * -b) * pow map { it.t1.foldRight(it.t2) { f, node -> f(listOf(), node, Side.LEFT) } }

    val mulOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +asterisk map { Pair(it, ::InfixAsteriskNode) },
        +slash map { Pair(it, ::InfixSlashNode) },
        +(percent * percent) map { Pair(it, ::InfixPercentPercentNode) },
        +percent map { Pair(it, ::InfixPercentNode) },
    )
    val mul: Parser<Node> by leftAssociative(left, -s * mulOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val addOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +plus map { Pair(it, ::InfixPlusNode) },
        +(minus * -NotParser(greater)) map { Pair(it, ::InfixMinusNode) },
        +(ampersand * -NotParser(ampersand)) map { Pair(it, ::InfixAmpersandNode) },
    )
    val add: Parser<Node> by leftAssociative(mul, -s * addOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val rangeOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +(period * period) map { Pair(it, ::InfixPeriodPeriodNode) },
        +tilde map { Pair(it, ::InfixTildeNode) },
    )
    val range: Parser<Node> by leftAssociative(add, -s * rangeOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val infixIdentifierOperator: Parser<(Node, Node) -> Node> by OrCombinator(
        identifier or quotedIdentifier map { { left, right -> InfixIdentifierNode(left, it, right) } },
        +exclamation * (identifier or quotedIdentifier) map { { left, right -> InfixExclamationIdentifierNode(left, it.t1, it.t2, right) } },
    )
    val infixIdentifier: Parser<Node> by leftAssociative(range, -s * infixIdentifierOperator * -b) { left, operator, right -> operator(left, right) }
    val spaceshipOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by +(less * equal * greater) map { Pair(it, ::InfixLessEqualGreaterNode) }
    val spaceship: Parser<Node> by leftAssociative(infixIdentifier, -s * spaceshipOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val comparisonOperator: Parser<Pair<List<TokenMatch>, ComparisonOperatorType>> by OrCombinator(
        +(equal * equal) map { Pair(it, ComparisonOperatorType.EQUAL) }, // ==
        +(exclamation * equal) map { Pair(it, ComparisonOperatorType.EXCLAMATION_EQUAL) }, // !=
        +(greater * equal) map { Pair(it, ComparisonOperatorType.GREATER_EQUAL) }, // >=
        +(greater * -NotParser(greater)) map { Pair(it, ComparisonOperatorType.GREATER) }, // >
        +(less * equal) map { Pair(it, ComparisonOperatorType.LESS_EQUAL) }, // <=
        +(less * -NotParser(less)) map { Pair(it, ComparisonOperatorType.LESS) }, // <
        +(question * equal) map { Pair(it, ComparisonOperatorType.QUESTION_EQUAL) }, // ?=
        +at map { Pair(it, ComparisonOperatorType.AT) }, // @
    )
    val comparison: Parser<Node> by spaceship * zeroOrMore(-s * comparisonOperator * -b * spaceship) map {
        if (it.t2.isNotEmpty()) {
            ComparisonsNode(listOf(it.t1, *it.t2.map { t -> t.t2 }.toTypedArray()), it.t2.map { t -> t.t1 })
        } else {
            it.t1
        }
    }
    val andOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by +(ampersand * ampersand) map { Pair(it, ::InfixAmpersandAmpersandNode) }
    val and: Parser<Node> by leftAssociative(comparison, -s * andOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val orOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by +(pipe * pipe) map { Pair(it, ::InfixPipePipeNode) }
    val or: Parser<Node> by leftAssociative(and, -s * orOperator * -b) { left, operator, right -> operator.second(left, operator.first, right) }
    val condition: Parser<Node> by OrCombinator(
        or * -b * question * -b * cachedParser { condition } * -b * (colon * -NotParser(colon)) * -b * cachedParser { condition } map { ConditionNode(it.t1, it.t2, it.t3, it.t4, it.t5) },
        or * -b * +(question * colon) * -b * cachedParser { condition } map { InfixQuestionColonNode(it.t1, it.t2, it.t3) },
        or * -b * +(exclamation * question) * -b * cachedParser { condition } map { InfixExclamationQuestionNode(it.t1, it.t2, it.t3) },
        or,
    )

    val commasPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        (condition * -b or (EmptyParser map { EmptyNode })) * comma * (-b * cachedParser { commasPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        condition map { Pair(listOf(it), listOf()) },
    )
    val commas: Parser<Node> by commasPart map { if (it.first.size == 1) it.first.first() else CommasNode(it.first, it.second) }

    val pipeOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +pipe map { Pair(it, ::InfixPipeNode) }, // |
    )
    val argumentsOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +(equal * greater) map { Pair(it, ::InfixEqualGreaterNode) }, // =>
    )
    val executionOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +(greater * greater) map { Pair(it, ::InfixGreaterGreaterNode) }, // >>
    )
    val assignmentOperator: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by OrCombinator(
        +(equal * -NotParser(greater)) map { Pair(it, ::InfixEqualNode) }, // =
        +(colon * -NotParser(equal or colon)) map { Pair(it, ::InfixColonNode) }, // :
        +(colon * equal) map { Pair(it, ::InfixColonEqualNode) }, // :=
        +(less * less) map { Pair(it, ::InfixLessLessNode) }, // <<
        +(minus * greater) map { Pair(it, ::InfixMinusGreaterNode) }, // ->
    )

    val pipeOperatorPart: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by -b * pipeOperator * -b or -s * argumentsOperator * -b
    val executionOperatorPart: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by -b * executionOperator * -b
    val assignmentOperatorPart: Parser<Pair<List<TokenMatch>, (Node, List<TokenMatch>, Node) -> InfixNode>> by -s * assignmentOperator * -b

    val pipeRight: Parser<Node> by OrCombinator(
        commas * pipeOperatorPart * cachedParser { pipeRight } map { it.t2.second(it.t1, it.t2.first, it.t3) },
        commas * assignmentOperatorPart * cachedParser { stream } map { it.t2.second(it.t1, it.t2.first, it.t3) },
        commas,
    )
    val executionRight: Parser<Node> by OrCombinator(
        commas * assignmentOperatorPart * cachedParser { stream } map { it.t2.second(it.t1, it.t2.first, it.t3) },
        commas,
    )
    val streamRightPart: Parser<(Node) -> Node> by OrCombinator(
        pipeOperatorPart * pipeRight map { { left -> it.t1.second(left, it.t1.first, it.t2) } },
        executionOperatorPart * executionRight map { { left -> it.t1.second(left, it.t1.first, it.t2) } },
    )
    val stream: Parser<Node> by OrCombinator(
        commas * assignmentOperatorPart * cachedParser { stream } map { it.t2.second(it.t1, it.t2.first, it.t3) },
        commas * zeroOrMore(streamRightPart) map { it.t2.fold(it.t1) { left, part -> part(left) } },
    )

    val semicolonsPart: Parser<Pair<List<Node>, List<TokenMatch>>> by OrCombinator(
        stream * -s * br * -b * cachedParser { semicolonsPart } map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        (stream * -s or (EmptyParser map { EmptyNode })) * semicolon * (-b * cachedParser { semicolonsPart } or (EmptyParser map { Pair(listOf(EmptyNode), listOf()) })) map { Pair(listOf(it.t1) + it.t3.first, listOf(it.t2) + it.t3.second) },
        stream map { Pair(listOf(it), listOf()) },
    )
    val semicolons: Parser<Node> by semicolonsPart map { if (it.first.size == 1) it.first.first() else SemicolonsNode(it.first, it.second) }
    val expression: Parser<Node> by semicolons

    override val rootParser: Parser<Node> by -b * optional(expression * -b) map { it ?: EmptyNode }
}

@JvmName("unaryPlus1")
private operator fun Parser<TokenMatch>.unaryPlus() = this map { listOf(it) }

@JvmName("unaryPlus2")
private operator fun Parser<Tuple2<TokenMatch, TokenMatch>>.unaryPlus() = this map { listOf(it.t1, it.t2) }

@JvmName("unaryPlus3")
private operator fun Parser<Tuple3<TokenMatch, TokenMatch, TokenMatch>>.unaryPlus() = this map { listOf(it.t1, it.t2, it.t3) }
