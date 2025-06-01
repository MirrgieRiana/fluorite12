package mirrg.fluorite12

import com.github.h0tk3y.betterParse.lexer.TokenMatch

sealed class Node
object EmptyNode : Node()
class IdentifierNode(val left: List<TokenMatch>, val tokens: List<TokenMatch>, val string: String, val right: List<TokenMatch>) : Node()
sealed class NumberNode(val tokens: List<TokenMatch>, val string: String) : Node()
class IntegerNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class HexadecimalNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class FloatNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class RawStringNode(val left: TokenMatch, val node: LiteralStringContent, val right: TokenMatch) : Node()
class TemplateStringNode(val left: TokenMatch, val stringContents: List<StringContent>, val right: TokenMatch) : Node()
class EmbeddedStringNode(val left: List<TokenMatch>, val stringContents: List<StringContent>, val right: List<TokenMatch>) : Node()
sealed class BracketsNode(val left: TokenMatch, val body: Node, val right: TokenMatch) : Node()
sealed class BracketsLiteralNode(left: TokenMatch, body: Node, right: TokenMatch) : BracketsNode(left, body, right)
sealed class BracketsLiteralArrowedNode(left: TokenMatch, val arguments: Node, val arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsLiteralNode(left, body, right)
class BracketsLiteralArrowedRoundNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsLiteralArrowedNode(left, arguments, arrow, body, right)
class BracketsLiteralArrowedSquareNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsLiteralArrowedNode(left, arguments, arrow, body, right)
class BracketsLiteralArrowedCurlyNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsLiteralArrowedNode(left, arguments, arrow, body, right)
sealed class BracketsLiteralSimpleNode(left: TokenMatch, body: Node, right: TokenMatch) : BracketsLiteralNode(left, body, right)
class BracketsLiteralSimpleRoundNode(left: TokenMatch, body: Node, right: TokenMatch) : BracketsLiteralSimpleNode(left, body, right)
class BracketsLiteralSimpleSquareNode(left: TokenMatch, body: Node, right: TokenMatch) : BracketsLiteralSimpleNode(left, body, right)
class BracketsLiteralSimpleCurlyNode(left: TokenMatch, body: Node, right: TokenMatch) : BracketsLiteralSimpleNode(left, body, right)
sealed class BracketsRightNode(val receiver: Node, left: TokenMatch, body: Node, right: TokenMatch) : BracketsNode(left, body, right)
sealed class BracketsRightArrowedNode(receiver: Node, left: TokenMatch, val arguments: Node, val arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsRightNode(receiver, left, body, right)
class BracketsRightArrowedRoundNode(receiver: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsRightArrowedNode(receiver, left, arguments, arrow, body, right)
class BracketsRightArrowedSquareNode(receiver: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsRightArrowedNode(receiver, left, arguments, arrow, body, right)
class BracketsRightArrowedCurlyNode(receiver: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : BracketsRightArrowedNode(receiver, left, arguments, arrow, body, right)
sealed class BracketsRightSimpleNode(receiver: Node, left: TokenMatch, body: Node, right: TokenMatch) : BracketsRightNode(receiver, left, body, right)
class BracketsRightSimpleRoundNode(receiver: Node, left: TokenMatch, body: Node, right: TokenMatch) : BracketsRightSimpleNode(receiver, left, body, right)
class BracketsRightSimpleSquareNode(receiver: Node, left: TokenMatch, body: Node, right: TokenMatch) : BracketsRightSimpleNode(receiver, left, body, right)
class BracketsRightSimpleCurlyNode(receiver: Node, left: TokenMatch, body: Node, right: TokenMatch) : BracketsRightSimpleNode(receiver, left, body, right)
sealed class UnaryNode(val operator: List<TokenMatch>, val main: Node, val side: Side) : Node()
class UnaryPlusNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryMinusNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryQuestionNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryExclamationNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryAmpersandNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryDollarSharpNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryDollarAmpersandNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryDollarAsteriskNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryAtNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
class UnaryExclamationExclamationNode(operator: List<TokenMatch>, main: Node, side: Side) : UnaryNode(operator, main, side)
sealed class InfixNode(val left: Node, val operator: List<TokenMatch>, val right: Node) : Node()
class InfixPeriodNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixQuestionPeriodNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixColonColonNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixQuestionColonColonNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPlusNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixAmpersandNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixMinusNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixAsteriskNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixSlashNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPercentPercentNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPercentNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixCircumflexNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPeriodPeriodNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixLessEqualGreaterNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixTildeNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixAmpersandAmpersandNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPipePipeNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixQuestionColonNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixExclamationQuestionNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixColonNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixEqualNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixMinusGreaterNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixPipeNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixGreaterGreaterNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixLessLessNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixColonEqualNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class InfixEqualGreaterNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
class ComparisonsNode(val nodes: List<Node>, val operators: List<Pair<List<TokenMatch>, ComparisonOperatorType>>) : Node()
class ConditionNode(val condition: Node, val question: TokenMatch, val ok: Node, val colon: TokenMatch, val ng: Node) : Node()
class CommasNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class SemicolonsNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()

enum class Side {
    LEFT,
    RIGHT,
}

enum class ComparisonOperatorType {
    EQUAL,
    EXCLAMATION_EQUAL,
    GREATER,
    LESS,
    GREATER_EQUAL,
    LESS_EQUAL,
    QUESTION_EQUAL,
    AT,
}

sealed class StringContent
class LiteralStringContent(val tokens: List<TokenMatch>, val string: String) : StringContent()
class NodeStringContent(val left: List<TokenMatch>, val main: Node, val right: List<TokenMatch>) : StringContent()
class FormattedStringContent(val left: List<TokenMatch>, val formatter: Formatter, val main: Node) : StringContent()

class Formatter(val string: String, val flags: Set<FormatterFlag>, val width: Int?, val precision: Int?, val conversion: FormatterConversion)

enum class FormatterFlag {
    LEFT_ALIGNED,
    SIGNED,
    SPACE_FOR_SIGN,
    LEADING_ZEROS,
}

enum class FormatterConversion {
    DECIMAL,
    HEXADECIMAL,
    FLOAT,
    STRING,
}
