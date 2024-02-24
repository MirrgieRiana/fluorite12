package mirrg.fluorite12


class Frame(val parent: Frame? = null) {
    val frameIndex: Int = parent?.let { it.frameIndex + 1 } ?: 0
    val variableIndexTable = mutableMapOf<String, Int>()
    var nextVariableIndex = 0
}

class Environment(val parent: Environment?, variableCount: Int) {
    val variableTable: Array<Array<FluoriteValue>> = if (parent != null) {
        arrayOf(*parent.variableTable, Array(variableCount) { FluoriteNull })
    } else {
        arrayOf(Array(variableCount) { FluoriteNull })
    }
    val overrides = mutableMapOf<Signature, FluoriteValue>()
}

class Signature(val fluoriteClass: FluoriteObject, val name: String) {
    override fun toString() = "Signature[$fluoriteClass.$name]"
    override fun equals(other: Any?) = other is Signature && other.fluoriteClass === this.fluoriteClass && other.name == this.name
    override fun hashCode() = 31 * fluoriteClass.hashCode() + name.hashCode()
}


fun Frame.defineVariable(name: String): Int {
    val variableIndex = nextVariableIndex
    variableIndexTable[name] = variableIndex
    nextVariableIndex++
    return variableIndex
}

fun Frame.getVariable(name: String): Pair<Int, Int>? {
    var currentFrame = this
    while (true) {
        val variableIndex = currentFrame.variableIndexTable[name]
        if (variableIndex != null) return Pair(currentFrame.frameIndex, variableIndex)
        currentFrame = currentFrame.parent ?: return null
    }
}

fun Environment.getOverride(signature: Signature): FluoriteValue? {
    var currentEnvironment = this
    while (true) {
        val override = currentEnvironment.overrides[signature]
        if (override != null) return override
        currentEnvironment = currentEnvironment.parent ?: return null
    }
}

fun Frame.defineConstant(name: String, value: FluoriteValue): Runner {
    val variableIndex = defineVariable(name)
    val getter = LiteralGetter(value)
    return AssignmentRunner(frameIndex, variableIndex, getter)
}


fun Frame.defineCommonBuiltinVariables() = listOf(
    defineConstant("VALUE_CLASS", FluoriteValue.fluoriteClass),
    defineConstant("NULL_CLASS", FluoriteNull.fluoriteClass),
    defineConstant("VOID_CLASS", FluoriteVoid.fluoriteClass),
    defineConstant("INT_CLASS", FluoriteInt.fluoriteClass),
    defineConstant("DOUBLE_CLASS", FluoriteDouble.fluoriteClass),
    defineConstant("BOOLEAN_CLASS", FluoriteBoolean.fluoriteClass),
    defineConstant("STRING_CLASS", FluoriteString.fluoriteClass),
    defineConstant("ARRAY_CLASS", FluoriteArray.fluoriteClass),
    defineConstant("OBJECT_CLASS", FluoriteObject.fluoriteClass),
    defineConstant("FUNCTION_CLASS", FluoriteFunction.fluoriteClass),
    defineConstant("STREAM_CLASS", FluoriteStream.fluoriteClass),

    defineConstant("NULL", FluoriteNull),
    defineConstant("TRUE", FluoriteBoolean.TRUE),
    defineConstant("FALSE", FluoriteBoolean.FALSE),
    defineConstant("VOID", FluoriteVoid),
)

suspend fun Frame.compileToGetter(node: Node): Getter {
    return when (node) {
        is EmptyNode -> throw IllegalArgumentException("Unexpected empty")

        is IdentifierNode -> {
            val name = node.string
            val variable = getVariable(name) ?: throw IllegalArgumentException("Unknown variable: $name")
            VariableGetter(variable.first, variable.second)
        }

        is IntegerNode -> LiteralGetter(FluoriteInt(node.string.toInt()))

        is FloatNode -> LiteralGetter(FluoriteDouble(node.string.toDouble()))

        is RawStringNode -> LiteralGetter(FluoriteString(node.node.string))

        is TemplateStringNode -> {
            val getters = node.stringContents.map {
                when (it) {
                    is LiteralStringContent -> LiteralStringGetter(it.string)
                    is NodeStringContent -> ConversionStringGetter(compileToGetter(it.main))
                }
            }
            StringConcatenationGetter(getters)
        }

        is EmbeddedStringNode -> {
            val getters = node.stringContents.map {
                when (it) {
                    is LiteralStringContent -> LiteralStringGetter(it.string)
                    is NodeStringContent -> ConversionStringGetter(compileToGetter(it.main))
                }
            }
            StringConcatenationGetter(getters)
        }

        is BracketNode -> when (node.left.text) {
            "(" -> {
                val frame = Frame(this)
                val newNode = frame.compileRootNodeToGetter(node.main)
                NewEnvironmentGetter(frame.nextVariableIndex, newNode)
            }

            "[" -> {
                val nodes = when (node.main) {
                    is EmptyNode -> listOf()
                    is SemicolonNode -> node.main.nodes
                    else -> listOf(node.main)
                }
                ArrayCreationGetter(nodes.map { compileToGetter(it) })
            }

            "{" -> {
                val contentNodes = when (node.main) {
                    is EmptyNode -> listOf()
                    is SemicolonNode -> node.main.nodes
                    else -> listOf(node.main)
                }
                ObjectCreationGetter(null, contentNodes.map { compileToGetter(it) })
            }

            else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} A ${node.right.text}")
        }

        is RightBracketNode -> when (node.left.text) {
            "(" -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    MethodInvocationGetter(receiverGetter, name, argumentGetters)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    FunctionInvocationGetter(functionGetter, argumentGetters)
                }
            }

            "{" -> {
                val parentGetter = compileToGetter(node.main)
                val contentNodes = when (node.argument) {
                    is EmptyNode -> listOf()
                    is SemicolonNode -> node.argument.nodes
                    else -> listOf(node.argument)
                }
                val contentGetters = contentNodes.map { compileToGetter(it) }
                ObjectCreationGetter(parentGetter, contentGetters)
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.left.text} B ${node.right.text}")
        }

        is LeftNode -> {
            when (node.left.text) {
                "+" -> ToNumberGetter(compileToGetter(node.right))
                "-" -> ToNegativeNumberGetter(compileToGetter(node.right))
                "?" -> ToBooleanGetter(compileToGetter(node.right))
                "!" -> ToNegativeBooleanGetter(compileToGetter(node.right))
                "&" -> MethodInvocationGetter(compileToGetter(node.right), "TO_STRING", listOf())
                "$#" -> GetLengthGetter(compileToGetter(node.right))
                "$&" -> MethodInvocationGetter(compileToGetter(node.right), "TO_JSON", listOf())
                "$*" -> FromJsonGetter(compileToGetter(node.right))
                else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} B")
            }
        }

        is InfixNode -> when (node.operator.text) {
            "." -> {
                val receiverGetter = compileToGetter(node.left)
                val nameGetter = when (node.right) {
                    is IdentifierNode -> LiteralGetter(FluoriteString(node.right.string))
                    else -> compileToGetter(node.right)
                }
                ItemAccessGetter(receiverGetter, nameGetter)
            }

            "+" -> PlusGetter(compileToGetter(node.left), compileToGetter(node.right))
            "-" -> MinusGetter(compileToGetter(node.left), compileToGetter(node.right))
            "*" -> TimesGetter(compileToGetter(node.left), compileToGetter(node.right))
            "/" -> DivGetter(compileToGetter(node.left), compileToGetter(node.right))
            ".." -> RangeGetter(compileToGetter(node.left), compileToGetter(node.right))

            ":" -> {
                val leftGetter = when (node.left) {
                    is IdentifierNode -> LiteralGetter(FluoriteString(node.left.string))
                    else -> compileToGetter(node.left)
                }
                EntryGetter(leftGetter, compileToGetter(node.right))
            }

            "=" -> {
                when (node.left) {
                    is IdentifierNode -> {
                        val name = node.left.string
                        val (frameIndex, variableIndex) = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
                        AssignmentGetter(frameIndex, variableIndex, compileToGetter(node.right))
                    }

                    else -> throw IllegalArgumentException("Illegal assignation: ${node.left::class} = ${node.right::class}")
                }
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
                val newFrame = Frame(this)
                val argumentsVariableIndex = newFrame.defineVariable("__")
                val variableIndices = variables.map { newFrame.defineVariable(it) }
                val getter = newFrame.compileToGetter(node.right)
                FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
            }

            "|" -> {
                val streamGetter = compileToGetter(node.left)
                val (variable, contentNode) = if (node.right is InfixNode && node.right.operator.text == "=>") {
                    require(node.right.left is IdentifierNode)
                    Pair(node.right.left.string, node.right.right)
                } else {
                    Pair("_", node.right)
                }
                val newFrame = Frame(this)
                val argumentVariableIndex = newFrame.defineVariable(variable)
                val contentGetter = newFrame.compileToGetter(contentNode)
                PipeGetter(streamGetter, newFrame.frameIndex, argumentVariableIndex, contentGetter)
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.operator.text} B")
        }

        is ComparisonNode -> {
            val termGetters = node.nodes.map { compileToGetter(it) }
            val operators: List<(FluoriteValue, FluoriteValue) -> Boolean> = node.operators.map {
                when (it.text) {
                    "==" -> ({ a, b -> a == b })
                    "!=" -> ({ a, b -> a != b })
                    ">" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() > (b as FluoriteNumber).value.toDouble() })
                    "<" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() < (b as FluoriteNumber).value.toDouble() })
                    ">=" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() >= (b as FluoriteNumber).value.toDouble() })
                    "<=" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() <= (b as FluoriteNumber).value.toDouble() })
                    "?=" -> ({ a, b -> a.instanceOf(b) })
                    else -> throw IllegalArgumentException("Unknown operator: A ${it.text} B")
                }
            }
            ComparisonChainGetter(termGetters, operators)
        }

        is ConditionNode -> IfGetter(compileToGetter(node.condition), compileToGetter(node.ok), compileToGetter(node.ng))

        is ListNode -> when (node.operators.first().text) {
            "," -> StreamConcatenationGetter(node.nodes.map { compileToGetter(it) })
            else -> throw IllegalArgumentException("Unknown operator: A ${node.operators.first().text} B")
        }

        is SemicolonNode -> throw IllegalArgumentException("Unexpected semicolon")

        is RootNode -> compileRootNodeToGetter(node.main)
    }
}

private suspend fun Frame.compileRootNodeToGetter(node: Node): Getter {
    val nodes = when (node) {
        is SemicolonNode -> node.nodes
        else -> listOf(node)
    }
    val runners = nodes.dropLast(1).map { compileToRunner(it) }
    val getter = compileToGetter(nodes.last())
    return LinesGetter(runners, getter)
}

private suspend fun Frame.compileToRunner(node: Node): Runner {
    return when {
        node is InfixNode && node.operator.text == "=" -> when { // 代入文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val (frameIndex, variableIndex) = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
                AssignmentRunner(frameIndex, variableIndex, compileToGetter(node.right))
            }

            else -> throw IllegalArgumentException("Illegal assignation: ${node.left::class} = ${node.right::class}")
        }

        node is InfixNode && node.operator.text == ":=" -> when { // 宣言文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variableIndex = defineVariable(name)
                AssignmentRunner(frameIndex, variableIndex, compileToGetter(node.right))
            }

            else -> throw IllegalArgumentException("Illegal definition: ${node.left::class} := ${node.right::class}")
        }

        else -> GetterRunner(compileToGetter(node)) // 式文
    }
}
