package mirrg.fluorite12.compilers

import mirrg.fluorite12.EmptyNode
import mirrg.fluorite12.Frame
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.InfixNode
import mirrg.fluorite12.InfixOperatorType
import mirrg.fluorite12.LeftNode
import mirrg.fluorite12.Node
import mirrg.fluorite12.SemicolonsNode
import mirrg.fluorite12.UnaryOperatorType
import mirrg.fluorite12.defineVariable
import mirrg.fluorite12.mount
import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.GetterRunner
import mirrg.fluorite12.operations.MountRunner
import mirrg.fluorite12.operations.Runner
import mirrg.fluorite12.operations.TryCatchRunner
import mirrg.fluorite12.operations.VariableSetter

fun Frame.compileToRunner(node: Node): List<Runner> {
    return when {
        node is EmptyNode -> listOf()

        node is InfixNode && node.type == InfixOperatorType.EQUAL -> { // 代入文
            val setter = compileToSetter(node.left)
            val getter = compileToGetter(node.right)
            listOf(AssignmentRunner(setter, getter))
        }

        node is InfixNode && node.type == InfixOperatorType.COLON_EQUAL -> when { // 宣言文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variableIndex = defineVariable(name)
                listOf(AssignmentRunner(VariableSetter(frameIndex, variableIndex), compileToGetter(node.right)))
            }

            else -> throw IllegalArgumentException("Illegal definition: ${node.left::class} := ${node.right::class}")
        }

        node is InfixNode && node.type == InfixOperatorType.EXCLAMATION_QUESTION -> {
            val (name, rightNode) = if (node.right is InfixNode && node.right.type == InfixOperatorType.EQUAL_GREATER) {
                require(node.right.left is IdentifierNode)
                Pair(node.right.left.string, node.right.right)
            } else {
                Pair("_", node.right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(name)
            listOf(TryCatchRunner(compileToRunner(node.left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToRunner(rightNode)))
        }

        node is LeftNode && node.type == UnaryOperatorType.AT -> {
            val getter = compileToGetter(node.right)
            val newMountIndex = mount()
            listOf(MountRunner(frameIndex, newMountIndex, getter))
        }

        node is SemicolonsNode -> node.nodes.flatMap { compileToRunner(it) }

        else -> listOf(GetterRunner(compileToGetter(node))) // 式文
    }
}
