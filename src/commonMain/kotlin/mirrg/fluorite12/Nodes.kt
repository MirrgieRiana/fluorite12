package mirrg.fluorite12

import com.github.h0tk3y.betterParse.lexer.TokenMatch

sealed class Node
object EmptyNode : Node()
class IdentifierNode(val tokens: List<TokenMatch>, val string: String) : Node()
open class NumberNode(val tokens: List<TokenMatch>, val string: String) : Node()
class IntegerNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class HexadecimalNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class FloatNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class RawStringNode(val left: TokenMatch, val node: LiteralStringContent, val right: TokenMatch) : Node()
class TemplateStringNode(val left: TokenMatch, val stringContents: List<StringContent>, val right: TokenMatch) : Node()
class EmbeddedStringNode(val left: List<TokenMatch>, val stringContents: List<StringContent>, val right: List<TokenMatch>) : Node()
class BracketNode(val left: TokenMatch, val main: Node, val right: TokenMatch) : Node()
class RightBracketNode(val main: Node, val left: TokenMatch, val argument: Node, val right: TokenMatch) : Node()
class RightNode(val left: Node, val right: List<TokenMatch>) : Node()
class LeftNode(val left: List<TokenMatch>, val right: Node) : Node()
class InfixNode(val left: Node, val operator: List<TokenMatch>, val right: Node) : Node()
class ComparisonNode(val nodes: List<Node>, val operators: List<List<TokenMatch>>) : Node()
class ConditionNode(val condition: Node, val question: TokenMatch, val ok: Node, val colon: TokenMatch, val ng: Node) : Node()
class CommaNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class SemicolonNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class RootNode(val main: Node) : Node()

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
