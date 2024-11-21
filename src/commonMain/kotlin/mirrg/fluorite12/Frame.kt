package mirrg.fluorite12

import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.operations.AdditiveBuiltinMountRunner
import mirrg.fluorite12.operations.AssignmentRunner
import mirrg.fluorite12.operations.BuiltinMountRunner
import mirrg.fluorite12.operations.LiteralGetter
import mirrg.fluorite12.operations.Runner
import mirrg.fluorite12.operations.VariableSetter


class Frame(val parent: Frame? = null) {
    val frameIndex: Int = parent?.let { it.frameIndex + 1 } ?: 0
    val variableIndexTable = mutableMapOf<String, Int>()
    var nextVariableIndex = 0
    var mountCount = 0
}

class Environment(val parent: Environment?, variableCount: Int, mountCount: Int) {
    val variableTable: Array<Array<FluoriteValue>> = if (parent != null) {
        arrayOf(*parent.variableTable, Array(variableCount) { FluoriteNull })
    } else {
        arrayOf(Array(variableCount) { FluoriteNull })
    }
    val mountTable: Array<Array<Map<String, FluoriteValue>?>> = if (parent != null) {
        arrayOf(*parent.mountTable, Array(mountCount) { null })
    } else {
        arrayOf(Array(mountCount) { null })
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

fun Frame.mount(): Int {
    val mountIndex = mountCount
    mountCount++
    return mountIndex
}

fun Frame.getMount(): Pair<Int, Int>? {
    var currentFrame = this
    while (true) {
        if (currentFrame.mountCount > 0) {
            val mountIndex = currentFrame.mountCount - 1
            return Pair(currentFrame.frameIndex, mountIndex)
        }
        currentFrame = currentFrame.parent ?: return null
    }
}

fun Frame.defineBuiltinMount(map: Map<String, FluoriteValue>): Runner {
    val oldMount = getMount()
    val newMountIndex = mount()
    return if (oldMount != null) {
        AdditiveBuiltinMountRunner(oldMount.first, oldMount.second, frameIndex, newMountIndex, map)
    } else {
        BuiltinMountRunner(frameIndex, newMountIndex, map)
    }
}
