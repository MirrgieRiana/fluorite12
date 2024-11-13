package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.FluoriteValue
import mirrg.fluorite12.toFluoriteNumber

class VariableSetter(private val frameIndex: Int, private val variableIndex: Int) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        return {
            env.variableTable[frameIndex][variableIndex] = it
        }
    }

    override val code get() = "Variable[$frameIndex;$variableIndex]"
}

class ArrayItemSetter(private val arrayGetter: Getter, private val indexGetter: Getter) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        val array = arrayGetter.evaluate(env) as FluoriteArray
        val index = indexGetter.evaluate(env).toFluoriteNumber().roundToInt()
        return {
            if (it is FluoriteStream) throw IllegalArgumentException("Stream assignment is not supported")
            array.values[index] = it
        }
    }

    override val code get() = "ArrayItem[${arrayGetter.code};${indexGetter.code}]"
}
