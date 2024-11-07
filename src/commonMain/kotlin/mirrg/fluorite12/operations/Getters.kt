package mirrg.fluorite12.operations

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mirrg.fluorite12.Environment
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteBoolean
import mirrg.fluorite12.FluoriteDouble
import mirrg.fluorite12.FluoriteFunction
import mirrg.fluorite12.FluoriteInt
import mirrg.fluorite12.FluoriteNull
import mirrg.fluorite12.FluoriteNumber
import mirrg.fluorite12.FluoriteObject
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.FluoriteString
import mirrg.fluorite12.FluoriteValue
import mirrg.fluorite12.call
import mirrg.fluorite12.callMethod
import mirrg.fluorite12.collect
import mirrg.fluorite12.escapeJsonString
import mirrg.fluorite12.toBoolean
import mirrg.fluorite12.toFluoriteBoolean
import mirrg.fluorite12.toFluoriteString
import kotlin.math.pow

object NullGetter : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteNull
    override val code get() = "Null"
}

class LiteralGetter(private val value: FluoriteValue) : Getter {
    override suspend fun evaluate(env: Environment) = value
    override val code get() = "Literal[$value]"
}

class VariableGetter(private val frameIndex: Int, private val variableIndex: Int) : Getter {
    override suspend fun evaluate(env: Environment) = env.variableTable[frameIndex][variableIndex]
    override val code get() = "Variable[$frameIndex;$variableIndex]"
}

class StringConcatenationGetter(private val stringGetters: List<StringGetter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val sb = StringBuilder()
        stringGetters.forEach {
            sb.append(it.evaluate(env))
        }
        return sb.toString().toFluoriteString()
    }

    override val code get() = "StringConcatenation[${stringGetters.code}]"
}

class NewEnvironmentGetter(private val variableCount: Int, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(Environment(env, variableCount))
    override val code get() = "NewEnvironment[$variableCount;${getter.code}]"
}

class LinesGetter(private val runners: List<Runner>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        runners.forEach {
            it.evaluate(env)
        }
        return getter.evaluate(env)
    }

    override val code get() = "Lines[${runners.code};${getter.code}]"
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

    override val code get() = "ArrayCreation[${getters.code}]"
}

class ObjectCreationGetter(private val parentGetter: Getter?, private val contentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val parent = parentGetter?.let { it.evaluate(env) as FluoriteObject } ?: FluoriteObject.fluoriteClass
        val map = mutableMapOf<String, FluoriteValue>()
        contentGetters.forEach {
            val value = it.evaluate(env)
            if (value is FluoriteStream) {
                value.collect { item ->
                    require(item is FluoriteArray)
                    require(item.values.size == 2)
                    map[item.values[0].toString()] = item.values[1]
                }
            } else {
                require(value is FluoriteArray)
                require(value.values.size == 2)
                map[value.values[0].toString()] = value.values[1]
            }
        }
        return FluoriteObject(parent, map)
    }

    override val code get() = "ObjectCreation[${parentGetter?.code};${contentGetters.code}]"
}

class MethodInvocationGetter(private val receiverGetter: Getter, private val name: String, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return receiver.callMethod(name, arguments)
    }

    override val code get() = "MethodInvocation[${receiverGetter.code};${name.escapeJsonString()};${argumentGetters.code}]"
}

class FunctionInvocationGetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val function = functionGetter.evaluate(env) as FluoriteFunction
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return function.call(arguments)
    }

    override val code get() = "FunctionInvocation[${functionGetter.code};${argumentGetters.code}]"
}

class MethodBindGetter(private val receiverGetter: Getter, private val name: String, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
        return FluoriteFunction { arguments2 ->
            receiver.callMethod(name, arguments + arguments2)
        }
    }

    override val code get() = "MethodBind[${receiverGetter.code};${name.escapeJsonString()};${argumentGetters.code}]"
}

class FunctionBindGetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return when (val value = functionGetter.evaluate(env)) {
            is FluoriteFunction -> {
                val arguments = Array(argumentGetters.size) { argumentGetters[it].evaluate(env) }
                return FluoriteFunction { arguments2 ->
                    value.call(arguments + arguments2)
                }
            }

            is FluoriteArray -> when (argumentGetters.size) {
                0 -> FluoriteStream(value.values)
                else -> throw IllegalArgumentException("Element access is not supported") // TODO
            }

            is FluoriteObject -> when (argumentGetters.size) {
                0 -> {
                    FluoriteStream {
                        value.map.entries.forEach {
                            emit(FluoriteArray(mutableListOf(it.key.toFluoriteString(), it.value)))
                        }
                    }
                }

                else -> throw IllegalArgumentException("Element access is not supported") // TODO
            }

            else -> throw IllegalArgumentException("This value does not support element access: $value")
        }
    }

    override val code get() = "FunctionBind[${functionGetter.code};${argumentGetters.code}]"
}

// TODO to method
class ToNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteNull -> FluoriteInt(0)
        is FluoriteInt -> value
        is FluoriteDouble -> value
        is FluoriteString -> if ("." in value.value) FluoriteDouble(value.value.toDouble()) else FluoriteInt(value.value.toInt())
        is FluoriteBoolean -> FluoriteInt(if (value.value) 1 else 0)

        is FluoriteStream -> {
            var v = 0.0
            value.collect { item ->
                v += (item as FluoriteNumber).value.toDouble()
            }
            if (v.toInt().toDouble() == v) {
                FluoriteInt(v.toInt())
            } else {
                FluoriteDouble(v)
            }
        }

        else -> throw IllegalArgumentException("Can not convert to number: $value")
    }

    override val code get() = "ToNumber[${getter.code}]"
}

// TODO to method
class ToNegativeNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteNull -> FluoriteInt(0)
        is FluoriteInt -> value
        is FluoriteDouble -> value
        is FluoriteString -> if ("." in value.value) FluoriteDouble(value.value.toDouble()) else FluoriteInt(value.value.toInt())
        is FluoriteBoolean -> FluoriteInt(if (value.value) 1 else 0)

        is FluoriteStream -> {
            var v = 0.0
            value.collect { item ->
                v += (item as FluoriteNumber).value.toDouble()
            }
            if (v.toInt().toDouble() == v) {
                FluoriteInt(v.toInt())
            } else {
                FluoriteDouble(v)
            }
        }

        else -> throw IllegalArgumentException("Can not convert to number: $value")
    }.negate()

    override val code get() = "ToNegativeNumber[${getter.code}]"
}

class ToBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean()
    override val code get() = "ToBoolean[${getter.code}]"
}

class ToNegativeBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean().not()
    override val code get() = "ToNegativeBoolean[${getter.code}]"
}

// TODO to method
class GetLengthGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteString -> FluoriteInt(value.value.length)
        is FluoriteArray -> FluoriteInt(value.values.size)
        is FluoriteObject -> FluoriteInt(value.map.size)
        else -> throw IllegalArgumentException("Can not calculate length: $value")
    }

    override val code get() = "GetLength[${getter.code}]"
}

class FromJsonGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val value = getter.evaluate(env)
        val data = Json.decodeFromString<JsonElement>((value as FluoriteString).value)
        fun f(data: JsonElement): FluoriteValue = when (data) {
            is JsonObject -> FluoriteObject(FluoriteObject.fluoriteClass, data.mapValues { (_, it) -> f(it) }.toMutableMap())
            is JsonArray -> FluoriteArray(data.map { f(it) }.toMutableList())
            is JsonNull -> FluoriteNull
            is JsonPrimitive -> when {
                data.isString -> data.content.toFluoriteString()
                data.content == "true" -> FluoriteBoolean.TRUE
                data.content == "false" -> FluoriteBoolean.FALSE
                "." !in data.content -> FluoriteInt(data.content.toInt())
                else -> FluoriteDouble(data.content.toDouble())
            }

            else -> throw IllegalArgumentException()
        }
        return f(data)
    }

    override val code get() = "FromJson[${getter.code}]"
}

class FluoriteException(val value: FluoriteValue) : Exception(value.toString())

class ThrowGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = throw FluoriteException(getter.evaluate(env))
    override val code get() = "Throw[${getter.code}]"
}

// TODO to method
class ItemAccessGetter(private val receiverGetter: Getter, private val keyGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        val key = keyGetter.evaluate(env)
        return when (receiver) {
            is FluoriteString -> when (key) {
                is FluoriteInt -> {
                    if (key.value >= 0) {
                        receiver.value.getOrNull(key.value)?.let { FluoriteString(it.toString()) } ?: FluoriteNull
                    } else {
                        receiver.value.getOrNull(receiver.value.length + key.value)?.let { FluoriteString(it.toString()) } ?: FluoriteNull
                    }
                }

                else -> throw IllegalArgumentException("Unknown operator: ${receiver::class} . ${key::class}")
            }

            is FluoriteArray -> when (key) {
                is FluoriteInt -> {
                    if (key.value >= 0) {
                        receiver.values.getOrNull(key.value) ?: FluoriteNull
                    } else {
                        receiver.values.getOrNull(receiver.values.size + key.value) ?: FluoriteNull
                    }
                }

                else -> throw IllegalArgumentException("Unknown operator: ${receiver::class} . ${key::class}")
            }

            is FluoriteObject -> receiver.map[key.toString()] ?: FluoriteNull

            else -> throw IllegalArgumentException("Unknown operator: ${receiver::class} . ${key::class}")
        }
    }

    override val code get() = "ItemAccess[${receiverGetter.code};${keyGetter.code}]"
}

// TODO to method
class PlusGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> FluoriteInt(left.value + right.value)
                is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value + right.value)
                is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteArray -> {
                val list = mutableListOf<FluoriteValue>()
                list += left.values
                list += (right as FluoriteArray).values
                FluoriteArray(list)
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "Plus[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Minus[${leftGetter.code};${rightGetter.code}]"
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

            is FluoriteString -> left.value.repeat((right as FluoriteNumber).value.toInt()).toFluoriteString()

            is FluoriteArray -> {
                val list = mutableListOf<FluoriteValue>()
                repeat((right as FluoriteNumber).value.toInt()) {
                    list += left.values
                }
                FluoriteArray(list)
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }

    override val code get() = "Times[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Div[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Divisible[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Mod[${leftGetter.code};${rightGetter.code}]"
}

class PowerGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env) as FluoriteNumber
        val right = rightGetter.evaluate(env) as FluoriteNumber
        return FluoriteDouble(left.value.toDouble().pow(right.value.toDouble()))
    }

    override val code get() = "Power[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Range[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "Range[${leftGetter.code};${rightGetter.code}]"
}

class EntryGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteArray(mutableListOf(leftGetter.evaluate(env), rightGetter.evaluate(env)))
    override val code get() = "Entry[${leftGetter.code};${rightGetter.code}]"
}

class FunctionGetter(private val newFrameIndex: Int, private val argumentsVariableIndex: Int, private val variableIndices: List<Int>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return FluoriteFunction { arguments ->
            val newEnv = Environment(env, 1 + variableIndices.size)
            newEnv.variableTable[newFrameIndex][argumentsVariableIndex] = FluoriteArray(arguments.toMutableList())
            variableIndices.forEachIndexed { i, variableIndex ->
                newEnv.variableTable[newFrameIndex][variableIndex] = arguments.getOrNull(i) ?: FluoriteNull
            }
            getter.evaluate(newEnv)
        }
    }

    override val code get() = "Function[$newFrameIndex;$argumentsVariableIndex;${variableIndices.joinToString(",") { "$it" }};${getter.code}]"
}

// TODO operatorsのOperation化
class ComparisonChainGetter(private val termGetters: List<Getter>, private val operators: List<suspend (FluoriteValue, FluoriteValue) -> Boolean>) : Getter {
    init {
        require(operators.isNotEmpty())
        require(termGetters.size == operators.size + 1)
    }

    override suspend fun evaluate(env: Environment): FluoriteValue {
        val values = arrayOfNulls<FluoriteValue>(termGetters.size)
        values[0] = termGetters[0].evaluate(env)
        operators.forEachIndexed { i, operator ->
            val leftValue = values[i]!!
            val rightValue = termGetters[i + 1].evaluate(env)
            values[i + 1] = rightValue
            if (!operator(leftValue, rightValue)) return FluoriteBoolean.FALSE
        }
        return FluoriteBoolean.TRUE
    }

    override val code get() = "ComparisonChain[${termGetters.code};${operators.joinToString(",") { it.toString() }}]"
}

class AndGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (!left.toBoolean()) left else rightGetter.evaluate(env)
    }

    override val code get() = "And[${leftGetter.code};${rightGetter.code}]"
}

class OrGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left.toBoolean()) left else rightGetter.evaluate(env)
    }

    override val code get() = "Or[${leftGetter.code};${rightGetter.code}]"
}

class IfGetter(private val conditionGetter: Getter, private val okGetter: Getter, private val ngGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = if (conditionGetter.evaluate(env).toBoolean()) okGetter.evaluate(env) else ngGetter.evaluate(env)
    override val code get() = "If[${conditionGetter.code};${okGetter.code},${ngGetter.code}]"
}

class ElvisGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left != FluoriteNull) left else rightGetter.evaluate(env)
    }

    override val code get() = "Elvis[${leftGetter.code};${rightGetter.code}]"
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

    override val code get() = "StreamConcatenation[${getters.code}]"
}

class AssignmentGetter(private val setter: Setter, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = setter.evaluate(env)
        val right = getter.evaluate(env)
        left.invoke(right)
        return right
    }

    override val code get() = "Assignment[${setter.code};${getter.code}]"
}

class CatchGetter(private val leftGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return try {
            leftGetter.evaluate(env)
        } catch (e: FluoriteException) {
            val newEnv = Environment(env, 1)
            newEnv.variableTable[newFrameIndex][argumentVariableIndex] = e.value
            rightGetter.evaluate(newEnv)
        }
    }

    override val code get() = "Catch[${leftGetter.code};$newFrameIndex;$argumentVariableIndex;${rightGetter.code}]"
}

class PipeGetter(private val streamGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val contentGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val stream = streamGetter.evaluate(env)
        return if (stream is FluoriteStream) {
            FluoriteStream {
                val newEnv = Environment(env, 1)
                stream.collect { value ->
                    newEnv.variableTable[newFrameIndex][argumentVariableIndex] = value
                    val result = contentGetter.evaluate(newEnv)
                    if (result is FluoriteStream) {
                        result.flowProvider(this)
                    } else {
                        emit(result)
                    }
                }
            }
        } else {
            val newEnv = Environment(env, 1)
            newEnv.variableTable[newFrameIndex][argumentVariableIndex] = stream
            contentGetter.evaluate(newEnv)
        }
    }

    override val code get() = "Pipe[${streamGetter.code};$newFrameIndex;$argumentVariableIndex;${contentGetter.code}]"
}
