package mirrg.fluorite12

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt


sealed interface FluoriteValue {
    companion object {
        // fluoriteクラスはlazyにしなければJSで初期か順序によるエラーが出る
        // https://youtrack.jetbrains.com/issue/KT-25796
        // 他の同様のプロパティも同じ
        val fluoriteClass by lazy {
            FluoriteObject(
                null, mutableMapOf(
                    "TO_STRING" to FluoriteFunction { "${it[0]}".toFluoriteString() },
                )
            )
        }
    }

    val parent: FluoriteObject?
}


object FluoriteNull : FluoriteValue {
    val fluoriteClass by lazy {
        FluoriteObject(
            FluoriteValue.fluoriteClass, mutableMapOf(
                "TO_NUMBER" to FluoriteFunction { FluoriteInt.ZERO },
                "TO_BOOLEAN" to FluoriteFunction { FluoriteBoolean.FALSE },
                "TO_JSON" to FluoriteFunction { "null".toFluoriteString() },
            )
        )
    }
    override val parent = fluoriteClass
    override fun toString() = "NULL"
}


interface FluoriteNumber : FluoriteValue {
    val value: Number
    fun negate(): FluoriteNumber
    fun roundToInt(): Int
}


class FluoriteInt(override val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_NUMBER" to FluoriteFunction { it[0] as FluoriteInt },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteInt).value != 0).toFluoriteBoolean() },
                    "TO_JSON" to FluoriteFunction { "${(it[0] as FluoriteInt).value}".toFluoriteString() },
                )
            )
        }
        val ZERO = FluoriteInt(0)
        val ONE = FluoriteInt(1)
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteInt && value == other.value
    override fun hashCode() = value
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteInt(-value)
    override fun roundToInt() = value
}


class FluoriteDouble(override val value: Double) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_NUMBER" to FluoriteFunction { it[0] as FluoriteDouble },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteDouble).value != 0.0).toFluoriteBoolean() },
                    "TO_JSON" to FluoriteFunction { "${(it[0] as FluoriteDouble).value}".toFluoriteString() },
                )
            )
        }
        val ZERO = FluoriteDouble(0.0)
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteDouble && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteDouble(-value)
    override fun roundToInt() = value.roundToInt()
}


enum class FluoriteBoolean(val value: Boolean) : FluoriteValue {
    TRUE(true),
    FALSE(false),
    ;

    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_NUMBER" to FluoriteFunction { if ((it[0] as FluoriteBoolean).value) FluoriteInt.ONE else FluoriteInt.ZERO },
                    "TO_BOOLEAN" to FluoriteFunction { it[0] as FluoriteBoolean },
                    "TO_JSON" to FluoriteFunction { (if ((it[0] as FluoriteBoolean).value) "true" else "false").toFluoriteString() },
                )
            )
        }

        fun of(value: Boolean) = if (value) TRUE else FALSE
    }

    override fun toString() = if (value) "TRUE" else "FALSE"
    override val parent get() = fluoriteClass
    fun not() = if (value) FALSE else TRUE
}

fun Boolean.toFluoriteBoolean() = if (this) FluoriteBoolean.TRUE else FluoriteBoolean.FALSE


class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "INVOKE" to FluoriteFunction { arguments ->
                        val string = (arguments[0] as FluoriteString).value
                        when (arguments.size) {
                            1 -> {
                                FluoriteStream {
                                    string.forEach {
                                        emit(it.toString().toFluoriteString())
                                    }
                                }
                            }

                            2 -> {
                                suspend fun get(index: FluoriteValue) = string.getOrNull(index.toFluoriteNumber().roundToInt()).toString().toFluoriteString() ?: FluoriteNull

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    FluoriteStream {
                                        argument.collect { index ->
                                            emit(get(index))
                                        }
                                    }
                                } else {
                                    get(argument)
                                }
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "TO_NUMBER" to FluoriteFunction { arguments ->
                        val string = (arguments[0] as FluoriteString).value
                        if (string.all { c -> c in '0'..'9' }) {
                            when (val int = string.toInt()) {
                                0 -> FluoriteInt.ZERO
                                1 -> FluoriteInt.ONE
                                else -> FluoriteInt(int)
                            }
                        } else {
                            when (val double = string.toDouble()) {
                                0.0 -> FluoriteDouble.ZERO
                                else -> FluoriteDouble(double)
                            }
                        }
                    },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteString).value != "").toFluoriteBoolean() },
                    "TO_STRING" to FluoriteFunction { it[0] as FluoriteString },
                    "TO_JSON" to FluoriteFunction { arguments ->
                        val escaped = (arguments[0] as FluoriteString).value.escapeJsonString()
                        "\"$escaped\"".toFluoriteString()
                    },
                    "CONTAINS" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteString).value).toFluoriteBoolean() },
                )
            )
        }
    }

    override fun toString() = value
    override fun equals(other: Any?) = other is FluoriteString && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
}

fun String.toFluoriteString() = FluoriteString(this)


class FluoriteArray(val values: MutableList<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "INVOKE" to FluoriteFunction { arguments ->
                        val array = arguments[0] as FluoriteArray
                        when (arguments.size) {
                            1 -> FluoriteStream(array.values)

                            2 -> {
                                suspend fun get(index: FluoriteValue) = array.values.getOrNull(index.toFluoriteNumber().roundToInt()) ?: FluoriteNull

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    FluoriteStream {
                                        argument.collect { index ->
                                            emit(get(index))
                                        }
                                    }
                                } else {
                                    get(argument)
                                }
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "TO_STRING" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('[')
                        (arguments[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(';')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { FluoriteBoolean.TRUE },
                    "TO_JSON" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('[')
                        (arguments[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(',')
                            sb.append((value.toJson() as FluoriteString).value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
                    "CONTAINS" to FluoriteFunction { (it[1] in (it[0] as FluoriteArray).values).toFluoriteBoolean() }, // TODO EQUALSメソッドの使用
                )
            )
        }
    }

    override fun toString() = "[${values.joinToString(";") { "$it" }}]"
    override val parent get() = fluoriteClass
}


class FluoriteObject(override val parent: FluoriteObject?, val map: MutableMap<String, FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "INVOKE" to FluoriteFunction { arguments ->
                        val obj = arguments[0] as FluoriteObject
                        when (arguments.size) {
                            1 -> {
                                FluoriteStream {
                                    obj.map.entries.forEach {
                                        emit(FluoriteArray(mutableListOf(it.key.toFluoriteString(), it.value)))
                                    }
                                }
                            }

                            2 -> {
                                suspend fun get(key: FluoriteValue) = obj.map[key.toFluoriteString().value] ?: FluoriteNull

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    FluoriteStream {
                                        argument.collect { key ->
                                            emit(get(key))
                                        }
                                    }
                                } else {
                                    get(argument)
                                }
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "TO_STRING" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('{')
                        (arguments[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
                            if (i != 0) sb.append(';')
                            sb.append(key)
                            sb.append(':')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append('}')
                        sb.toString().toFluoriteString()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { FluoriteBoolean.TRUE },
                    "TO_JSON" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('{')
                        (arguments[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
                            if (i != 0) sb.append(',')
                            sb.append('"')
                            sb.append(key.escapeJsonString())
                            sb.append('"')
                            sb.append(':')
                            sb.append((value.toJson() as FluoriteString).value)
                        }
                        sb.append('}')
                        sb.toString().toFluoriteString()
                    },
                    "CONTAINS" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteObject).map).toFluoriteBoolean() },
                )
            )
        }
    }

    override fun toString() = "{${map.entries.joinToString(";") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend (Array<FluoriteValue>) -> FluoriteValue) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "INVOKE" to FluoriteFunction { arguments ->
                        (arguments[0] as FluoriteFunction).function(arguments.sliceArray(1 until arguments.size))
                    },
                )
            )
        }
    }

    override val parent get() = fluoriteClass
}


class FluoriteStream(val flowProvider: suspend FlowCollector<FluoriteValue>.() -> Unit) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_NUMBER" to FluoriteFunction { arguments ->
                        var intSum = 0
                        var doubleSum = 0.0
                        (arguments[0] as FluoriteStream).collect { item ->
                            when (val number = item.toFluoriteNumber()) {
                                is FluoriteInt -> intSum += number.value
                                is FluoriteDouble -> doubleSum += number.value
                            }
                        }
                        if (doubleSum == 0.0) FluoriteInt(intSum) else FluoriteDouble(intSum + doubleSum)
                    },
                    "TO_BOOLEAN" to FluoriteFunction { arguments ->
                        flow {
                            (arguments[0] as FluoriteStream).collect {
                                if (it.toBoolean()) emit(FluoriteBoolean.TRUE)
                            }
                            emit(FluoriteBoolean.FALSE)
                        }.first()
                    },
                    "TO_STRING" to FluoriteFunction { arguments ->
                        val stream = arguments[0] as FluoriteStream
                        val sb = StringBuilder()
                        stream.collect { item ->
                            sb.append(item.toFluoriteString().value)
                        }
                        "$sb".toFluoriteString()
                    },
                )
            )
        }
        val EMPTY = FluoriteStream {}
    }

    override val parent get() = fluoriteClass
}

fun FluoriteStream(vararg values: FluoriteValue) = FluoriteStream {
    values.forEach {
        emit(it)
    }
}

fun FluoriteStream(values: Iterable<FluoriteValue>) = FluoriteStream {
    values.forEach {
        emit(it)
    }
}

operator fun FluoriteStream.plus(other: FluoriteStream) = FluoriteStream {
    this@plus.flowProvider(this)
    other.flowProvider(this)
}

fun Iterable<FluoriteStream>.concat() = FluoriteStream {
    this@concat.forEach {
        it.flowProvider(this)
    }
}

// ↓ flowProvider { のように書くとJSでemitが呼び出せないエラーになる
suspend fun FluoriteStream.collect(block: suspend (FluoriteValue) -> Unit) = this.flowProvider(FlowCollector {
    block(it)
})
