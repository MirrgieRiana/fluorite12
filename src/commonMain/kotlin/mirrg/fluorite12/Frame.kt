package mirrg.fluorite12

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class Frame(val parent: Frame? = null) {
    val variables = mutableMapOf<String, FluoriteValue>()

    init {
        if (parent == null) {
            variables["NULL"] = FluoriteNull
            variables["TRUE"] = FluoriteBoolean.TRUE
            variables["FALSE"] = FluoriteBoolean.FALSE
        }
    }
}

suspend fun Frame.evaluate(node: Node): FluoriteValue {
    return when (node) {
        is IdentifierNode -> {
            val variable = node.string
            fun f(frame: Frame): FluoriteValue {
                return frame.variables[variable] ?: if (frame.parent != null) {
                    f(frame.parent)
                } else {
                    throw IllegalArgumentException("Unknown variable: $variable")
                }
            }
            f(this)
        }

        is IntegerNode -> FluoriteInt(node.string.toInt())

        is FloatNode -> FluoriteDouble(node.string.toDouble())

        is RawStringNode -> FluoriteString(node.node.string)

        is TemplateStringNode -> {
            val sb = StringBuilder()
            node.nodes.forEach {
                when (it) {
                    is LiteralStringContent -> sb.append(it.string)
                    is NodeStringContent -> sb.append("${evaluate(it.main)}")
                }
            }
            FluoriteString("$sb")
        }

        is EmbeddedStringNode -> {
            val sb = StringBuilder()
            node.nodes.forEach {
                when (it) {
                    is LiteralStringContent -> sb.append(it.string)
                    is NodeStringContent -> sb.append("${evaluate(it.main)}")
                }
            }
            FluoriteString("$sb")
        }

        is BracketNode -> when (node.left.text) {
            "(" -> evaluate(node.main)

            "[" -> {
                val nodes = if (node.main is SemicolonNode) {
                    node.main.nodes
                } else {
                    listOf(node.main)
                }
                val values = mutableListOf<FluoriteValue>()
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
                val values = mutableListOf<Pair<String, FluoriteValue>>()
                nodes.forEach {
                    val value = evaluate(it)
                    if (value is FluoriteStream) {
                        value.flow.collect { item ->
                            require(item is FluoriteArray)
                            require(item.values.size == 2)
                            values += Pair(item.values[0].toString(), item.values[1])
                        }
                    } else {
                        require(value is FluoriteArray)
                        require(value.values.size == 2)
                        values += Pair(value.values[0].toString(), value.values[1])
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

        is LeftNode -> {
            fun FluoriteValue.toNumber() = when (this) {
                is FluoriteInt -> this
                is FluoriteDouble -> this
                is FluoriteString -> if ("." in this.value) FluoriteDouble(this.value.toDouble()) else FluoriteInt(this.value.toInt())
                is FluoriteBoolean -> FluoriteInt(if (this.value) 1 else 0)
                else -> throw IllegalArgumentException("Can not convert to number: $this")
            }

            fun FluoriteValue.toBoolean() = when (this) {
                is FluoriteInt -> FluoriteBoolean.of(this.value != 0)
                is FluoriteDouble -> FluoriteBoolean.of(this.value != 0.0)
                is FluoriteString -> FluoriteBoolean.of(this.value != "")
                is FluoriteBoolean -> this
                else -> throw IllegalArgumentException("Can not convert to boolean: $this")
            }

            fun FluoriteValue.toFluoriteString() = FluoriteString("$this")
            fun FluoriteValue.getLength() = when (this) {
                is FluoriteString -> FluoriteInt(this.value.length)
                is FluoriteArray -> FluoriteInt(this.values.size)
                is FluoriteObject -> FluoriteInt(this.map.size)
                else -> throw IllegalArgumentException("Can not calculate length: $this")
            }
            when (node.left.text) {
                "+" -> evaluate(node.right).toNumber()
                "-" -> evaluate(node.right).toNumber().negate()
                "?" -> evaluate(node.right).toBoolean()
                "!" -> evaluate(node.right).toBoolean().not()
                "&" -> evaluate(node.right).toFluoriteString()
                "$#" -> evaluate(node.right).getLength()
                else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} B")
            }
        }

        is InfixNode -> when (node.operator.text) {
            "+" -> when (val left = evaluate(node.left)) {
                is FluoriteInt -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteInt(left.value + right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                is FluoriteDouble -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteDouble(left.value + right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
            }

            "-" -> when (val left = evaluate(node.left)) {
                is FluoriteInt -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteInt(left.value - right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value - right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                is FluoriteDouble -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteDouble(left.value - right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value - right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
            }

            "*" -> when (val left = evaluate(node.left)) {
                is FluoriteInt -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteInt(left.value * right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value * right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                is FluoriteDouble -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteDouble(left.value * right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value * right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
            }

            "/" -> when (val left = evaluate(node.left)) {
                is FluoriteInt -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteInt(left.value / right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value / right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                is FluoriteDouble -> when (val right = evaluate(node.right)) {
                    is FluoriteInt -> FluoriteDouble(left.value / right.value)
                    is FluoriteDouble -> FluoriteDouble(left.value / right.value)
                    else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
            }

            ".." -> FluoriteStream(((evaluate(node.left) as FluoriteInt).value..(evaluate(node.right) as FluoriteInt).value).asFlow().map { FluoriteInt(it) })

            ":" -> {
                val key = when (node.left) {
                    is IdentifierNode -> FluoriteString(node.left.string)
                    else -> evaluate(node.left)
                }
                FluoriteArray(listOf(key, evaluate(node.right)))
            }

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
                    it.string
                }
                FluoriteFunction { arguments ->
                    val frame = Frame(this)
                    frame.variables["__"] = FluoriteArray(arguments)
                    variables.forEachIndexed { i, it ->
                        frame.variables[it] = arguments.getOrNull(i) ?: FluoriteNull
                    }
                    frame.evaluate(node.right)
                }
            }

            "|" -> {
                val stream = evaluate(node.left)
                val (variable, body) = if (node.right is InfixNode && node.right.operator.text == "=>") {
                    require(node.right.left is IdentifierNode)
                    Pair(node.right.left.string, node.right.right)
                } else {
                    Pair("_", node.right)
                }
                suspend fun f(value: FluoriteValue): FluoriteValue {
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
            val values = arrayOfNulls<FluoriteValue>(node.nodes.size)
            values[0] = evaluate(node.nodes[0])
            node.operators.forEachIndexed { i, operator ->
                val leftValue = values[i]
                val rightValue = evaluate(node.nodes[i + 1])
                values[i + 1] = rightValue
                val result = when (operator.text) {
                    "==" -> leftValue == rightValue
                    "!=" -> leftValue != rightValue
                    ">" -> (leftValue as FluoriteNumber).value.toDouble() > (rightValue as FluoriteNumber).value.toDouble()
                    "<" -> (leftValue as FluoriteNumber).value.toDouble() < (rightValue as FluoriteNumber).value.toDouble()
                    ">=" -> (leftValue as FluoriteNumber).value.toDouble() >= (rightValue as FluoriteNumber).value.toDouble()
                    "<=" -> (leftValue as FluoriteNumber).value.toDouble() <= (rightValue as FluoriteNumber).value.toDouble()
                    else -> throw IllegalArgumentException("Unknown operator: A ${operator.text} B")
                }
                if (!result) return@run FluoriteBoolean.FALSE
            }
            FluoriteBoolean.TRUE
        }

        is ConditionNode -> if ((evaluate(node.condition) as FluoriteBoolean).value) evaluate(node.ok) else evaluate(node.ng)

        is ListNode -> when (node.operators.first().text) {
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
