package mirrg.fluorite12.compilers

import com.ionspin.kotlin.bignum.integer.toBigInteger
import mirrg.fluorite12.BracketsLiteralArrowedNode
import mirrg.fluorite12.BracketsLiteralArrowedRoundNode
import mirrg.fluorite12.BracketsLiteralSimpleCurlyNode
import mirrg.fluorite12.BracketsLiteralSimpleRoundNode
import mirrg.fluorite12.BracketsLiteralSimpleSquareNode
import mirrg.fluorite12.BracketsRightArrowedCurlyNode
import mirrg.fluorite12.BracketsRightArrowedNode
import mirrg.fluorite12.BracketsRightArrowedRoundNode
import mirrg.fluorite12.BracketsRightArrowedSquareNode
import mirrg.fluorite12.BracketsRightNode
import mirrg.fluorite12.BracketsRightSimpleCurlyNode
import mirrg.fluorite12.BracketsRightSimpleNode
import mirrg.fluorite12.BracketsRightSimpleRoundNode
import mirrg.fluorite12.BracketsRightSimpleSquareNode
import mirrg.fluorite12.CommasNode
import mirrg.fluorite12.ComparisonOperatorType
import mirrg.fluorite12.ComparisonsNode
import mirrg.fluorite12.ConditionNode
import mirrg.fluorite12.EmbeddedStringNode
import mirrg.fluorite12.EmptyNode
import mirrg.fluorite12.FloatNode
import mirrg.fluorite12.FormattedStringContent
import mirrg.fluorite12.Frame
import mirrg.fluorite12.HexadecimalNode
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.InfixAmpersandAmpersandNode
import mirrg.fluorite12.InfixAmpersandNode
import mirrg.fluorite12.InfixAsteriskNode
import mirrg.fluorite12.InfixCircumflexNode
import mirrg.fluorite12.InfixColonColonNode
import mirrg.fluorite12.InfixColonEqualNode
import mirrg.fluorite12.InfixColonNode
import mirrg.fluorite12.InfixEqualGreaterNode
import mirrg.fluorite12.InfixEqualNode
import mirrg.fluorite12.InfixExclamationIdentifierNode
import mirrg.fluorite12.InfixExclamationQuestionNode
import mirrg.fluorite12.InfixGreaterGreaterNode
import mirrg.fluorite12.InfixIdentifierNode
import mirrg.fluorite12.InfixLessEqualGreaterNode
import mirrg.fluorite12.InfixLessLessNode
import mirrg.fluorite12.InfixMinusGreaterNode
import mirrg.fluorite12.InfixMinusNode
import mirrg.fluorite12.InfixNode
import mirrg.fluorite12.InfixPercentNode
import mirrg.fluorite12.InfixPercentPercentNode
import mirrg.fluorite12.InfixPeriodNode
import mirrg.fluorite12.InfixPeriodPeriodNode
import mirrg.fluorite12.InfixPipeNode
import mirrg.fluorite12.InfixPipePipeNode
import mirrg.fluorite12.InfixPlusNode
import mirrg.fluorite12.InfixQuestionColonColonNode
import mirrg.fluorite12.InfixQuestionColonNode
import mirrg.fluorite12.InfixQuestionPeriodNode
import mirrg.fluorite12.InfixSlashNode
import mirrg.fluorite12.InfixTildeNode
import mirrg.fluorite12.IntegerNode
import mirrg.fluorite12.LiteralStringContent
import mirrg.fluorite12.Node
import mirrg.fluorite12.NodeStringContent
import mirrg.fluorite12.RawStringNode
import mirrg.fluorite12.SemicolonsNode
import mirrg.fluorite12.TemplateStringNode
import mirrg.fluorite12.UnaryAmpersandNode
import mirrg.fluorite12.UnaryAtNode
import mirrg.fluorite12.UnaryDollarAmpersandNode
import mirrg.fluorite12.UnaryDollarAsteriskNode
import mirrg.fluorite12.UnaryDollarSharpNode
import mirrg.fluorite12.UnaryExclamationExclamationNode
import mirrg.fluorite12.UnaryExclamationNode
import mirrg.fluorite12.UnaryMinusNode
import mirrg.fluorite12.UnaryPlusNode
import mirrg.fluorite12.UnaryQuestionNode
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
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
import mirrg.fluorite12.operations.FunctionalMethodAccessGetter
import mirrg.fluorite12.operations.GetLengthGetter
import mirrg.fluorite12.operations.Getter
import mirrg.fluorite12.operations.GetterObjectInitializer
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
import mirrg.fluorite12.operations.ObjectInitializer
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
import mirrg.fluorite12.operations.VariableDefinitionObjectInitializer
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

        is IntegerNode -> LiteralGetter(node.string.toFluoriteNumber())

        is HexadecimalNode -> LiteralGetter(node.string.toBigInteger(base = 16).toString().toFluoriteNumber())

        is FloatNode -> LiteralGetter(node.string.toFluoriteNumber())

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

        is BracketsLiteralArrowedNode -> throw IllegalArgumentException("Unknown operator: ${node.left.text} ${node.arguments} ${node.arrow.text} ${node.body} ${node.right.text}")

        is BracketsLiteralSimpleRoundNode -> {
            val frame = Frame(this)
            val newNode = frame.compileToGetter(node.body)
            NewEnvironmentGetter(frame.nextVariableIndex, frame.mountCount, newNode)
        }

        is BracketsLiteralSimpleSquareNode -> {
            val nodes = if (node.body is SemicolonsNode) node.body.nodes else listOf(node.body)
            ArrayCreationGetter(nodes.filter { it !is EmptyNode }.map { compileToGetter(it) })
        }

        is BracketsLiteralSimpleCurlyNode -> compileObjectCreationToGetter(null, node.body)

        is UnaryPlusNode -> ToNumberGetter(compileToGetter(node.main))
        is UnaryMinusNode -> compileUnaryMinusToGetter(node.main)
        is UnaryQuestionNode -> ToBooleanGetter(compileToGetter(node.main))
        is UnaryExclamationNode -> ToNegativeBooleanGetter(compileToGetter(node.main))
        is UnaryAmpersandNode -> ToStringGetter(compileToGetter(node.main))
        is UnaryDollarSharpNode -> GetLengthGetter(compileToGetter(node.main))
        is UnaryDollarAmpersandNode -> ToJsonGetter(compileToGetter(node.main))
        is UnaryDollarAsteriskNode -> FromJsonGetter(compileToGetter(node.main))
        is UnaryAtNode -> throw IllegalArgumentException("Unknown operator: ${node.operator.text}")
        is UnaryExclamationExclamationNode -> ThrowGetter(compileToGetter(node.main))

        is BracketsRightArrowedRoundNode -> compileFunctionalAccessToGetter(node, false, ::createArrowedArgumentGetters)
        is BracketsRightArrowedSquareNode -> compileFunctionalAccessToGetter(node, true, ::createArrowedArgumentGetters)
        is BracketsRightArrowedCurlyNode -> throw IllegalArgumentException("Unknown operator: ${node.receiver} ${node.left.text} ${node.arguments} ${node.arrow.text} ${node.body} ${node.right.text}")
        is BracketsRightSimpleRoundNode -> compileFunctionalAccessToGetter(node, false, ::createSimpleArgumentGetters)
        is BracketsRightSimpleSquareNode -> compileFunctionalAccessToGetter(node, true, ::createSimpleArgumentGetters)
        is BracketsRightSimpleCurlyNode -> compileObjectCreationToGetter(node.receiver, node.body)

        is InfixNode -> compileInfixOperatorToGetter(node)

        is InfixIdentifierNode -> FunctionInvocationGetter(compileToGetter(node.infix), listOf(compileToGetter(node.left), compileToGetter(node.right)))
        is InfixExclamationIdentifierNode -> ToNegativeBooleanGetter(FunctionInvocationGetter(compileToGetter(node.infix), listOf(compileToGetter(node.left), compileToGetter(node.right))))

        is ComparisonsNode -> {
            val termGetters = node.nodes.map { compileToGetter(it) }
            val operators: List<Comparator> = node.operators.map {
                when (it.second) {
                    ComparisonOperatorType.EQUAL -> EqualComparator
                    ComparisonOperatorType.EXCLAMATION_EQUAL -> NotEqualComparator
                    ComparisonOperatorType.GREATER -> GreaterComparator
                    ComparisonOperatorType.LESS -> LessComparator
                    ComparisonOperatorType.GREATER_EQUAL -> GreaterEqualComparator
                    ComparisonOperatorType.LESS_EQUAL -> LessEqualComparator
                    ComparisonOperatorType.QUESTION_EQUAL -> InstanceOfComparator
                    ComparisonOperatorType.AT -> ContainsComparator
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

private fun Frame.compileObjectCreationToGetter(parentNode: Node?, bodyNode: Node): Getter {
    val parentGetter = parentNode?.let { compileToGetter(it) }
    val newFrame = Frame(this)
    val contentNodes = if (bodyNode is SemicolonsNode) bodyNode.nodes else listOf(bodyNode)
    val objectInitializerCreators: List<() -> ObjectInitializer> = contentNodes.mapNotNull { contentNode ->
        if (contentNode is EmptyNode) {
            null
        } else if (contentNode is InfixColonEqualNode && contentNode.left is IdentifierNode) {
            val variableIndex = newFrame.defineVariable(contentNode.left.string)
            ({ VariableDefinitionObjectInitializer(contentNode.left.string, newFrame.frameIndex, variableIndex, newFrame.compileToGetter(contentNode.right)) })
        } else {
            { GetterObjectInitializer(newFrame.compileToGetter(contentNode)) }
        }
    }
    return ObjectCreationGetter(parentGetter, newFrame.nextVariableIndex, objectInitializerCreators.map { it() })
}

private fun Frame.compileUnaryMinusToGetter(main: Node): Getter {
    return when (main) {
        is IntegerNode -> LiteralGetter("-${main.string}".toFluoriteNumber())
        is HexadecimalNode -> LiteralGetter("-${main.string}".toBigInteger(base = 16).toString().toFluoriteNumber())
        is FloatNode -> LiteralGetter("-${main.string}".toFluoriteNumber())
        else -> ToNegativeNumberGetter(compileToGetter(main))
    }
}

fun Frame.createArrowedArgumentGetters(node: BracketsRightArrowedNode): List<Getter> {
    val getter = compileFunctionBodyToGetter(node.arguments, node.body)
    return listOf(getter)
}

fun Frame.createSimpleArgumentGetters(node: BracketsRightSimpleNode): List<Getter> {
    val argumentNodes = when (node.body) {
        is EmptyNode -> listOf()
        is SemicolonsNode -> node.body.nodes
        else -> listOf(node.body)
    }
    return argumentNodes.map { compileToGetter(it) }
}

fun Frame.compileToMethodAccessGetter(receiverNode: Node, methodNode: Node, argumentGetters: List<Getter>, isBinding: Boolean, isNullSafe: Boolean): Getter {
    return when (methodNode) {
        is IdentifierNode -> {
            val receiverGetter = compileToGetter(receiverNode)
            val name = methodNode.string
            val variable = getVariable("::$name")
            val mountCounts = getMountCounts()
            MethodAccessGetter(receiverGetter, variable, mountCounts, name, argumentGetters, isBinding, isNullSafe)
        }

        is BracketsLiteralSimpleRoundNode -> {
            val receiverGetter = compileToGetter(receiverNode)
            val functionGetter = compileToGetter(methodNode)
            FunctionalMethodAccessGetter(receiverGetter, functionGetter, argumentGetters, isBinding, isNullSafe)
        }

        else -> throw IllegalArgumentException("Must be IdentifierNode or BracketsLiteralSimpleRoundNode: $methodNode")
    }
}

fun <T : BracketsRightNode> Frame.compileFunctionalAccessToGetter(node: T, isBinding: Boolean, argumentGettersFactory: (T) -> List<Getter>): Getter {
    return if (node.receiver is InfixColonColonNode) { // メソッド呼出し
        compileToMethodAccessGetter(node.receiver.left, node.receiver.right, argumentGettersFactory(node), isBinding, false)
    } else if (node.receiver is InfixQuestionColonColonNode) {
        compileToMethodAccessGetter(node.receiver.left, node.receiver.right, argumentGettersFactory(node), isBinding, true)
    } else { // 関数呼び出し
        val functionGetter = compileToGetter(node.receiver)
        val argumentGetters = argumentGettersFactory(node)
        if (!isBinding) {
            FunctionInvocationGetter(functionGetter, argumentGetters)
        } else {
            FunctionBindGetter(functionGetter, argumentGetters)
        }
    }
}

fun Frame.compileFunctionBodyToGetter(arguments: Node, body: Node): Getter {
    val identifierNodes = when (val commasNode = arguments) {
        is EmptyNode -> listOf()
        is CommasNode -> commasNode.nodes
        is SemicolonsNode -> commasNode.nodes
        else -> listOf(commasNode)
    }
    val variables = identifierNodes.mapNotNull {
        when (it) {
            is IdentifierNode -> it.string
            is EmptyNode -> null
            else -> throw IllegalArgumentException("Invalid argument: $it")
        }
    }
    val newFrame = Frame(this)
    val argumentsVariableIndex = newFrame.defineVariable("__")
    val variableIndices = variables.map { newFrame.defineVariable(it) }
    val getter = newFrame.compileToGetter(body)
    val getter2 = FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
    return NewEnvironmentGetter(newFrame.nextVariableIndex, newFrame.mountCount, getter2)
}

private fun Frame.compileInfixOperatorToGetter(node: InfixNode): Getter {
    return when (node) {
        is InfixPeriodNode -> {
            val receiverGetter = compileToGetter(node.left)
            val nameGetter = when (node.right) {
                is IdentifierNode -> LiteralGetter(FluoriteString(node.right.string))
                else -> compileToGetter(node.right)
            }
            ItemAccessGetter(receiverGetter, nameGetter, false)
        }

        is InfixQuestionPeriodNode -> {
            val receiverGetter = compileToGetter(node.left)
            val nameGetter = when (node.right) {
                is IdentifierNode -> LiteralGetter(FluoriteString(node.right.string))
                else -> compileToGetter(node.right)
            }
            ItemAccessGetter(receiverGetter, nameGetter, true)
        }

        is InfixColonColonNode -> compileToMethodAccessGetter(node.left, node.right, listOf(), true, false)
        is InfixQuestionColonColonNode -> compileToMethodAccessGetter(node.left, node.right, listOf(), true, true)

        is InfixPlusNode -> PlusGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixAmpersandNode -> StringConcatenationGetter(listOf(ConversionStringGetter(compileToGetter(node.left)), ConversionStringGetter(compileToGetter(node.right))))
        is InfixMinusNode -> MinusGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixAsteriskNode -> TimesGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixSlashNode -> DivGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixPercentPercentNode -> DivisibleGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixPercentNode -> ModGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixCircumflexNode -> PowerGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixPeriodPeriodNode -> RangeGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixLessEqualGreaterNode -> SpaceshipGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixTildeNode -> ExclusiveRangeGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixAmpersandAmpersandNode -> AndGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixPipePipeNode -> OrGetter(compileToGetter(node.left), compileToGetter(node.right))
        is InfixQuestionColonNode -> ElvisGetter(compileToGetter(node.left), compileToGetter(node.right))

        is InfixExclamationQuestionNode -> {
            val (name, rightNode) = if (node.right is BracketsLiteralArrowedRoundNode) {
                require(node.right.arguments is IdentifierNode)
                Pair(node.right.arguments.string, node.right.body)
            } else {
                Pair(null, node.right)
            }
            if (name != null) {
                val newFrame = Frame(this)
                val argumentVariableIndex = newFrame.defineVariable(name)
                TryCatchWithVariableGetter(compileToGetter(node.left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToGetter(rightNode))
            } else {
                TryCatchGetter(compileToGetter(node.left), compileToGetter(rightNode))
            }
        }

        is InfixColonNode -> {
            val leftGetter = when (node.left) {
                is IdentifierNode -> LiteralGetter(FluoriteString(node.left.string))
                else -> compileToGetter(node.left)
            }
            EntryGetter(leftGetter, compileToGetter(node.right))
        }

        is InfixEqualNode -> {
            val setter = compileToSetter(node.left)
            val getter = compileToGetter(node.right)
            AssignmentGetter(setter, getter)
        }

        is InfixMinusGreaterNode -> {
            val commasNode = if (node.left is BracketsLiteralSimpleRoundNode) {
                node.left.body
            } else {
                node.left
            }
            val identifierNodes = when (commasNode) {
                is EmptyNode -> listOf()
                is CommasNode -> commasNode.nodes
                is SemicolonsNode -> commasNode.nodes
                else -> listOf(commasNode)
            }
            val variables = identifierNodes.mapNotNull {
                when (it) {
                    is IdentifierNode -> it.string
                    is EmptyNode -> null
                    else -> throw IllegalArgumentException("Invalid argument: $it")
                }
            }
            val newFrame = Frame(this)
            val argumentsVariableIndex = newFrame.defineVariable("__")
            val variableIndices = variables.map { newFrame.defineVariable(it) }
            val getter = newFrame.compileToGetter(node.right)
            FunctionGetter(newFrame.frameIndex, argumentsVariableIndex, variableIndices, getter)
        }

        is InfixPipeNode -> {
            val streamGetter = compileToGetter(node.left)
            val (variable, contentNode) = if (node.right is InfixEqualGreaterNode) {
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

        is InfixGreaterGreaterNode -> {
            val valueGetter = compileToGetter(node.left)
            val functionGetter = compileToGetter(node.right)
            FunctionInvocationGetter(functionGetter, listOf(valueGetter))
        }

        is InfixLessLessNode -> {
            val valueGetter = compileToGetter(node.right)
            val functionGetter = compileToGetter(node.left)
            FunctionInvocationGetter(functionGetter, listOf(valueGetter))
        }

        else -> throw IllegalArgumentException("Unknown operator: A ${node.operator.text} B")
    }
}
