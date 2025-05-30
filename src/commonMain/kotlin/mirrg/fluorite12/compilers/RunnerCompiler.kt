package mirrg.fluorite12.compilers

import mirrg.fluorite12.EmptyNode
import mirrg.fluorite12.Frame
import mirrg.fluorite12.IdentifierNode
import mirrg.fluorite12.InfixColonEqualNode
import mirrg.fluorite12.InfixEqualGreaterNode
import mirrg.fluorite12.InfixEqualNode
import mirrg.fluorite12.InfixExclamationQuestionNode
import mirrg.fluorite12.Node
import mirrg.fluorite12.SemicolonsNode
import mirrg.fluorite12.UnaryAtNode
import mirrg.fluorite12.defineVariable
import mirrg.fluorite12.mount
import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.GetterRunner
import mirrg.fluorite12.operations.MountRunner
import mirrg.fluorite12.operations.Runner
import mirrg.fluorite12.operations.TryCatchRunner
import mirrg.fluorite12.operations.VariableDefinitionSetter

fun Frame.compileToRunner(node: Node): List<Runner> {
    return when (node) {
        is EmptyNode -> listOf()

        is InfixEqualNode -> { // 代入文
            val setter = compileToSetter(node.left)
            val getter = compileToGetter(node.right)
            listOf(AssignmentRunner(setter, getter))
        }

        is InfixColonEqualNode -> when { // 宣言文
            node.left is IdentifierNode -> {
                val name = node.left.string
                val variableIndex = defineVariable(name)
                listOf(AssignmentRunner(VariableDefinitionSetter(frameIndex, variableIndex), compileToGetter(node.right)))
            }

            else -> throw IllegalArgumentException("Illegal definition: ${node.left::class} := ${node.right::class}")
        }

        is InfixExclamationQuestionNode -> {
            val (name, rightNode) = if (node.right is InfixEqualGreaterNode) {
                require(node.right.left is IdentifierNode)
                Pair(node.right.left.string, node.right.right)
            } else {
                Pair("_", node.right)
            }
            val newFrame = Frame(this)
            val argumentVariableIndex = newFrame.defineVariable(name)
            listOf(TryCatchRunner(compileToRunner(node.left), newFrame.frameIndex, argumentVariableIndex, newFrame.compileToRunner(rightNode)))
        }

        is UnaryAtNode -> {
            val getter = compileToGetter(node.main)
            val newMountIndex = mount()
            listOf(MountRunner(frameIndex, newMountIndex, getter))
        }

        is SemicolonsNode -> node.nodes.flatMap { compileToRunner(it) }

        else -> listOf(GetterRunner(compileToGetter(node))) // 式文
    }
}
