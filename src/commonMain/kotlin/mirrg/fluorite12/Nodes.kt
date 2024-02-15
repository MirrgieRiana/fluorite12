package mirrg.fluorite12

import com.github.h0tk3y.betterParse.lexer.TokenMatch

sealed class Node
class IdentifierNode(val tokens: List<TokenMatch>, val string: String) : Node()
sealed class NumberNode(val tokens: List<TokenMatch>, val string: String) : Node()
class IntegerNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class FloatNode(tokens: List<TokenMatch>, string: String) : NumberNode(tokens, string)
class RawStringNode(val left: TokenMatch, val right: TokenMatch, val node: LiteralStringContent) : Node()
class TemplateStringNode(val left: TokenMatch, val right: TokenMatch, val nodes: List<StringContent>) : Node()
class BracketNode(val left: TokenMatch, val main: Node, val right: TokenMatch) : Node()
class RightBracketNode(val main: Node, val left: TokenMatch, val argument: Node, val right: TokenMatch) : Node()
class LeftNode(val left: List<TokenMatch>, val right: Node) : Node()
class InfixNode(val left: Node, val operator: List<TokenMatch>, val right: Node) : Node()
class ComparisonNode(val nodes: List<Node>, val operators: List<List<TokenMatch>>) : Node()
class ConditionNode(val condition: Node, val question: TokenMatch, val ok: Node, val colon: TokenMatch, val ng: Node) : Node()
class ListNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class SemicolonNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()

sealed class StringContent
class LiteralStringContent(val tokens: List<TokenMatch>, val string: String) : StringContent()
class NodeStringContent(val token: TokenMatch, val main: Node) : StringContent()
