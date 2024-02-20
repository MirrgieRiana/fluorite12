package mirrg.fluorite12

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class Frame(val parent: Frame? = null) {
    val variables = mutableMapOf<String, Variable>()
    val overrides = mutableMapOf<Signature, FluoriteValue>()
}

fun Frame.defineCommonBuiltinVariables() {
    variables["NULL"] = Variable(false, FluoriteNull)
    variables["TRUE"] = Variable(false, FluoriteBoolean.TRUE)
    variables["FALSE"] = Variable(false, FluoriteBoolean.FALSE)
}

class Variable(val writable: Boolean, defaultValue: FluoriteValue) {
    var value: FluoriteValue = defaultValue
        set(value) {
            if (!writable) throw RuntimeException("Illegal assignment to constant")
            field = value
        }
}

class Signature(val fluoriteClass: FluoriteObject, val name: String) {
    override fun toString() = "Signature[$fluoriteClass.$name]"
    override fun equals(other: Any?) = other is Signature && other.fluoriteClass === this.fluoriteClass && other.name == this.name
    override fun hashCode() = 31 * fluoriteClass.hashCode() + name.hashCode()
}

fun Frame.getVariable(name: String): Variable? {
    var currentFrame = this
    while (true) {
        val variable = currentFrame.variables[name]
        if (variable != null) return variable
        currentFrame = currentFrame.parent ?: return null
    }
}

fun Frame.getOverride(signature: Signature): FluoriteValue? {
    var currentFrame = this
    while (true) {
        val override = currentFrame.overrides[signature]
        if (override != null) return override
        currentFrame = currentFrame.parent ?: return null
    }
}

private suspend fun Frame.evaluateExecution(node: Node): FluoriteValue {
    return when {
        node is InfixNode && node.operator.text == "=" -> when { // 代入文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variable = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
                variable.value = evaluate(node.right)
                variable.value
            }

            else -> throw IllegalArgumentException("Illegal assignation: ${node.left::class} = ${node.right::class}")
        }

        node is InfixNode && node.operator.text == ":=" -> when { // 宣言文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variable = Variable(true, FluoriteNull)
                variables[name] = variable
                variable.value = evaluate(node.right)
                variable.value
            }

            else -> throw IllegalArgumentException("Illegal definition: ${node.left::class} := ${node.right::class}")
        }

        else -> evaluate(node) // 式文
    }
}

private suspend fun Frame.evaluateRootNode(node: Node): FluoriteValue {
    val executionNodeList = if (node is SemicolonNode) node.nodes else listOf(node)
    var result: FluoriteValue = FluoriteNull
    executionNodeList.forEachIndexed { index, executionNode ->
        val lastResult = evaluateExecution(executionNode)
        if (index == executionNodeList.size - 1) result = lastResult
    }
    return result
}

suspend fun Frame.evaluate(node: Node): FluoriteValue {
    return when (node) {
        is EmptyNode -> throw IllegalArgumentException("Unexpected empty")

        is IdentifierNode -> {
            val name = node.string
            val variable = getVariable(name) ?: throw IllegalArgumentException("Unknown variable: $name")
            variable.value
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
            "(" -> {
                val frame = Frame(this)
                frame.evaluateRootNode(node.main)
            }

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
                val values = mutableMapOf<String, FluoriteValue>()
                nodes.forEach {
                    val value = evaluate(it)
                    if (value is FluoriteStream) {
                        value.flow.collect { item ->
                            require(item is FluoriteArray)
                            require(item.values.size == 2)
                            values[item.values[0].toString()] = item.values[1]
                        }
                    } else {
                        require(value is FluoriteArray)
                        require(value.values.size == 2)
                        values[value.values[0].toString()] = value.values[1]
                    }
                }
                FluoriteObject(FluoriteObject.fluoriteClass, values)
            }

            else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} A ${node.right.text}")
        }

        is RightBracketNode -> when (node.left.text) {
            "(" -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiver = evaluate(node.main.left)
                    val name = node.main.right.string
                    val method = getMethod(receiver, name) ?: throw IllegalArgumentException("No such method: ${node.main.left}::$name")
                    if (method !is FluoriteFunction) throw IllegalArgumentException("Can not call: ${node.main.left}::$name")
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentValues = argumentNodes.map { evaluate(it) }
                    method.function(listOf(receiver) + argumentValues)
                } else { // 関数呼び出し
                    val mainValue = evaluate(node.main)
                    require(mainValue is FluoriteFunction)
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentValues = argumentNodes.map { evaluate(it) }
                    mainValue.function(argumentValues)
                }
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

            suspend fun FluoriteValue.toFluoriteString(): FluoriteValue {
                return when (val method = getMethod(this, "TO_STRING")) {
                    null -> throw IllegalArgumentException("No such method: $this.TO_STRING")
                    is FluoriteFunction -> method.function(listOf(this))
                    else -> throw IllegalArgumentException("$method is not a function")
                }
            }

            fun FluoriteValue.getLength() = when (this) {
                is FluoriteString -> FluoriteInt(this.value.length)
                is FluoriteArray -> FluoriteInt(this.values.size)
                is FluoriteObject -> FluoriteInt(this.map.size)
                else -> throw IllegalArgumentException("Can not calculate length: $this")
            }

            fun FluoriteValue.fromJson(): FluoriteValue {
                val data = Json.decodeFromString<JsonElement>((this as FluoriteString).value)
                fun f(data: JsonElement): FluoriteValue = when (data) {
                    is JsonObject -> FluoriteObject(FluoriteObject.fluoriteClass, data.mapValues { (_, it) -> f(it) }.toMutableMap())
                    is JsonArray -> FluoriteArray(data.map { f(it) })
                    is JsonNull -> FluoriteNull
                    is JsonPrimitive -> when {
                        data.isString -> data.content.toFluoriteString()
                        data.content == "true" -> FluoriteBoolean.TRUE
                        data.content == "false" -> FluoriteBoolean.FALSE
                        "." !in data.content -> FluoriteInt(data.content.toInt())
                        else -> FluoriteDouble(data.content.toDouble())
                    }

                    else -> throw IllegalArgumentException()
                }
                return f(data)
            }

            when (node.left.text) {
                "+" -> evaluate(node.right).toNumber()
                "-" -> evaluate(node.right).toNumber().negate()
                "?" -> evaluate(node.right).toBoolean()
                "!" -> evaluate(node.right).toBoolean().not()
                "&" -> evaluate(node.right).toFluoriteString()
                "$#" -> evaluate(node.right).getLength()
                "$&" -> this.toJson(evaluate(node.right))
                "$*" -> evaluate(node.right).fromJson()
                else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} B")
            }
        }

        is InfixNode -> when (node.operator.text) {
            "." -> when (val left = evaluate(node.left)) {
                is FluoriteString -> left.value.getOrNull((evaluate(node.right) as FluoriteInt).value)?.toString()?.let { FluoriteString(it) } ?: FluoriteNull
                is FluoriteArray -> left.values.getOrNull((evaluate(node.right) as FluoriteInt).value) ?: FluoriteNull

                is FluoriteObject -> {
                    val key = if (node.right is IdentifierNode) node.right.string else evaluate(node.right).toString()
                    left.map[key] ?: FluoriteNull
                }

                else -> throw IllegalArgumentException("Unknown operator: ${left::class} . ${node.right::class}")
            }

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
                val identifierNodes = when {
                    commasNode is EmptyNode -> listOf()
                    commasNode is ListNode && commasNode.operators.first().text == "," -> commasNode.nodes
                    commasNode is SemicolonNode -> commasNode.nodes
                    else -> listOf(commasNode)
                }
                val variables = identifierNodes.map {
                    require(it is IdentifierNode)
                    it.string
                }
                FluoriteFunction { arguments ->
                    val frame = Frame(this)
                    frame.variables["__"] = Variable(false, FluoriteArray(arguments))
                    variables.forEachIndexed { i, it ->
                        frame.variables[it] = Variable(true, arguments.getOrNull(i) ?: FluoriteNull)
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
                    frame.variables[variable] = Variable(false, value)
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

        is RootNode -> evaluateRootNode(node.main)
    }
}
