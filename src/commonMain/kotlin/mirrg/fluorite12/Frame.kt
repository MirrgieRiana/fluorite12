package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import mirrg.fluorite12.compilers.compileToGetter
import mirrg.fluorite12.compilers.compileToRunner
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.operations.BuiltinMountRunner
import mirrg.fluorite12.operations.Runner


class Frame(val parent: Frame? = null) {
    val frameIndex: Int = parent?.let { it.frameIndex + 1 } ?: 0
    val variableIndexTable = mutableMapOf<String, Int>()
    var nextVariableIndex = 0
    var mountCount = 0
}

class Environment(val parent: Environment?, variableCount: Int, mountCount: Int) {
    val variableTable: Array<Array<Variable?>> = if (parent != null) {
        arrayOf(*parent.variableTable, arrayOfNulls(variableCount))
    } else {
        arrayOf(arrayOfNulls(variableCount))
    }
    val mountTable: Array<Array<Map<String, FluoriteValue>>> = if (parent != null) {
        arrayOf(*parent.mountTable, Array(mountCount) { mapOf() })
    } else {
        arrayOf(Array(mountCount) { mapOf() })
    }
}

interface Variable {
    fun get(): FluoriteValue
    fun set(value: FluoriteValue)
}

class LocalVariable : Variable {
    companion object {
        fun of(value: FluoriteValue): LocalVariable {
            return LocalVariable().apply {
                this.value = value
            }
        }
    }

    private var value: FluoriteValue = FluoriteNull

    override fun get() = value

    override fun set(value: FluoriteValue) {
        this.value = value
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

fun Frame.mount(): Int {
    val mountIndex = mountCount
    mountCount++
    return mountIndex
}

fun Frame.getMountCounts(): IntArray {
    val mountCounts = mutableListOf<Int>()
    var frame = this
    while (true) {
        mountCounts += frame.mountCount
        frame = frame.parent ?: break
    }
    return mountCounts.reversed().toIntArray()
}

fun Frame.defineBuiltinMount(map: Map<String, FluoriteValue>): Runner {
    val newMountIndex = mount()
    return BuiltinMountRunner(frameIndex, newMountIndex, map)
}

fun Environment.getMounts(name: String, mountCounts: IntArray): Sequence<FluoriteValue> {
    return sequence {
        var currentFrameIndex = mountCounts.size - 1
        while (currentFrameIndex >= 0) {

            var currentMountIndex = mountCounts[currentFrameIndex] - 1
            while (currentMountIndex >= 0) {

                val value = this@getMounts.mountTable[currentFrameIndex][currentMountIndex][name]
                if (value != null) yield(value)

                currentMountIndex--
            }

            currentFrameIndex--
        }
    }
}


class Evaluator {

    private var currentFrame: Frame? = null
    private var currentEnv: Environment? = null

    suspend fun defineMounts(maps: List<Map<String, FluoriteValue>>) {
        val frame = Frame(currentFrame)
        currentFrame = frame
        val runners = maps.map {
            frame.defineBuiltinMount(it)
        }
        val env = Environment(currentEnv, frame.nextVariableIndex, frame.mountCount)
        currentEnv = env
        runners.forEach {
            it.evaluate(env)
        }
    }

    suspend fun get(src: String): FluoriteValue {
        val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
        val frame = Frame(currentFrame)
        currentFrame = frame
        val getter = frame.compileToGetter(parseResult.value)
        val env = Environment(currentEnv, frame.nextVariableIndex, frame.mountCount)
        currentEnv = env
        return getter.evaluate(env)
    }

    suspend fun run(src: String) {
        val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
        val frame = Frame(currentFrame)
        currentFrame = frame
        val runners = frame.compileToRunner(parseResult.value)
        val env = Environment(currentEnv, frame.nextVariableIndex, frame.mountCount)
        currentEnv = env
        runners.forEach {
            it.evaluate(env)
        }
    }

}
