package mirrg.fluorite12

import kotlinx.coroutines.flow.FlowCollector


interface FluoriteValue {
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
}


class FluoriteInt(override val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_JSON" to FluoriteFunction { "${(it[0] as FluoriteInt).value}".toFluoriteString() },
                )
            )
        }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteInt && value == other.value
    override fun hashCode() = value
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteInt(-value)
}


class FluoriteDouble(override val value: Double) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_JSON" to FluoriteFunction { "${(it[0] as FluoriteDouble).value}".toFluoriteString() },
                )
            )
        }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteDouble && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteDouble(-value)
}


enum class FluoriteBoolean(val value: Boolean) : FluoriteValue {
    TRUE(true),
    FALSE(false),
    ;

    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
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


class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_JSON" to FluoriteFunction {
                        val escaped = (it[0] as FluoriteString).value.escapeJsonString()
                        "\"$escaped\"".toFluoriteString()
                    },
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


class FluoriteArray(val values: List<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "TO_JSON" to FluoriteFunction {
                        val sb = StringBuilder()
                        sb.append('[')
                        (it[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(',')
                            sb.append((value.toJson() as FluoriteString).value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
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
                    "TO_JSON" to FluoriteFunction {
                        val sb = StringBuilder()
                        sb.append('{')
                        (it[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
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
                )
            )
        }
    }

    override fun toString() = "{${map.entries.joinToString(";") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend (List<FluoriteValue>) -> FluoriteValue) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy { FluoriteObject(FluoriteValue.fluoriteClass, mutableMapOf()) }
    }

    override val parent get() = fluoriteClass
}


class FluoriteStream(val flowProvider: suspend FlowCollector<FluoriteValue>.() -> Unit) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy { FluoriteObject(FluoriteValue.fluoriteClass, mutableMapOf()) }
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
