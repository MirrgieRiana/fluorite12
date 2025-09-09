package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.LocalVariable
import mirrg.fluorite12.OperatorMethod
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.callMethod
import mirrg.fluorite12.compilers.objects.setInvoke

class VariableSetter(private val frameIndex: Int, private val variableIndex: Int) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        return {
            env.variableTable[frameIndex][variableIndex]!!.set(env, it)
        }
    }

    override val code get() = "VariableSetter[$frameIndex;$variableIndex]"
}

class VariableDefinitionSetter(private val frameIndex: Int, private val variableIndex: Int) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        return {
            env.variableTable[frameIndex][variableIndex] = LocalVariable(it)
        }
    }

    override val code get() = "VariableDefinitionSetter[$frameIndex;$variableIndex]"
}

class ItemAccessSetter(private val receiverGetter: Getter, private val keyGetter: Getter) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        val receiver = receiverGetter.evaluate(env)
        val key = keyGetter.evaluate(env)
        return {
            receiver.callMethod(OperatorMethod.SET_PROPERTY.methodName, arrayOf(key, it))
        }
    }

    override val code get() = "ItemAccessSetter[${receiverGetter.code};${keyGetter.code}]"
}

class FunctionInvocationSetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Setter {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue) -> Unit {
        val function = functionGetter.evaluate(env)
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return { value ->
            function.setInvoke(arguments + value)
        }
    }

    override val code get() = "FunctionInvocationSetter[${functionGetter.code};${argumentGetters.code}]"
}
