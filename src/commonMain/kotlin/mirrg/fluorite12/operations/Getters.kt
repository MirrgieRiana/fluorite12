package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.LocalVariable
import mirrg.fluorite12.compilers.objects.Callable
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.bind
import mirrg.fluorite12.compilers.objects.callMethod
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.compareTo
import mirrg.fluorite12.compilers.objects.getMethod
import mirrg.fluorite12.compilers.objects.instanceOf
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.compilers.objects.plus
import mirrg.fluorite12.compilers.objects.toBoolean
import mirrg.fluorite12.compilers.objects.toFluoriteBoolean
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.escapeJsonString
import mirrg.fluorite12.getMounts
import mirrg.fluorite12.toFluoriteValueAsJson
import mirrg.fluorite12.toJsonFluoriteValue
import kotlin.math.pow

object NullGetter : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteNull
    override val code get() = "NullGetter"
}

class LiteralGetter(private val value: FluoriteValue) : Getter {
    override suspend fun evaluate(env: Environment) = value
    override val code get() = "LiteralGetter[$value]"
}

class VariableGetter(private val frameIndex: Int, private val variableIndex: Int) : Getter {
    override suspend fun evaluate(env: Environment) = env.variableTable[frameIndex][variableIndex]!!.get(env)
    override val code get() = "VariableGetter[$frameIndex;$variableIndex]"
}

class MountGetter(private val mountCounts: IntArray, private val name: String) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        env.getMounts(name, mountCounts).forEach {
            return it
        }
        throw IllegalArgumentException("No such mount entry: $name")
    }

    override val code get() = "MountGetter[${mountCounts.joinToString(",") { "$it" }};$name]"
}

class StringConcatenationGetter(private val stringGetters: List<StringGetter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val sb = StringBuilder()
        stringGetters.forEach {
            sb.append(it.evaluate(env))
        }
        return sb.toString().toFluoriteString()
    }

    override val code get() = "StringConcatenationGetter[${stringGetters.code}]"
}

class NewEnvironmentGetter(private val variableCount: Int, private val mountCount: Int, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(Environment(env, variableCount, mountCount))
    override val code get() = "NewEnvironmentGetter[$variableCount;$mountCount;${getter.code}]"
}

class LinesGetter(private val runners: List<Runner>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        runners.forEach {
            it.evaluate(env)
        }
        return getter.evaluate(env)
    }

    override val code get() = "LinesGetter[${runners.code};${getter.code}]"
}

class ArrayCreationGetter(private val getters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val values = mutableListOf<FluoriteValue>()
        getters.forEach {
            val value = it.evaluate(env)
            if (value is FluoriteStream) {
                value.collect { item ->
                    values += item
                }
            } else {
                values += value
            }
        }
        return FluoriteArray(values)
    }

    override val code get() = "ArrayCreationGetter[${getters.code}]"
}

class ObjectCreationGetter(private val parentGetter: Getter?, private val variableCount: Int, private val objectInitializers: List<ObjectInitializer>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val parent = parentGetter?.let { it.evaluate(env) as FluoriteObject } ?: FluoriteObject.fluoriteClass
        val newEnv = Environment(env, variableCount, 0)
        val map = mutableMapOf<String, FluoriteValue>()
        objectInitializers.forEach {
            it.initializeVariable(newEnv, map)
        }
        objectInitializers.forEach {
            it.evaluate(newEnv, map)
        }
        return FluoriteObject(parent, map)
    }

    override val code get() = "ObjectCreationGetter[${parentGetter?.code};$variableCount;${objectInitializers.code}]"
}

class MethodAccessGetter(
    private val receiverGetter: Getter,
    private val variable: Pair<Int, Int>?,
    private val mountCounts: IntArray,
    private val name: String,
    private val argumentGetters: List<Getter>,
    private val isBinding: Boolean,
    private val isNullSafe: Boolean,
) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        if (isNullSafe && receiver == FluoriteNull) {
            return if (isBinding) {
                FluoriteFunction { arguments2 ->
                    FluoriteNull
                }
            } else {
                FluoriteNull
            }
        }

        suspend fun processFunction(function: FluoriteValue): FluoriteValue {
            val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
            return if (isBinding) {
                FluoriteFunction { arguments2 ->
                    receiver.callMethod(function, arguments + arguments2)
                }
            } else {
                receiver.callMethod(function, arguments)
            }
        }

        suspend fun processCallable(callable: Callable): FluoriteValue {
            val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
            return if (isBinding) {
                FluoriteFunction { arguments2 ->
                    callable.call(arguments + arguments2)
                }
            } else {
                callable.call(arguments)
            }
        }

        suspend fun processEntry(entry: FluoriteArray): FluoriteValue? {
            if (entry.values.size == 2) {
                val clazz = entry.values[0]
                val function = entry.values[1]
                if (receiver.instanceOf(clazz)) {
                    return processFunction(function)
                }
            }
            return null
        }

        suspend fun processEntries(value: FluoriteValue): FluoriteValue? {
            if (value is FluoriteArray) {
                if (value.values.size >= 1 && value.values[0] is FluoriteArray) {
                    value.values.forEach { item ->
                        if (item is FluoriteArray) {
                            val result = processEntry(item)
                            if (result != null) return result
                        }
                    }
                } else {
                    val result = processEntry(value)
                    if (result != null) return result
                }
            }
            return null
        }

        // ローカル変数のチェック
        if (variable != null) {
            val value = env.variableTable[variable.first][variable.second]!!.get(env)
            val result = processEntries(value)
            if (result != null) return result
        }

        // レシーバのメソッドのチェック
        run {
            val callable = receiver.getMethod(name)
            if (callable != null) return processCallable(callable)
        }

        // マウントのチェック
        env.getMounts("::$name", mountCounts).forEach {
            val result = processEntries(it)
            if (result != null) return result
        }

        throw FluoriteException("Method not found: $receiver::$name".toFluoriteString())
    }

    override val code get() = "MethodAccessGetter[${receiverGetter.code};$variable;${mountCounts.joinToString { "$it" }};${name.escapeJsonString()};${argumentGetters.code};$isBinding,$isNullSafe]"
}

class FunctionInvocationGetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val function = functionGetter.evaluate(env)
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return function.invoke(arguments)
    }

    override val code get() = "FunctionInvocationGetter[${functionGetter.code};${argumentGetters.code}]"
}

class FunctionBindGetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val function = functionGetter.evaluate(env)
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return function.bind(arguments)
    }

    override val code get() = "FunctionBindGetter[${functionGetter.code};${argumentGetters.code}]"
}

class ToNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteNumber()
    override val code get() = "ToNumberGetter[${getter.code}]"
}

class ToNegativeNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteNumber().negate()
    override val code get() = "ToNegativeNumberGetter[${getter.code}]"
}

class ToStringGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteString()
    override val code get() = "ToStringGetter[${getter.code}]"
}

class ToBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean()
    override val code get() = "ToBooleanGetter[${getter.code}]"
}

class ToNegativeBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean().not()
    override val code get() = "ToNegativeBooleanGetter[${getter.code}]"
}

// TODO to method
class GetLengthGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteString -> FluoriteInt(value.value.length)
        is FluoriteArray -> FluoriteInt(value.values.size)
        is FluoriteObject -> FluoriteInt(value.map.size)
        else -> throw IllegalArgumentException("Can not calculate length: $value")
    }

    override val code get() = "GetLengthGetter[${getter.code}]"
}

class ToJsonGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toJsonFluoriteValue()
    override val code get() = "ToJsonGetter[${getter.code}]"
}

class FromJsonGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteValueAsJson()
    override val code get() = "FromJsonGetter[${getter.code}]"
}

class FluoriteException(val value: FluoriteValue) : Exception(value.toString())

class ThrowGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = throw FluoriteException(getter.evaluate(env))
    override val code get() = "ThrowGetter[${getter.code}]"
}

class ItemAccessGetter(private val receiverGetter: Getter, private val keyGetter: Getter, private val isNullSafe: Boolean) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        if (isNullSafe && receiver == FluoriteNull) return FluoriteNull
        val key = keyGetter.evaluate(env)
        return receiver.callMethod("_._", arrayOf(key))
    }

    override val code get() = "ItemAccessGetter[${receiverGetter.code};${keyGetter.code};$isNullSafe]"
}

class PlusGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return left.plus(right)
    }

    override val code get() = "PlusGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class MinusGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> FluoriteInt(left.value - right.value)
                is FluoriteDouble -> FluoriteDouble(left.value - right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value - right.value)
                is FluoriteDouble -> FluoriteDouble(left.value - right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "MinusGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class TimesGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> FluoriteInt(left.value * right.value)
                is FluoriteDouble -> FluoriteDouble(left.value * right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value * right.value)
                is FluoriteDouble -> FluoriteDouble(left.value * right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteString -> left.value.repeat((right as FluoriteNumber).toInt()).toFluoriteString()

            is FluoriteArray -> {
                val list = mutableListOf<FluoriteValue>()
                repeat((right as FluoriteNumber).toInt()) {
                    list += left.values
                }
                FluoriteArray(list)
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "TimesGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class DivGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value.toDouble() / right.value.toDouble())
                is FluoriteDouble -> FluoriteDouble(left.value.toDouble() / right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value / right.value.toDouble())
                is FluoriteDouble -> FluoriteDouble(left.value / right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "DivGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class DivisibleGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> (left.value % right.value == 0).toFluoriteBoolean()
                is FluoriteDouble -> (left.value.toDouble() % right.value == 0.0).toFluoriteBoolean()
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> (left.value % right.value.toDouble() == 0.0).toFluoriteBoolean()
                is FluoriteDouble -> (left.value % right.value == 0.0).toFluoriteBoolean()
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "DivisibleGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class ModGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> {
                    val a = left.value
                    val b = right.value
                    FluoriteInt(if (a >= 0) a % b else (b - 1) + (a + 1) % b)
                }

                is FluoriteDouble -> {
                    val a = left.value
                    val b = right.value
                    FluoriteDouble(if (a >= 0) a % b else (b - 1) + (a + 1) % b)
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> {
                    val a = left.value
                    val b = right.value
                    FluoriteDouble(if (a >= 0) a % b else (b - 1) + (a + 1) % b)
                }

                is FluoriteDouble -> {
                    val a = left.value
                    val b = right.value
                    FluoriteDouble(if (a >= 0) a % b else (b - 1) + (a + 1) % b)
                }

                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "ModGetter[${leftGetter.code};${rightGetter.code}]"
}

class PowerGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env) as FluoriteNumber
        val right = rightGetter.evaluate(env) as FluoriteNumber
        return FluoriteDouble(left.toDouble().pow(right.toDouble()))
    }

    override val code get() = "PowerGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class RangeGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = (leftGetter.evaluate(env) as FluoriteInt).value
        val right = (rightGetter.evaluate(env) as FluoriteInt).value
        return if (left > right) {
            // 下降
            FluoriteStream {
                var i = left
                while (i >= right) {
                    emit(FluoriteInt(i))
                    i--
                }
            }
        } else {
            // 上昇
            FluoriteStream {
                var i = left
                while (i <= right) {
                    emit(FluoriteInt(i))
                    i++
                }
            }
        }
    }

    override val code get() = "RangeGetter[${leftGetter.code};${rightGetter.code}]"
}

// TODO to method
class ExclusiveRangeGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = (leftGetter.evaluate(env) as FluoriteInt).value
        val right = (rightGetter.evaluate(env) as FluoriteInt).value
        return FluoriteStream {
            var i = left
            while (i < right) {
                emit(FluoriteInt(i))
                i++
            }
        }
    }

    override val code get() = "ExclusiveRangeGetter[${leftGetter.code};${rightGetter.code}]"
}

class EntryGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteArray(mutableListOf(leftGetter.evaluate(env), rightGetter.evaluate(env)))
    override val code get() = "EntryGetter[${leftGetter.code};${rightGetter.code}]"
}

class FunctionGetter(private val newFrameIndex: Int, private val argumentsVariableIndex: Int, private val variableIndices: List<Int>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return FluoriteFunction { arguments ->
            val newEnv = Environment(env, 1 + variableIndices.size, 0)
            newEnv.variableTable[newFrameIndex][argumentsVariableIndex] = LocalVariable(FluoriteArray(arguments.toMutableList()))
            variableIndices.forEachIndexed { i, variableIndex ->
                newEnv.variableTable[newFrameIndex][variableIndex] = LocalVariable(arguments.getOrNull(i) ?: FluoriteNull)
            }
            getter.evaluate(newEnv)
        }
    }

    override val code get() = "FunctionGetter[$newFrameIndex;$argumentsVariableIndex;${variableIndices.joinToString(",") { "$it" }};${getter.code}]"
}

class SpaceshipGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return left.compareTo(right)
    }

    override val code get() = "SpaceshipGetter"
}

class ComparisonChainGetter(private val termGetters: List<Getter>, private val comparators: List<Comparator>) : Getter {
    init {
        require(comparators.isNotEmpty())
        require(termGetters.size == comparators.size + 1)
    }

    override suspend fun evaluate(env: Environment): FluoriteValue {
        val values = arrayOfNulls<FluoriteValue>(termGetters.size)
        values[0] = termGetters[0].evaluate(env)
        comparators.forEachIndexed { i, comparator ->
            val leftValue = values[i]!!
            val rightValue = termGetters[i + 1].evaluate(env)
            values[i + 1] = rightValue
            if (!comparator.evaluate(env)(leftValue, rightValue)) return FluoriteBoolean.FALSE
        }
        return FluoriteBoolean.TRUE
    }

    override val code get() = "ComparisonChainGetter[${termGetters.code};${comparators.code}]"
}

class AndGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (!left.toBoolean()) left else rightGetter.evaluate(env)
    }

    override val code get() = "AndGetter[${leftGetter.code};${rightGetter.code}]"
}

class OrGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left.toBoolean()) left else rightGetter.evaluate(env)
    }

    override val code get() = "OrGetter[${leftGetter.code};${rightGetter.code}]"
}

class IfGetter(private val conditionGetter: Getter, private val okGetter: Getter, private val ngGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = if (conditionGetter.evaluate(env).toBoolean()) okGetter.evaluate(env) else ngGetter.evaluate(env)
    override val code get() = "IfGetter[${conditionGetter.code};${okGetter.code},${ngGetter.code}]"
}

class ElvisGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left != FluoriteNull) left else rightGetter.evaluate(env)
    }

    override val code get() = "ElvisGetter[${leftGetter.code};${rightGetter.code}]"
}

class StreamConcatenationGetter(private val getters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return FluoriteStream {
            getters.forEach {
                val value = it.evaluate(env)
                if (value is FluoriteStream) {
                    value.flowProvider(this)
                } else {
                    emit(value)
                }
            }
        }
    }

    override val code get() = "StreamConcatenationGetter[${getters.code}]"
}

class AssignmentGetter(private val setter: Setter, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = setter.evaluate(env)
        val right = getter.evaluate(env)
        left.invoke(right)
        return right
    }

    override val code get() = "AssignmentGetter[${setter.code};${getter.code}]"
}

class TryCatchWithVariableGetter(private val leftGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return try {
            leftGetter.evaluate(env)
        } catch (e: FluoriteException) {
            val newEnv = Environment(env, 1, 0)
            newEnv.variableTable[newFrameIndex][argumentVariableIndex] = LocalVariable(e.value)
            rightGetter.evaluate(newEnv)
        }
    }

    override val code get() = "TryCatchWithVariableGetter[${leftGetter.code};$newFrameIndex;$argumentVariableIndex;${rightGetter.code}]"
}

class TryCatchGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return try {
            leftGetter.evaluate(env)
        } catch (e: FluoriteException) {
            rightGetter.evaluate(env)
        }
    }

    override val code get() = "TryCatchGetter[${leftGetter.code};${rightGetter.code}]"
}

class PipeGetter(private val streamGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val contentGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val stream = streamGetter.evaluate(env)
        return if (stream is FluoriteStream) {
            FluoriteStream {
                val newEnv = Environment(env, 1, 0)
                stream.collect { value ->
                    newEnv.variableTable[newFrameIndex][argumentVariableIndex] = LocalVariable(value)
                    val result = contentGetter.evaluate(newEnv)
                    if (result is FluoriteStream) {
                        result.flowProvider(this)
                    } else {
                        emit(result)
                    }
                }
            }
        } else {
            val newEnv = Environment(env, 1, 0)
            newEnv.variableTable[newFrameIndex][argumentVariableIndex] = LocalVariable(stream)
            contentGetter.evaluate(newEnv)
        }
    }

    override val code get() = "PipeGetter[${streamGetter.code};$newFrameIndex;$argumentVariableIndex;${contentGetter.code}]"
}
