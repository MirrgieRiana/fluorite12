package mirrg.fluorite12

import com.github.h0tk3y.betterParse.lexer.TokenMatch

sealed class Node
class IdentifierNode(val main: TokenMatch) : Node()
class NumberNode(val main: TokenMatch) : Node()
class BracketNode(val left: TokenMatch, val main: Node, val right: TokenMatch) : Node()
class RightBracketNode(val main: Node, val left: TokenMatch, val argument: Node, val right: TokenMatch) : Node()
class InfixNode(val left: Node, val operator: TokenMatch, val right: Node) : Node()
class ComparisonNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class ConditionNode(val condition: Node, val question: TokenMatch, val ok: Node, val colon: TokenMatch, val ng: Node) : Node()
class ListNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
class SemicolonNode(val nodes: List<Node>, val operators: List<TokenMatch>) : Node()
