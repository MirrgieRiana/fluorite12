package mirrg.fluorite12.compile

import mirrg.fluorite12.BracketsNode
import mirrg.fluorite12.BracketsType
import mirrg.fluorite12.CommasNode
import mirrg.fluorite12.ComparisonsNode
import mirrg.fluorite12.ConditionNode
import mirrg.fluorite12.EmbeddedStringNode
import mirrg.fluorite12.EmptyNode
import mirrg.fluorite12.FloatNode
import mirrg.fluorite12.FluoriteDouble
import mirrg.fluorite12.FluoriteInt
import mirrg.fluorite12.FluoriteNumber
import mirrg.fluorite12.FluoriteString
import mirrg.fluorite12.FluoriteValue
import mirrg.fluorite12.FormattedStringContent
import mirrg.fluorite12.Frame
import mirrg.fluorite12.HexadecimalNode
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.InfixNode
import mirrg.fluorite12.IntegerNode
import mirrg.fluorite12.LeftNode
import mirrg.fluorite12.LiteralStringContent
import mirrg.fluorite12.Node
import mirrg.fluorite12.NodeStringContent
import mirrg.fluorite12.RawStringNode
import mirrg.fluorite12.RightBracketsNode
import mirrg.fluorite12.RightNode
import mirrg.fluorite12.RootNode
import mirrg.fluorite12.SemicolonsNode
import mirrg.fluorite12.TemplateStringNode
import mirrg.fluorite12.contains
import mirrg.fluorite12.defineVariable
import mirrg.fluorite12.getVariable
import mirrg.fluorite12.instanceOf
import mirrg.fluorite12.operations.AndGetter
import mirrg.fluorite12.operations.ArrayCreationGetter
import mirrg.fluorite12.operations.ArrayItemSetter
import mirrg.fluorite12.operations.AssignmentGetter
import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.ComparisonChainGetter
import mirrg.fluorite12.operations.ConversionStringGetter
import mirrg.fluorite12.operations.DivGetter
import mirrg.fluorite12.operations.DivisibleGetter
import mirrg.fluorite12.operations.ElvisGetter
import mirrg.fluorite12.operations.EntryGetter
import mirrg.fluorite12.operations.ExclusiveRangeGetter
import mirrg.fluorite12.operations.FormattedStringGetter
import mirrg.fluorite12.operations.FromJsonGetter
import mirrg.fluorite12.operations.FunctionBindGetter
import mirrg.fluorite12.operations.FunctionGetter
import mirrg.fluorite12.operations.FunctionInvocationGetter
import mirrg.fluorite12.operations.GetLengthGetter
import mirrg.fluorite12.operations.Getter
import mirrg.fluorite12.operations.GetterRunner
import mirrg.fluorite12.operations.IfGetter
import mirrg.fluorite12.operations.ItemAccessGetter
import mirrg.fluorite12.operations.LinesGetter
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.LiteralStringGetter
import mirrg.fluorite12.operations.MethodBindGetter
import mirrg.fluorite12.operations.MethodInvocationGetter
import mirrg.fluorite12.operations.MinusGetter
import mirrg.fluorite12.operations.ModGetter
import mirrg.fluorite12.operations.NewEnvironmentGetter
import mirrg.fluorite12.operations.NullGetter
import mirrg.fluorite12.operations.ObjectCreationGetter
import mirrg.fluorite12.operations.OrGetter
import mirrg.fluorite12.operations.PipeGetter
import mirrg.fluorite12.operations.PlusGetter
import mirrg.fluorite12.operations.PowerGetter
import mirrg.fluorite12.operations.RangeGetter
import mirrg.fluorite12.operations.Runner
import mirrg.fluorite12.operations.Setter
import mirrg.fluorite12.operations.StreamConcatenationGetter
import mirrg.fluorite12.operations.StringConcatenationGetter
import mirrg.fluorite12.operations.ThrowGetter
import mirrg.fluorite12.operations.TimesGetter
import mirrg.fluorite12.operations.ToBooleanGetter
import mirrg.fluorite12.operations.ToJsonGetter
import mirrg.fluorite12.operations.ToNegativeBooleanGetter
import mirrg.fluorite12.operations.ToNegativeNumberGetter
import mirrg.fluorite12.operations.ToNumberGetter
import mirrg.fluorite12.operations.ToStringGetter
import mirrg.fluorite12.operations.TryCatchGetter
import mirrg.fluorite12.operations.TryCatchRunner
import mirrg.fluorite12.operations.VariableGetter
import mirrg.fluorite12.operations.VariableSetter
import mirrg.fluorite12.text

fun Frame.compileToGetter(node: Node): Getter {
    return when (node) {
        is EmptyNode -> NullGetter

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
                        val newNode = frame.compileToGetter(it.main)
                        ConversionStringGetter(NewEnvironmentGetter(frame.nextVariableIndex, newNode))
                    }

                    is FormattedStringContent -> FormattedStringGetter(it.formatter, compileToGetter(it.main))
                }
            }
            StringConcatenationGetter(getters)
        }

        is BracketsNode -> when (node.type) {
            BracketsType.ROUND -> {
                val frame = Frame(this)
                val newNode = frame.compileToGetter(node.main)
                NewEnvironmentGetter(frame.nextVariableIndex, newNode)
            }

            BracketsType.SQUARE -> {
                val nodes = if (node.main is SemicolonsNode) node.main.nodes else listOf(node.main)
                ArrayCreationGetter(nodes.filter { it !is EmptyNode }.map { compileToGetter(it) })
            }

            BracketsType.CURLY -> {
                val contentNodes = if (node.main is SemicolonsNode) node.main.nodes else listOf(node.main)
                ObjectCreationGetter(null, contentNodes.filter { it !is EmptyNode }.map { compileToGetter(it) })
            }
        }

        is RightNode -> when {
            node.right.text.startsWith(".") -> compileUnaryOperatorToGetter(node.right.text.drop(1), node.left)
            else -> throw IllegalArgumentException("Unknown operator: A ${node.right.text}")
        }

        is RightBracketsNode -> when (node.type) {
            BracketsType.ROUND -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    MethodInvocationGetter(receiverGetter, name, argumentGetters)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    FunctionInvocationGetter(functionGetter, argumentGetters)
                }
            }

            BracketsType.SQUARE -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    MethodBindGetter(receiverGetter, name, argumentGetters)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    FunctionBindGetter(functionGetter, argumentGetters)
                }
            }

            BracketsType.CURLY -> {
                val parentGetter = compileToGetter(node.main)
                val contentNodes = if (node.argument is SemicolonsNode) node.argument.nodes else listOf(node.argument)
                val contentGetters = contentNodes.filter { it !is EmptyNode }.map { compileToGetter(it) }
                ObjectCreationGetter(parentGetter, contentGetters)
            }
        }

        is LeftNode -> compileUnaryOperatorToGetter(node.left.text, node.right)

        is InfixNode -> compileInfixOperatorToGetter(node.operator.text, node.left, node.right)

        is ComparisonsNode -> {
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
                    "@" -> ({ a, b -> b.contains(a).value })
                    else -> throw IllegalArgumentException("Unknown operator: A ${it.text} B")
                }
            }
            ComparisonChainGetter(termGetters, operators)
        }

        is ConditionNode -> IfGetter(compileToGetter(node.condition), compileToGetter(node.ok), compileToGetter(node.ng))

        is CommasNode -> StreamConcatenationGetter(node.nodes.filter { it !is EmptyNode }.map { compileToGetter(it) })

        is SemicolonsNode -> {
            val runners = node.nodes.dropLast(1).flatMap { compileToRunner(it) }
            val getter = compileToGetter(node.nodes.last())
            if (runners.isEmpty()) return getter
            return LinesGetter(runners, getter)
        }

        is RootNode -> compileToGetter(node.main)
    }
}

private fun Frame.compileUnaryOperatorToGetter(text: String, main: Node): Getter {
    return when (text) {
        "+" -> ToNumberGetter(compileToGetter(main))
        "-" -> ToNegativeNumberGetter(compileToGetter(main))
        "?" -> ToBooleanGetter(compileToGetter(main))
        "!" -> ToNegativeBooleanGetter(compileToGetter(main))
        "&" -> ToStringGetter(compileToGetter(main))
        "$#" -> GetLengthGetter(compileToGetter(main))
        "$&" -> ToJsonGetter(compileToGetter(main))
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
            TryCatchGetter(compileToGetter(left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToGetter(rightNode))
        }

        ":" -> {
            val leftGetter = when (left) {
                is IdentifierNode -> LiteralGetter(FluoriteString(left.string))
                else -> compileToGetter(left)
            }
            EntryGetter(leftGetter, compileToGetter(right))
        }

        "=" -> {
            val setter = compileToSetter(left)
            val getter = compileToGetter(right)
            AssignmentGetter(setter, getter)
        }

        "->" -> {
            val commasNode = if (left is BracketsNode && left.type == BracketsType.ROUND) {
                left.main
            } else {
                left
            }
            val identifierNodes = when (commasNode) {
                is EmptyNode -> listOf()
                is CommasNode -> commasNode.nodes
                is SemicolonsNode -> commasNode.nodes
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

        ">>" -> {
            val valueGetter = compileToGetter(left)
            val functionGetter = compileToGetter(right)
            FunctionInvocationGetter(functionGetter, listOf(valueGetter))
        }

        "<<" -> {
            val valueGetter = compileToGetter(right)
            val functionGetter = compileToGetter(left)
            FunctionInvocationGetter(functionGetter, listOf(valueGetter))
        }

        else -> throw IllegalArgumentException("Unknown operator: A $text B")
    }
}

private fun Frame.compileToRunner(node: Node): List<Runner> {
    return when {
        node is EmptyNode -> listOf()

        node is InfixNode && node.operator.text == "=" -> { // 代入文
            val setter = compileToSetter(node.left)
            val getter = compileToGetter(node.right)
            listOf(AssignmentRunner(setter, getter))
        }

        node is InfixNode && node.operator.text == ":=" -> when { // 宣言文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variableIndex = defineVariable(name)
                listOf(AssignmentRunner(VariableSetter(frameIndex, variableIndex), compileToGetter(node.right)))
            }

            else -> throw IllegalArgumentException("Illegal definition: ${node.left::class} := ${node.right::class}")
        }

        node is InfixNode && node.operator.text == "!?" -> {
            val (name, rightNode) = if (node.right is InfixNode && node.right.operator.text == "=>") {
                require(node.right.left is IdentifierNode)
                Pair(node.right.left.string, node.right.right)
            } else {
                Pair("_", node.right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(name)
            listOf(TryCatchRunner(compileToRunner(node.left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToRunner(rightNode)))
        }

        else -> listOf(GetterRunner(compileToGetter(node))) // 式文
    }
}

private fun Frame.compileToSetter(node: Node): Setter {
    return when (node) {
        is IdentifierNode -> {
            val name = node.string
            val (frameIndex, variableIndex) = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
            VariableSetter(frameIndex, variableIndex)
        }

        is RightBracketsNode -> when (node.type) {
            BracketsType.ROUND -> ArrayItemSetter(compileToGetter(node.main), compileToGetter(node.argument))
            else -> throw IllegalArgumentException("Illegal setter: ${node::class}")
        }

        else -> throw IllegalArgumentException("Illegal setter: ${node::class}")
    }
}
