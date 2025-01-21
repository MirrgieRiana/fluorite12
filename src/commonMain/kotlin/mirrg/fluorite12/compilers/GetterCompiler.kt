package mirrg.fluorite12.compilers

import mirrg.fluorite12.ArrowBracketsNode
import mirrg.fluorite12.BracketsNode
import mirrg.fluorite12.BracketsType
import mirrg.fluorite12.CommasNode
import mirrg.fluorite12.ComparisonsNode
import mirrg.fluorite12.ConditionNode
import mirrg.fluorite12.EmbeddedStringNode
import mirrg.fluorite12.EmptyNode
import mirrg.fluorite12.FloatNode
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
import mirrg.fluorite12.RightArrowBracketsNode
import mirrg.fluorite12.RightBracketsNode
import mirrg.fluorite12.RightNode
import mirrg.fluorite12.SemicolonsNode
import mirrg.fluorite12.TemplateStringNode
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.defineVariable
import mirrg.fluorite12.getMountCounts
import mirrg.fluorite12.getVariable
import mirrg.fluorite12.operations.AndGetter
import mirrg.fluorite12.operations.ArrayCreationGetter
import mirrg.fluorite12.operations.AssignmentGetter
import mirrg.fluorite12.operations.Comparator
import mirrg.fluorite12.operations.ComparisonChainGetter
import mirrg.fluorite12.operations.ContainsComparator
import mirrg.fluorite12.operations.ConversionStringGetter
import mirrg.fluorite12.operations.DivGetter
import mirrg.fluorite12.operations.DivisibleGetter
import mirrg.fluorite12.operations.ElvisGetter
import mirrg.fluorite12.operations.EntryGetter
import mirrg.fluorite12.operations.EqualComparator
import mirrg.fluorite12.operations.ExclusiveRangeGetter
import mirrg.fluorite12.operations.FormattedStringGetter
import mirrg.fluorite12.operations.FromJsonGetter
import mirrg.fluorite12.operations.FunctionBindGetter
import mirrg.fluorite12.operations.FunctionGetter
import mirrg.fluorite12.operations.FunctionInvocationGetter
import mirrg.fluorite12.operations.GetLengthGetter
import mirrg.fluorite12.operations.Getter
import mirrg.fluorite12.operations.GreaterComparator
import mirrg.fluorite12.operations.GreaterEqualComparator
import mirrg.fluorite12.operations.IfGetter
import mirrg.fluorite12.operations.InstanceOfComparator
import mirrg.fluorite12.operations.ItemAccessGetter
import mirrg.fluorite12.operations.LessComparator
import mirrg.fluorite12.operations.LessEqualComparator
import mirrg.fluorite12.operations.LinesGetter
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.LiteralStringGetter
import mirrg.fluorite12.operations.MethodAccessGetter
import mirrg.fluorite12.operations.MinusGetter
import mirrg.fluorite12.operations.ModGetter
import mirrg.fluorite12.operations.MountGetter
import mirrg.fluorite12.operations.NewEnvironmentGetter
import mirrg.fluorite12.operations.NotEqualComparator
import mirrg.fluorite12.operations.NullGetter
import mirrg.fluorite12.operations.ObjectCreationGetter
import mirrg.fluorite12.operations.OrGetter
import mirrg.fluorite12.operations.PipeGetter
import mirrg.fluorite12.operations.PlusGetter
import mirrg.fluorite12.operations.PowerGetter
import mirrg.fluorite12.operations.RangeGetter
import mirrg.fluorite12.operations.SpaceshipGetter
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
import mirrg.fluorite12.operations.TryCatchWithVariableGetter
import mirrg.fluorite12.operations.VariableGetter
import mirrg.fluorite12.text

fun Frame.compileToGetter(node: Node): Getter {
    return when (node) {
        is EmptyNode -> NullGetter

        is IdentifierNode -> {
            val name = node.string
            val variable = getVariable(name)
            if (variable != null) {
                VariableGetter(variable.first, variable.second)
            } else {
                val mountCounts = mutableListOf<Int>()
                var frame = this
                while (true) {
                    mountCounts += frame.mountCount
                    frame = frame.parent ?: break
                }
                MountGetter(mountCounts.reversed().toIntArray(), name)
            }
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
                        ConversionStringGetter(NewEnvironmentGetter(frame.nextVariableIndex, frame.mountCount, newNode))
                    }

                    is FormattedStringContent -> FormattedStringGetter(it.formatter, compileToGetter(it.main))
                }
            }
            StringConcatenationGetter(getters)
        }

        is ArrowBracketsNode -> throw IllegalArgumentException("Unknown operator: ${node.left.text} ${node.arguments} ${node.arrow.text} ${node.body} ${node.right.text}")

        is BracketsNode -> when (node.type) {
            BracketsType.ROUND -> {
                val frame = Frame(this)
                val newNode = frame.compileToGetter(node.main)
                NewEnvironmentGetter(frame.nextVariableIndex, frame.mountCount, newNode)
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

        is RightArrowBracketsNode -> when (node.type) {
            BracketsType.ROUND -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val variable = getVariable("::$name")
                    val mountCounts = getMountCounts()

                    val commasNode = node.arguments
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
                    val getter = newFrame.compileToGetter(node.body)
                    val getter2 = FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
                    val getter3 = NewEnvironmentGetter(newFrame.nextVariableIndex, newFrame.mountCount, getter2)

                    val argumentGetters = listOf(getter3)
                    MethodAccessGetter(receiverGetter, variable, mountCounts, name, argumentGetters, false)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)

                    val commasNode = node.arguments
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
                    val getter = newFrame.compileToGetter(node.body)
                    val getter2 = FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
                    val getter3 = NewEnvironmentGetter(newFrame.nextVariableIndex, newFrame.mountCount, getter2)

                    val argumentGetters = listOf(getter3)
                    FunctionInvocationGetter(functionGetter, argumentGetters)
                }
            }

            BracketsType.SQUARE -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val variable = getVariable("::$name")
                    val mountCounts = getMountCounts()

                    val commasNode = node.arguments
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
                    val getter = newFrame.compileToGetter(node.body)
                    val getter2 = FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
                    val getter3 = NewEnvironmentGetter(newFrame.nextVariableIndex, newFrame.mountCount, getter2)

                    val argumentGetters = listOf(getter3)
                    MethodAccessGetter(receiverGetter, variable, mountCounts, name, argumentGetters, true)
                } else { // 関数呼び出し
                    val functionGetter = compileToGetter(node.main)

                    val commasNode = node.arguments
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
                    val getter = newFrame.compileToGetter(node.body)
                    val getter2 = FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
                    val getter3 = NewEnvironmentGetter(newFrame.nextVariableIndex, newFrame.mountCount, getter2)

                    val argumentGetters = listOf(getter3)
                    FunctionBindGetter(functionGetter, argumentGetters)
                }
            }

            else -> throw IllegalArgumentException("Unknown operator: ${node.main} ${node.left.text} ${node.arguments} ${node.arrow.text} ${node.body} ${node.right.text}")
        }

        is RightBracketsNode -> when (node.type) {
            BracketsType.ROUND -> {
                if (node.main is InfixNode && node.main.operator.text == "::") { // メソッド呼出し
                    if (node.main.right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: ${node.main.right}")
                    val receiverGetter = compileToGetter(node.main.left)
                    val name = node.main.right.string
                    val variable = getVariable("::$name")
                    val mountCounts = getMountCounts()
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    MethodAccessGetter(receiverGetter, variable, mountCounts, name, argumentGetters, false)
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
                    val variable = getVariable("::$name")
                    val mountCounts = getMountCounts()
                    val argumentNodes = when (node.argument) {
                        is EmptyNode -> listOf()
                        is SemicolonsNode -> node.argument.nodes
                        else -> listOf(node.argument)
                    }
                    val argumentGetters = argumentNodes.map { compileToGetter(it) }
                    MethodAccessGetter(receiverGetter, variable, mountCounts, name, argumentGetters, true)
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
            val operators: List<Comparator> = node.operators.map {
                when (it.text) {
                    "==" -> EqualComparator
                    "!=" -> NotEqualComparator
                    ">" -> GreaterComparator
                    "<" -> LessComparator
                    ">=" -> GreaterEqualComparator
                    "<=" -> LessEqualComparator
                    "?=" -> InstanceOfComparator
                    "@" -> ContainsComparator
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
            LinesGetter(runners, getter)
        }
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

        "::" -> {
            if (right !is IdentifierNode) throw IllegalArgumentException("Must be an identifier: $right")
            val receiverGetter = compileToGetter(left)
            val name = right.string
            val variable = getVariable("::$name")
            val mountCounts = getMountCounts()
            MethodAccessGetter(receiverGetter, variable, mountCounts, name, listOf(), true)
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
        "<=>" -> SpaceshipGetter(compileToGetter(left), compileToGetter(right))
        "~" -> ExclusiveRangeGetter(compileToGetter(left), compileToGetter(right))
        "&&" -> AndGetter(compileToGetter(left), compileToGetter(right))
        "||" -> OrGetter(compileToGetter(left), compileToGetter(right))
        "?:" -> ElvisGetter(compileToGetter(left), compileToGetter(right))

        "!?" -> {
            val (name, rightNode) = if (right is ArrowBracketsNode && right.type == BracketsType.ROUND) {
                require(right.arguments is IdentifierNode)
                Pair(right.arguments.string, right.body)
            } else {
                Pair(null, right)
            }
            if (name != null) {
                val newFrame = Frame(this)
                val argumentVariableIndex = newFrame.defineVariable(name)
                TryCatchWithVariableGetter(compileToGetter(left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToGetter(rightNode))
            } else {
                TryCatchGetter(compileToGetter(left), compileToGetter(rightNode))
            }
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
