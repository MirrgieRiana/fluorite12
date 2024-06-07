package mirrg.fluorite12

fun Frame.compileToGetter(node: Node): Getter {
    return when (node) {
        is EmptyNode -> throw IllegalArgumentException("Unexpected empty")

        is IdentifierNode -> {
            val name = node.string
            val variable = getVariable(name) ?: throw IllegalArgumentException("Unknown variable: $name")
            VariableGetter(variable.first, variable.second)
        }

        is IntegerNode -> LiteralGetter(FluoriteInt(node.string.toInt()))

        is HexadecimalNode -> LiteralGetter(FluoriteInt(node.string.toInt(16)))

        is FloatNode -> LiteralGetter(FluoriteDouble(node.string.toDouble()))

        is RawStringNode -> LiteralGetter(FluoriteString(node.node.string))

        is TemplateStringNode -> {
            val getters = node.stringContents.map {
                when (it) {
                    is LiteralStringContent -> LiteralStringGetter(it.string)
                    is NodeStringContent -> ConversionStringGetter(compileToGetter(it.main))
                    is FormattedStringContent -> FormattedStringGetter(it.formatter, compileToGetter(it.main))
                }
            }
            StringConcatenationGetter(getters)
        }

        is EmbeddedStringNode -> {
            val getters = node.stringContents.map {
                when (it) {
                    is LiteralStringContent -> LiteralStringGetter(it.string)

                    is NodeStringContent -> {
                        val frame = Frame(this)
                        val newNode = frame.compileRootNodeToGetter(it.main)
                        ConversionStringGetter(NewEnvironmentGetter(frame.nextVariableIndex, newNode))
                    }

                    is FormattedStringContent -> FormattedStringGetter(it.formatter, compileToGetter(it.main))
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
                val nodes = if (node.main is SemicolonNode) node.main.nodes else listOf(node.main)
                ArrayCreationGetter(nodes.filter { it !is EmptyNode }.map { compileToGetter(it) })
            }

            "{" -> {
                val contentNodes = if (node.main is SemicolonNode) node.main.nodes else listOf(node.main)
                ObjectCreationGetter(null, contentNodes.filter { it !is EmptyNode }.map { compileToGetter(it) })
            }

            else -> throw IllegalArgumentException("Unknown operator: ${node.left.text} A ${node.right.text}")
        }

        is RightNode -> when {
            node.right.text.startsWith(".") -> compileUnaryOperatorToGetter(node.right.text.drop(1), node.left)
            else -> throw IllegalArgumentException("Unknown operator: A ${node.right.text}")
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

            "[" -> {
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
                    MethodBindGetter(receiverGetter, name, argumentGetters)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    FunctionBindGetter(functionGetter, argumentGetters)
                }
            }

            "{" -> {
                val parentGetter = compileToGetter(node.main)
                val contentNodes = if (node.argument is SemicolonNode) node.argument.nodes else listOf(node.argument)
                val contentGetters = contentNodes.filter { it !is EmptyNode }.map { compileToGetter(it) }
                ObjectCreationGetter(parentGetter, contentGetters)
            }

            else -> throw IllegalArgumentException("Unknown operator: A ${node.left.text} B ${node.right.text}")
        }

        is LeftNode -> compileUnaryOperatorToGetter(node.left.text, node.right)

        is InfixNode -> compileInfixOperatorToGetter(node.operator.text, node.left, node.right)

        is ComparisonNode -> {
            val termGetters = node.nodes.map { compileToGetter(it) }
            val operators: List<suspend (FluoriteValue, FluoriteValue) -> Boolean> = node.operators.map {
                when (it.text) {
                    "==" -> ({ a, b -> a == b })
                    "!=" -> ({ a, b -> a != b })
                    ">" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() > (b as FluoriteNumber).value.toDouble() })
                    "<" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() < (b as FluoriteNumber).value.toDouble() })
                    ">=" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() >= (b as FluoriteNumber).value.toDouble() })
                    "<=" -> ({ a, b -> (a as FluoriteNumber).value.toDouble() <= (b as FluoriteNumber).value.toDouble() })
                    "?=" -> ({ a, b -> a.instanceOf(b) })
                    "@" -> ({ a, b -> (b.callMethod("CONTAINS", a) as FluoriteBoolean).value })
                    else -> throw IllegalArgumentException("Unknown operator: A ${it.text} B")
                }
            }
            ComparisonChainGetter(termGetters, operators)
        }

        is ConditionNode -> IfGetter(compileToGetter(node.condition), compileToGetter(node.ok), compileToGetter(node.ng))

        is CommaNode -> StreamConcatenationGetter(node.nodes.filter { it !is EmptyNode }.map { compileToGetter(it) })

        is SemicolonNode -> throw IllegalArgumentException("Unexpected semicolon")

        is RootNode -> compileRootNodeToGetter(node.main)
    }
}

private fun Frame.compileRootNodeToGetter(node: Node): Getter {
    val nodes = when (node) {
        is SemicolonNode -> node.nodes
        else -> listOf(node)
    }
    val runners = nodes.dropLast(1).mapNotNull {
        when (it) {
            is EmptyNode -> null
            else -> compileToRunner(it)
        }
    }
    val getter = when (val getterNode = nodes.last()) {
        is EmptyNode -> NullGetter
        else -> compileToGetter(getterNode)
    }
    if (runners.isEmpty()) return getter
    return LinesGetter(runners, getter)
}

private fun Frame.compileUnaryOperatorToGetter(text: String, main: Node): Getter {
    return when (text) {
        "+" -> ToNumberGetter(compileToGetter(main))
        "-" -> ToNegativeNumberGetter(compileToGetter(main))
        "?" -> ToBooleanGetter(compileToGetter(main))
        "!" -> ToNegativeBooleanGetter(compileToGetter(main))
        "&" -> MethodInvocationGetter(compileToGetter(main), "TO_STRING", listOf())
        "$#" -> GetLengthGetter(compileToGetter(main))
        "$&" -> MethodInvocationGetter(compileToGetter(main), "TO_JSON", listOf())
        "$*" -> FromJsonGetter(compileToGetter(main))
        "!!" -> ThrowGetter(compileToGetter(main))
        else -> throw IllegalArgumentException("Unknown operator: Unary $text")
    }
}

private fun Frame.compileInfixOperatorToGetter(text: String, left: Node, right: Node): Getter {
    return when (text) {
        "." -> {
            val receiverGetter = compileToGetter(left)
            val nameGetter = when (right) {
                is IdentifierNode -> LiteralGetter(FluoriteString(right.string))
                else -> compileToGetter(right)
            }
            ItemAccessGetter(receiverGetter, nameGetter)
        }

        "+" -> PlusGetter(compileToGetter(left), compileToGetter(right))
        "&" -> StringConcatenationGetter(listOf(ConversionStringGetter(compileToGetter(left)), ConversionStringGetter(compileToGetter(right))))
        "-" -> MinusGetter(compileToGetter(left), compileToGetter(right))
        "*" -> TimesGetter(compileToGetter(left), compileToGetter(right))
        "/" -> DivGetter(compileToGetter(left), compileToGetter(right))
        "%%" -> DivisibleGetter(compileToGetter(left), compileToGetter(right))
        "%" -> ModGetter(compileToGetter(left), compileToGetter(right))
        "^" -> PowerGetter(compileToGetter(left), compileToGetter(right))
        ".." -> RangeGetter(compileToGetter(left), compileToGetter(right))
        "~" -> ExclusiveRangeGetter(compileToGetter(left), compileToGetter(right))
        "&&" -> AndGetter(compileToGetter(left), compileToGetter(right))
        "||" -> OrGetter(compileToGetter(left), compileToGetter(right))
        "?:" -> ElvisGetter(compileToGetter(left), compileToGetter(right))

        "!?" -> {
            val (name, rightNode) = if (right is InfixNode && right.operator.text == "=>") {
                require(right.left is IdentifierNode)
                Pair(right.left.string, right.right)
            } else {
                Pair("_", right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(name)
            CatchGetter(compileToGetter(left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToGetter(rightNode))
        }

        ":" -> {
            val leftGetter = when (left) {
                is IdentifierNode -> LiteralGetter(FluoriteString(left.string))
                else -> compileToGetter(left)
            }
            EntryGetter(leftGetter, compileToGetter(right))
        }

        "=" -> {
            when (left) {
                is IdentifierNode -> {
                    val name = left.string
                    val (frameIndex, variableIndex) = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
                    AssignmentGetter(frameIndex, variableIndex, compileToGetter(right))
                }

                else -> throw IllegalArgumentException("Illegal assignation: ${left::class} = ${right::class}")
            }
        }

        "->" -> {
            val commasNode = if (left is BracketNode && left.left.text == "(") {
                left.main
            } else {
                left
            }
            val identifierNodes = when (commasNode) {
                is EmptyNode -> listOf()
                is CommaNode -> commasNode.nodes
                is SemicolonNode -> commasNode.nodes
                else -> listOf(commasNode)
            }
            val variables = identifierNodes.map {
                require(it is IdentifierNode)
                it.string
            }
            val newFrame = Frame(this)
            val argumentsVariableIndex = newFrame.defineVariable("__")
            val variableIndices = variables.map { newFrame.defineVariable(it) }
            val getter = newFrame.compileToGetter(right)
            FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
        }

        "|" -> {
            val streamGetter = compileToGetter(left)
            val (variable, contentNode) = if (right is InfixNode && right.operator.text == "=>") {
                require(right.left is IdentifierNode)
                Pair(right.left.string, right.right)
            } else {
                Pair("_", right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(variable)
            val contentGetter = newFrame.compileToGetter(contentNode)
            PipeGetter(streamGetter, newFrame.frameIndex, argumentVariableIndex, contentGetter)
        }

        "?|" -> {
            val streamGetter = compileToGetter(left)
            val (variable, contentNode) = if (right is InfixNode && right.operator.text == "=>") {
                require(right.left is IdentifierNode)
                Pair(right.left.string, right.right)
            } else {
                Pair("_", right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(variable)
            val contentGetter = newFrame.compileToGetter(contentNode)
            FilterPipeGetter(streamGetter, newFrame.frameIndex, argumentVariableIndex, contentGetter)
        }

        "!|" -> {
            val streamGetter = compileToGetter(left)
            val (variable, contentNode) = if (right is InfixNode && right.operator.text == "=>") {
                require(right.left is IdentifierNode)
                Pair(right.left.string, right.right)
            } else {
                Pair("_", right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(variable)
            val contentGetter = newFrame.compileToGetter(contentNode)
            NotFilterPipeGetter(streamGetter, newFrame.frameIndex, argumentVariableIndex, contentGetter)
        }

        ">>" -> {
            val streamGetter = compileToGetter(left)
            val functionGetter = compileToGetter(right)
            FunctionInvocationGetter(functionGetter, listOf(streamGetter))
        }

        "<<" -> {
            val streamGetter = compileToGetter(right)
            val functionGetter = compileToGetter(left)
            FunctionInvocationGetter(functionGetter, listOf(streamGetter))
        }

        else -> throw IllegalArgumentException("Unknown operator: A $text B")
    }
}

private fun Frame.compileToRunner(node: Node): Runner {
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
