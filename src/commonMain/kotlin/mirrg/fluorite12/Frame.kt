package mirrg.fluorite12

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class Frame(val parent: Frame? = null) {
    val variables = mutableMapOf<String, Any?>()
}

suspend fun Frame.evaluate(node: Node): Any? {
    return when (node) {
        is IdentifierNode -> {
            val variable = node.main.text
            fun f(frame: Frame): Any? {
                return if (variable in frame.variables) {
                    frame.variables[variable]
                } else if (frame.parent != null) {
                    f(frame.parent)
                } else {
                    throw IllegalArgumentException("Unknown variable: $variable")
                }
            }
            f(this)
        }

        is NumberNode -> node.main.text.toInt()

        is StringNode -> node.string

        is BracketNode -> when (node.left.text) {
            "(" -> evaluate(node.main)

            "[" -> {
                val nodes = if (node.main is SemicolonNode) {
                    node.main.nodes
                } else {
                    listOf(node.main)
                }
                val values = mutableListOf<Any?>()
                nodes.forEach {
                    val value = evaluate(it)
                    if (value is FluoriteStream) {
                        value.flow.collect { item ->
                            values += item
                        }
                    } else {
                        values += value
                    }
                }
                FluoriteArray(values)
            }

            "{" -> {
                val nodes = if (node.main is SemicolonNode) {
                    node.main.nodes
                } else {
                    listOf(node.main)
                }
                val values = mutableListOf<Pair<String, Any?>>()
                nodes.forEach {
                    val value = evaluate(it)
                    if (value is FluoriteStream) {
                        value.flow.collect { item ->
                            require(item is FluoriteTuple)
                            require(item.values.size == 2)
                            values += Pair(item.values[0] as String, item.values[1])
                        }
                    } else {
                        require(value is FluoriteTuple)
                        require(value.values.size == 2)
                        values += Pair(value.values[0] as String, value.values[1])
                    }
                }
                FluoriteObject(values.toMap())
            }

            else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} A ${node.right.text}")
        }

        is RightBracketNode -> when (node.left.text) {
            "(" -> {
                val mainValue = evaluate(node.main)
                require(mainValue is FluoriteFunction)
                val argumentNodes = if (node.argument is SemicolonNode) {
                    node.argument.nodes
                } else {
                    listOf(node.argument)
                }
                val argumentValues = argumentNodes.map { evaluate(it) }
                mainValue.function(argumentValues)
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.left.text} B ${node.right.text}")
        }

        is InfixNode -> when (node.operator.text) {
            "+" -> (evaluate(node.left) as Number).toDouble() + (evaluate(node.right) as Number).toDouble()
            "-" -> (evaluate(node.left) as Number).toDouble() - (evaluate(node.right) as Number).toDouble()
            "*" -> (evaluate(node.left) as Number).toDouble() * (evaluate(node.right) as Number).toDouble()
            "/" -> (evaluate(node.left) as Number).toDouble() / (evaluate(node.right) as Number).toDouble()
            ".." -> FluoriteStream(((evaluate(node.left) as Number).toInt()..(evaluate(node.right) as Number).toInt()).asFlow())

            "->" -> {
                val commasNode = if (node.left is BracketNode && node.left.left.text == "(") {
                    node.left.main
                } else {
                    node.left
                }
                val identifierNodes = if (commasNode is ListNode && commasNode.operators.first().text == ",") {
                    commasNode.nodes
                } else if (commasNode is SemicolonNode) {
                    commasNode.nodes
                } else {
                    listOf(commasNode)
                }
                val variables = identifierNodes.map {
                    require(it is IdentifierNode)
                    it.main.text
                }
                FluoriteFunction { arguments ->
                    val frame = Frame(this)
                    frame.variables["__"] = FluoriteArray(arguments)
                    variables.forEachIndexed { i, it ->
                        frame.variables[it] = arguments.getOrNull(i)
                    }
                    frame.evaluate(node.right)
                }
            }

            "|" -> {
                val stream = evaluate(node.left)
                val (variable, body) = if (node.right is InfixNode && node.right.operator.text == "=>") {
                    require(node.right.left is IdentifierNode)
                    Pair(node.right.left.main.text, node.right.right)
                } else {
                    Pair("_", node.right)
                }
                suspend fun f(value: Any?): Any? {
                    val frame = Frame(this)
                    frame.variables[variable] = value
                    return frame.evaluate(body)
                }
                return if (stream is FluoriteStream) {
                    FluoriteStream(flow {
                        stream.flow.collect {
                            val value = f(it)
                            if (value is FluoriteStream) {
                                emitAll(value.flow)
                            } else {
                                emit(value)
                            }
                        }
                    })
                } else {
                    f(stream)
                }
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.operator.text} B")
        }

        is ComparisonNode -> run {
            val values = arrayOfNulls<Any?>(node.nodes.size)
            values[0] = evaluate(node.nodes[0])
            node.operators.forEachIndexed { i, operator ->
                val leftValue = values[i]
                val rightValue = evaluate(node.nodes[i + 1])
                values[i + 1] = rightValue
                val result = when (operator.text) {
                    "==" -> leftValue == rightValue
                    "!=" -> leftValue != rightValue
                    ">" -> (leftValue as Number).toDouble() > (rightValue as Number).toDouble()
                    "<" -> (leftValue as Number).toDouble() < (rightValue as Number).toDouble()
                    ">=" -> (leftValue as Number).toDouble() >= (rightValue as Number).toDouble()
                    "<=" -> (leftValue as Number).toDouble() <= (rightValue as Number).toDouble()
                    else -> throw IllegalArgumentException("Unknown operator: A ${operator.text} B")
                }
                if (!result) return@run false
            }
            true
        }

        is ConditionNode -> if (evaluate(node.condition) as Boolean) evaluate(node.ok) else evaluate(node.ng)

        is ListNode -> when (node.operators.first().text) {
            ":" -> FluoriteTuple(node.nodes.map { evaluate(it) })

            "," -> {
                node.nodes.map {
                    val value = evaluate(it)
                    if (value is FluoriteStream) value else streamOf(value)
                }.concat()
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.operators.first().text} B")
        }

        is SemicolonNode -> {
            node.nodes.dropLast(1).forEach {
                evaluate(it)
            }
            evaluate(node.nodes.last())
        }
    }
}
