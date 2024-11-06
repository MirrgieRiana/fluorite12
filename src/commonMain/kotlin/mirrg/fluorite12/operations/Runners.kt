package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteInt
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.collect

class GetterRunner(private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        val result = getter.evaluate(env)
        if (result is FluoriteStream) {
            result.collect {
                // イテレーションは行うがその結果は握りつぶす
            }
        }
    }

    override val code get() = "Getter[${getter.code}]"
}

class AssignmentRunner(private val frameIndex: Int, private val variableIndex: Int, private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        env.variableTable[frameIndex][variableIndex] = getter.evaluate(env)
    }

    override val code get() = "Assignment[$frameIndex;$variableIndex;${getter.code}]"
}

class ArrayItemAssignmentRunner(private val arrayGetter: Getter, private val indexGetter: Getter, private val valueGetter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        val array = arrayGetter.evaluate(env) as FluoriteArray
        val index = (indexGetter.evaluate(env) as FluoriteInt).value
        val value = valueGetter.evaluate(env)
        if (value is FluoriteStream) throw IllegalArgumentException("Stream assignment is not supported")
        array.values[index] = value
    }

    override val code get() = "ArrayItemAssignment[${arrayGetter.code};${indexGetter.code};${valueGetter.code}]"
}
