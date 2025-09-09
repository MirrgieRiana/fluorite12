package mirrg.fluorite12.compilers

import mirrg.fluorite12.BracketsRightSimpleRoundNode
import mirrg.fluorite12.Frame
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.InfixPeriodNode
import mirrg.fluorite12.Node
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.getVariable
import mirrg.fluorite12.operations.FunctionInvocationSetter
import mirrg.fluorite12.operations.ItemAccessSetter
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.Setter
import mirrg.fluorite12.operations.VariableSetter

fun Frame.compileToSetter(node: Node): Setter {
    return when (node) {
        is IdentifierNode -> {
            val name = node.string
            val (frameIndex, variableIndex) = getVariable(name) ?: throw IllegalArgumentException("No such variable: $name")
            VariableSetter(frameIndex, variableIndex)
        }

        is BracketsRightSimpleRoundNode -> FunctionInvocationSetter(compileToGetter(node.receiver), createSimpleArgumentGetters(node))

        is InfixPeriodNode -> {
            val receiverGetter = compileToGetter(node.left)
            val keyGetter = when (node.right) {
                is IdentifierNode -> LiteralGetter(FluoriteString(node.right.string))
                else -> compileToGetter(node.right)
            }
            ItemAccessSetter(receiverGetter, keyGetter)
        }

        else -> throw IllegalArgumentException("Illegal setter: ${node::class}")
    }
}
