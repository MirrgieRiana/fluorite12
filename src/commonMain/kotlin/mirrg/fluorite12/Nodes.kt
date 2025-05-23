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
sealed class ArrowBracketsNode(val left: TokenMatch, val arguments: Node, val arrow: List<TokenMatch>, val body: Node, val right: TokenMatch) : Node()
class ArrowBracketsRoundNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : ArrowBracketsNode(left, arguments, arrow, body, right)
class ArrowBracketsSquareNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : ArrowBracketsNode(left, arguments, arrow, body, right)
class ArrowBracketsCurlyNode(left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : ArrowBracketsNode(left, arguments, arrow, body, right)
sealed class BracketsNode(val left: TokenMatch, val main: Node, val right: TokenMatch) : Node()
class BracketsRoundNode(left: TokenMatch, main: Node, right: TokenMatch) : BracketsNode(left, main, right)
class BracketsSquareNode(left: TokenMatch, main: Node, right: TokenMatch) : BracketsNode(left, main, right)
class BracketsCurlyNode(left: TokenMatch, main: Node, right: TokenMatch) : BracketsNode(left, main, right)
sealed class RightArrowBracketsNode(val main: Node, val left: TokenMatch, val arguments: Node, val arrow: List<TokenMatch>, val body: Node, val right: TokenMatch) : Node()
class RightArrowBracketsRoundNode(main: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : RightArrowBracketsNode(main, left, arguments, arrow, body, right)
class RightArrowBracketsSquareNode(main: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : RightArrowBracketsNode(main, left, arguments, arrow, body, right)
class RightArrowBracketsCurlyNode(main: Node, left: TokenMatch, arguments: Node, arrow: List<TokenMatch>, body: Node, right: TokenMatch) : RightArrowBracketsNode(main, left, arguments, arrow, body, right)
sealed class RightBracketsNode(val main: Node, val left: TokenMatch, val argument: Node, val right: TokenMatch) : Node()
class RightBracketsRoundNode(main: Node, left: TokenMatch, argument: Node, right: TokenMatch) : RightBracketsNode(main, left, argument, right)
class RightBracketsSquareNode(main: Node, left: TokenMatch, argument: Node, right: TokenMatch) : RightBracketsNode(main, left, argument, right)
class RightBracketsCurlyNode(main: Node, left: TokenMatch, argument: Node, right: TokenMatch) : RightBracketsNode(main, left, argument, right)
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
class InfixColonColonNode(left: Node, operator: List<TokenMatch>, right: Node) : InfixNode(left, operator, right)
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
