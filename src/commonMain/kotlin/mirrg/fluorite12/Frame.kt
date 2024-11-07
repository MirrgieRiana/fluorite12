package mirrg.fluorite12

import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.Runner
import mirrg.fluorite12.operations.VariableSetter


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

fun Frame.defineConstant(name: String, value: FluoriteValue): Runner {
    val variableIndex = defineVariable(name)
    val getter = LiteralGetter(value)
    return AssignmentRunner(VariableSetter(frameIndex, variableIndex), getter)
}
