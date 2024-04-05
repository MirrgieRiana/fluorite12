package mirrg.fluorite12

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


interface Getter {
    suspend fun evaluate(env: Environment): FluoriteValue
}

object NullGetter : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteNull
}

class LiteralGetter(private val value: FluoriteValue) : Getter {
    override suspend fun evaluate(env: Environment) = value
}

class VariableGetter(private val frameIndex: Int, private val variableIndex: Int) : Getter {
    override suspend fun evaluate(env: Environment) = env.variableTable[frameIndex][variableIndex]
}

class StringConcatenationGetter(private val stringGetters: List<StringGetter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val sb = StringBuilder()
        stringGetters.forEach {
            sb.append(it.evaluate(env))
        }
        return sb.toString().toFluoriteString()
    }
}

class NewEnvironmentGetter(private val variableCount: Int, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(Environment(env, variableCount))
}

class LinesGetter(private val runners: List<Runner>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        runners.forEach {
            it.evaluate(env)
        }
        return getter.evaluate(env)
    }
}

class ArrayCreationGetter(private val getters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val values = mutableListOf<FluoriteValue>()
        getters.forEach {
            val value = it.evaluate(env)
            when (value) {
                is FluoriteStream -> {
                    value.collect { item ->
                        values += item
                    }
                }

                else -> values += value
            }
        }
        return FluoriteArray(values)
    }
}

class ObjectCreationGetter(private val parentGetter: Getter?, private val contentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val parent = parentGetter?.let { it.evaluate(env) as FluoriteObject } ?: FluoriteObject.fluoriteClass
        val map = mutableMapOf<String, FluoriteValue>()
        contentGetters.forEach {
            val value = it.evaluate(env)
            when (value) {
                is FluoriteStream -> {
                    value.collect { item ->
                        require(item is FluoriteArray)
                        require(item.values.size == 2)
                        map[item.values[0].toString()] = item.values[1]
                    }
                }

                else -> {
                    require(value is FluoriteArray)
                    require(value.values.size == 2)
                    map[value.values[0].toString()] = value.values[1]
                }
            }
        }
        return FluoriteObject(parent, map)
    }
}

class MethodInvocationGetter(private val receiverGetter: Getter, private val name: String, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        val arguments = argumentGetters.map { it.evaluate(env) }
        return receiver.callMethod(env, name, *arguments.toTypedArray())
    }
}

class FunctionInvocationGetter(private val functionGetter: Getter, private val argumentGetters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val function = functionGetter.evaluate(env) as FluoriteFunction
        val arguments = argumentGetters.map { it.evaluate(env) }
        return function.function.invoke(arguments)
    }
}

// TODO to method
class ToNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteInt -> value
        is FluoriteDouble -> value
        is FluoriteString -> if ("." in value.value) FluoriteDouble(value.value.toDouble()) else FluoriteInt(value.value.toInt())
        is FluoriteBoolean -> FluoriteInt(if (value.value) 1 else 0)
        else -> throw IllegalArgumentException("Can not convert to number: $value")
    }
}

// TODO to method
class ToNegativeNumberGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteInt -> value
        is FluoriteDouble -> value
        is FluoriteString -> if ("." in value.value) FluoriteDouble(value.value.toDouble()) else FluoriteInt(value.value.toInt())
        is FluoriteBoolean -> FluoriteInt(if (value.value) 1 else 0)
        else -> throw IllegalArgumentException("Can not convert to number: $value")
    }.negate()
}

class ToBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean()
}

class ToNegativeBooleanGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteBoolean().not()
}

// TODO to method
class GetLengthGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = when (val value = getter.evaluate(env)) {
        is FluoriteString -> FluoriteInt(value.value.length)
        is FluoriteArray -> FluoriteInt(value.values.size)
        is FluoriteObject -> FluoriteInt(value.map.size)
        else -> throw IllegalArgumentException("Can not calculate length: $value")
    }
}

class FromJsonGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val value = getter.evaluate(env)
        val data = Json.decodeFromString<JsonElement>((value as FluoriteString).value)
        fun f(data: JsonElement): FluoriteValue = when (data) {
            is JsonObject -> FluoriteObject(FluoriteObject.fluoriteClass, data.mapValues { (_, it) -> f(it) }.toMutableMap())
            is JsonArray -> FluoriteArray(data.map { f(it) })
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
}

class FluoriteException(val value: FluoriteValue) : Exception(value.toString())

class ThrowGetter(private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = throw FluoriteException(getter.evaluate(env))
}

// TODO to method
class ItemAccessGetter(private val receiverGetter: Getter, private val keyGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val receiver = receiverGetter.evaluate(env)
        val key = keyGetter.evaluate(env)
        return when (receiver) {
            is FluoriteString -> receiver.value.getOrNull((key as FluoriteInt).value)?.let { FluoriteString(it.toString()) } ?: FluoriteNull
            is FluoriteArray -> receiver.values.getOrNull((key as FluoriteInt).value) ?: FluoriteNull
            is FluoriteObject -> receiver.map[key.toString()] ?: FluoriteNull
            else -> throw IllegalArgumentException("Unknown operator: ${receiver::class} . ${key::class}")
        }
    }
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

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }
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

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }
}

// TODO to method
class DivGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> FluoriteInt(left.value / right.value)
                is FluoriteDouble -> FluoriteDouble(left.value / right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            is FluoriteDouble -> when (right) {
                is FluoriteInt -> FluoriteDouble(left.value / right.value)
                is FluoriteDouble -> FluoriteDouble(left.value / right.value)
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }
}

// TODO to method
class DivisibleGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        return when (left) {
            is FluoriteInt -> when (right) {
                is FluoriteInt -> (left.value % right.value == 0).toFluoriteBoolean()
                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }
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

                else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
            }

            else -> throw IllegalArgumentException("Can not convert to number: ${left::class}")
        }
    }
}

// TODO to method
class RangeGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        val right = rightGetter.evaluate(env)
        val range = (left as FluoriteInt).value..(right as FluoriteInt).value
        return FluoriteStream {
            range.forEach {
                emit(FluoriteInt(it))
            }
        }
    }
}

class EntryGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = FluoriteArray(listOf(leftGetter.evaluate(env), rightGetter.evaluate(env)))
}

class FunctionGetter(private val newFrameIndex: Int, private val argumentsVariableIndex: Int, private val variableIndices: List<Int>, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val newEnv = Environment(env, 1 + variableIndices.size)
        return FluoriteFunction { arguments ->
            newEnv.variableTable[newFrameIndex][argumentsVariableIndex] = FluoriteArray(arguments)
            variableIndices.forEachIndexed { i, variableIndex ->
                newEnv.variableTable[newFrameIndex][variableIndex] = arguments.getOrNull(i) ?: FluoriteNull
            }
            getter.evaluate(newEnv)
        }
    }
}

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
}

class AndGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (!left.toBoolean(env)) left else rightGetter.evaluate(env)
    }
}

class OrGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left.toBoolean(env)) left else rightGetter.evaluate(env)
    }
}

class IfGetter(private val conditionGetter: Getter, private val okGetter: Getter, private val ngGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment) = if (conditionGetter.evaluate(env).toBoolean()) okGetter.evaluate(env) else ngGetter.evaluate(env)
}

class ElvisGetter(private val leftGetter: Getter, private val rightGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val left = leftGetter.evaluate(env)
        return if (left != FluoriteNull) left else rightGetter.evaluate(env)
    }
}

class StreamConcatenationGetter(private val getters: List<Getter>) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return FluoriteStream {
            getters.forEach {
                when (val value = it.evaluate(env)) {
                    is FluoriteStream -> value.flowProvider(this)
                    else -> emit(value)
                }
            }
        }
    }
}

class AssignmentGetter(private val frameIndex: Int, private val variableIndex: Int, private val getter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val value = getter.evaluate(env)
        env.variableTable[frameIndex][variableIndex] = value
        return value
    }
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
}

class PipeGetter(private val streamGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val contentGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return when (val stream = streamGetter.evaluate(env)) {
            is FluoriteStream -> {
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
            }

            else -> {
                val newEnv = Environment(env, 1)
                newEnv.variableTable[newFrameIndex][argumentVariableIndex] = stream
                contentGetter.evaluate(newEnv)
            }
        }
    }
}

class FilterPipeGetter(private val streamGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val contentGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return when (val stream = streamGetter.evaluate(env)) {
            is FluoriteStream -> {
                FluoriteStream {
                    val newEnv = Environment(env, 1)
                    stream.collect { value ->
                        newEnv.variableTable[newFrameIndex][argumentVariableIndex] = value
                        if (contentGetter.evaluate(newEnv).toBoolean()) {
                            emit(value)
                        }
                    }
                }
            }

            else -> {
                val newEnv = Environment(env, 1)
                newEnv.variableTable[newFrameIndex][argumentVariableIndex] = stream
                if (contentGetter.evaluate(newEnv).toBoolean()) stream else FluoriteStream.EMPTY
            }
        }
    }
}

class NotFilterPipeGetter(private val streamGetter: Getter, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val contentGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        return when (val stream = streamGetter.evaluate(env)) {
            is FluoriteStream -> {
                FluoriteStream {
                    val newEnv = Environment(env, 1)
                    stream.collect { value ->
                        newEnv.variableTable[newFrameIndex][argumentVariableIndex] = value
                        if (!contentGetter.evaluate(newEnv).toBoolean()) {
                            emit(value)
                        }
                    }
                }
            }

            else -> {
                val newEnv = Environment(env, 1)
                newEnv.variableTable[newFrameIndex][argumentVariableIndex] = stream
                if (!contentGetter.evaluate(newEnv).toBoolean()) stream else FluoriteStream.EMPTY
            }
        }
    }
}


interface StringGetter {
    suspend fun evaluate(env: Environment): String
}

class LiteralStringGetter(private val string: String) : StringGetter {
    override suspend fun evaluate(env: Environment) = string
}

class ConversionStringGetter(private val getter: Getter) : StringGetter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteString(env).value
}


interface Runner {
    suspend fun evaluate(env: Environment)
}

class GetterRunner(private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        getter.evaluate(env)
    }
}

class AssignmentRunner(private val frameIndex: Int, private val variableIndex: Int, private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        env.variableTable[frameIndex][variableIndex] = getter.evaluate(env)
    }
}
