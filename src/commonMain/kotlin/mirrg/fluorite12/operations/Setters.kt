package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.LocalVariable
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.callMethod
import mirrg.fluorite12.compilers.objects.toFluoriteNumber

class VariableSetter(private val frameIndex: Int, private val variableIndex: Int) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        return {
            env.variableTable[frameIndex][variableIndex]!!.set(it)
        }
    }

    override val code get() = "Variable[$frameIndex;$variableIndex]"
}

class VariableDefinitionSetter(private val frameIndex: Int, private val variableIndex: Int) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        return {
            env.variableTable[frameIndex][variableIndex] = LocalVariable.of(it)
        }
    }

    override val code get() = "VariableDefinition[$frameIndex;$variableIndex]"
}

class ItemAccessSetter(private val receiverGetter: Getter, private val keyGetter: Getter) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        val receiver = receiverGetter.evaluate(env)
        val key = keyGetter.evaluate(env)
        return {
            receiver.callMethod("_._=", arrayOf(key, it))
        }
    }

    override val code get() = "ItemAccess[${receiverGetter.code};${keyGetter.code}]"
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
