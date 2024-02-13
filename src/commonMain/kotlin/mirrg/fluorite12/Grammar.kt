package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
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
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2
import com.github.h0tk3y.betterParse.utils.Tuple3
import com.github.h0tk3y.betterParse.utils.Tuple5
import kotlin.jvm.JvmName

@Suppress("MemberVisibilityCanBePrivate", "unused")
class Fluorite12Grammar : Grammar<Node>() {
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
    val lAlphabet by regexToken("""[A-Z]""".toRegex())
    val lSquare by literalToken("[")
    val bSlash by literalToken("\\")
    val rSquare by literalToken("]")
    val circumflex by regexToken("^")
    val underscore by regexToken("_")

    val bQuote by literalToken("`")
    val sAlphabet by regexToken("""[a-z]""".toRegex())
    val lCurly by literalToken("{")
    val pipe by literalToken("|")
    val rCurly by literalToken("}")
    val tilde by literalToken("~")


    val s by zeroOrMore(space or tab)
    val b by zeroOrMore(space or tab) * zeroOrMore(br * zeroOrMore(space or tab))

    val identifier: Parser<Node> by (lAlphabet or sAlphabet or underscore) * zeroOrMore(lAlphabet or sAlphabet or underscore or zero or nonZero) map {
        val tokens = listOf(it.t1, *it.t2.toTypedArray())
        IdentifierNode(tokens, tokens.joinToString("") { t -> t.text })
    }

    val integer: Parser<Node> by oneOrMore(zero or nonZero) map { NumberNode(it, it.joinToString("") { t -> t.text }) }

    val stringCharacter by OrCombinator(
        lAlphabet map { Pair(it, it.text) },
        sAlphabet map { Pair(it, it.text) },
        zero map { Pair(it, it.text) },
        nonZero map { Pair(it, it.text) },
    )
    val stringContent by OrCombinator(
        oneOrMore(stringCharacter) map { LiteralStringContent(it.map { t -> t.first }, it.joinToString("") { t -> t.second }) },
        dollar * parser { factor } map { NodeStringContent(it.t1, it.t2) },
    )
    val string by dQuote * zeroOrMore(stringContent) * dQuote map { StringNode(it.t1, it.t3, it.t2) }

    val round: Parser<Node> by lRound * -b * parser { expression } * -b * rRound map ::bracketNode
    val square: Parser<Node> by lSquare * -b * parser { expression } * -b * rSquare map ::bracketNode
    val curly: Parser<Node> by lCurly * -b * parser { expression } * -b * rCurly map ::bracketNode
    val factor: Parser<Node> by identifier or integer or string or round or square or curly

    val right: Parser<Node> by factor * zeroOrMore(
        -s * lRound * -b * parser { expression } * -b * rRound or
            -s * lSquare * -b * parser { expression } * -b * rSquare or
            -s * lCurly * -b * parser { expression } * -b * rCurly
    ) map ::rightBracketNode

    val mul: Parser<Node> by leftAssociative(right, -s * (+asterisk or +slash) * -b, ::infixNode)
    val add: Parser<Node> by leftAssociative(mul, -s * (+plus or +minus) * -b, ::infixNode)
    val range: Parser<Node> by leftAssociative(add, -s * +(period * period) * -b, ::infixNode)
    val comparison: Parser<Node> by range * zeroOrMore(-s * (+(equal * equal) or +(exclamation * equal) or +greater or +(greater * equal) or +less or +(less * equal)) * -b * range) map {
        if (it.t2.isNotEmpty()) {
            ComparisonNode(listOf(it.t1, *it.t2.map { t -> t.t2 }.toTypedArray()), it.t2.map { it.t1 })
        } else {
            it.t1
        }
    }
    val condition: Parser<Node> by (comparison * -s * question * -b * parser { condition } * -s * colon * -b * parser { condition } map ::conditionNode) or comparison

    val tuple: Parser<Node> by condition * zeroOrMore(-s * colon * -b * condition) map ::listNode
    val stream: Parser<Node> by tuple * zeroOrMore(-s * comma * -b * tuple) map ::listNode
    val lambda: Parser<Node> by rightAssociative(stream, -s * (+(minus * greater) or +(equal * greater)) * -b, ::infixNode)
    val query: Parser<Node> by leftAssociative(lambda, -s * +pipe * -b, ::infixNode)

    val lines: Parser<Node> by query * zeroOrMore(-s * (semicolon or br) * -b * query) map ::semicolonNode
    val expression: Parser<Node> by lines

    override val rootParser: Parser<Node> by -b * expression * -b
}

@JvmName("unaryPlus1")
private operator fun <T> Parser<T>.unaryPlus() = this map { listOf(it) }

@JvmName("unaryPlus2")
private operator fun <T> Parser<Tuple2<T, T>>.unaryPlus() = this map { listOf(it.t1, it.t2) }

private fun bracketNode(it: Tuple3<TokenMatch, Node, TokenMatch>) = BracketNode(it.t1, it.t2, it.t3)

private fun rightBracketNode(it: Tuple2<Node, List<Tuple3<TokenMatch, Node, TokenMatch>>>): Node {
    var main = it.t1
    it.t2.forEach { (left, argument, right) ->
        main = RightBracketNode(main, left, argument, right)
    }
    return main
}

private fun infixNode(left: Node, operator: List<TokenMatch>, right: Node) = InfixNode(left, operator, right)

private fun conditionNode(it: Tuple5<Node, TokenMatch, Node, TokenMatch, Node>) = ConditionNode(it.t1, it.t2, it.t3, it.t4, it.t5)

private fun listNode(it: Tuple2<Node, List<Tuple2<TokenMatch, Node>>>): Node {
    return if (it.t2.isNotEmpty()) {
        ListNode(listOf(it.t1) + it.t2.map { it.t2 }, it.t2.map { it.t1 })
    } else {
        it.t1
    }
}

private fun semicolonNode(it: Tuple2<Node, List<Tuple2<TokenMatch, Node>>>): Node {
    return if (it.t2.isNotEmpty()) {
        SemicolonNode(listOf(it.t1) + it.t2.map { it.t2 }, it.t2.map { it.t1 })
    } else {
        it.t1
    }
}
