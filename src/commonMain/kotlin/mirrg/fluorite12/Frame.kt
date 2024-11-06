package mirrg.fluorite12

import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.Runner


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
