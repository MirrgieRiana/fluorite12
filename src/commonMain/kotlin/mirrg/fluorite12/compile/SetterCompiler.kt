package mirrg.fluorite12.compile

import mirrg.fluorite12.BracketsType
import mirrg.fluorite12.Frame
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.Node
import mirrg.fluorite12.RightBracketsNode
import mirrg.fluorite12.getVariable
import mirrg.fluorite12.operations.ArrayItemSetter
import mirrg.fluorite12.operations.Setter
import mirrg.fluorite12.operations.VariableSetter

fun Frame.compileToSetter(node: Node): Setter {
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
