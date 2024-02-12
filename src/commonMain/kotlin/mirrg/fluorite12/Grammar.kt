package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
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
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2
import com.github.h0tk3y.betterParse.utils.Tuple3
import com.github.h0tk3y.betterParse.utils.Tuple5

@Suppress("MemberVisibilityCanBePrivate")
class Fluorite12Grammar : Grammar<Node>() {
    val space by regexToken("""[ \t]+""".toRegex())
    val lineBreak by regexToken("""\n|\r\n?""".toRegex())
    val identifier by regexToken("""[a-zA-Z_][a-zA-Z_0-9]*""".toRegex())
    val integer by regexToken("""[0-9]+""".toRegex())
    val leftRound by literalToken("(")
    val rightRound by literalToken(")")
    val leftSquare by literalToken("[")
    val rightSquare by literalToken("]")
    val asterisk by literalToken("*")
    val slash by literalToken("/")
    val plus by literalToken("+")
    val minus by regexToken("""-(?!>)""".toRegex())
    val periodPeriod by literalToken("..")
    val equalEqual by literalToken("==")
    val exclamationEqual by literalToken("!=")
    val greater by regexToken(""">(?!=)""".toRegex())
    val greaterEqual by regexToken("""<(?!=)""".toRegex())
    val less by literalToken(">=")
    val lessEqual by literalToken("<=")
    val question by literalToken("?")
    val colon by literalToken(":")
    val comma by literalToken(",")
    val minusGreater by literalToken("->")
    val equalGreater by literalToken("=>")
    val pipe by literalToken("|")
    val semicolon by literalToken(";")

    val s by optional(space)
    val b by optional(space) * zeroOrMore(lineBreak * optional(space))


    val identifierLiteral: Parser<Node> by identifier map { IdentifierNode(it) }
    val integerLiteral: Parser<Node> by integer map { NumberNode(it) }
    val number: Parser<Node> by integerLiteral
    val round: Parser<Node> by leftRound * -b * parser { expression } * -b * rightRound map ::bracketNode
    val square: Parser<Node> by leftSquare * -b * parser { expression } * -b * rightSquare map ::bracketNode
    val factor: Parser<Node> by identifierLiteral or number or round or square

    val call: Parser<Node> by factor * zeroOrMore(-s * leftRound * -b * parser { expression } * -b * rightRound or -s * leftSquare * -b * parser { expression } * -b * rightSquare) map ::rightBracketNode

    val mul: Parser<Node> by leftAssociative(call, -s * (asterisk or slash) * -b, ::infixNode)
    val add: Parser<Node> by leftAssociative(mul, -s * (plus or minus) * -b, ::infixNode)
    val range: Parser<Node> by leftAssociative(add, -s * periodPeriod * -b, ::infixNode)
    val comparison: Parser<Node> by range * zeroOrMore(-s * (equalEqual or exclamationEqual or greater or greaterEqual or less or lessEqual) * -b * range) map ::comparisonNode
    val condition: Parser<Node> by (comparison * -s * question * -b * parser { condition } * -s * colon * -b * parser { condition } map ::conditionNode) or comparison

    val stream: Parser<Node> by condition * zeroOrMore(-s * comma * -b * condition) map ::listNode
    val lambda: Parser<Node> by rightAssociative(stream, -s * (minusGreater or equalGreater) * -b, ::infixNode)
    val query: Parser<Node> by leftAssociative(lambda, -s * pipe * -b, ::infixNode)

    val lines: Parser<Node> by query * zeroOrMore(-s * (semicolon or lineBreak) * -b * query) map ::listNode
    val expression: Parser<Node> by lines

    override val rootParser: Parser<Node> by -b * expression * -b
}

private fun bracketNode(it: Tuple3<TokenMatch, Node, TokenMatch>) = BracketNode(it.t1, it.t2, it.t3)

private fun rightBracketNode(it: Tuple2<Node, List<Tuple3<TokenMatch, Node, TokenMatch>>>): Node {
    var main = it.t1
    it.t2.forEach { (left, argument, right) ->
        main = RightBracketNode(main, left, argument, right)
    }
    return main
}

private fun infixNode(left: Node, operator: TokenMatch, right: Node) = InfixNode(left, operator, right)

private fun comparisonNode(it: Tuple2<Node, List<Tuple2<TokenMatch, Node>>>): Node {
    return if (it.t2.isNotEmpty()) {
        ComparisonNode(listOf(it.t1) + it.t2.map { it.t2 }, it.t2.map { it.t1 })
    } else {
        it.t1
    }
}

private fun conditionNode(it: Tuple5<Node, TokenMatch, Node, TokenMatch, Node>) = ConditionNode(it.t1, it.t2, it.t3, it.t4, it.t5)

private fun listNode(it: Tuple2<Node, List<Tuple2<TokenMatch, Node>>>): Node {
    return if (it.t2.isNotEmpty()) {
        ListNode(listOf(it.t1) + it.t2.map { it.t2 }, it.t2.map { it.t1 })
    } else {
        it.t1
    }
}
