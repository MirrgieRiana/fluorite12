package mirrg.fluorite12

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow


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
                "EQUALS" to FluoriteFunction { (it[1] is FluoriteNull).toFluoriteBoolean() },
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
}


class FluoriteInt(override val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteInt
                        val b = it[1] as? FluoriteInt ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.value == b.value).toFluoriteBoolean()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteInt).value != 0).toFluoriteBoolean() },
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
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteDouble
                        val b = it[1] as? FluoriteDouble ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.value == b.value).toFluoriteBoolean()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteDouble).value != 0.0).toFluoriteBoolean() },
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
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteBoolean
                        val b = it[1] as? FluoriteBoolean ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.value == b.value).toFluoriteBoolean()
                    },
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
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteString
                        val b = it[1] as? FluoriteString ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.value == b.value).toFluoriteBoolean()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { ((it[0] as FluoriteString).value != "").toFluoriteBoolean() },
                    "TO_STRING" to FluoriteFunction { it[0] as FluoriteString },
                    "TO_JSON" to FluoriteFunction {
                        val escaped = (it[0] as FluoriteString).value.escapeJsonString()
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


class FluoriteArray(val values: List<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
<<<<<<< Updated upstream
                    "TO_STRING" to FluoriteFunction {
                        val sb = StringBuilder()
                        sb.append('[')
                        (it[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(';')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
=======
                    "EQUALS" to FluoriteFunction { it ->
                        val a = it[0] as FluoriteArray
                        val b = it[1] as? FluoriteArray ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        if (a.values.size != b.values.size) return@FluoriteFunction FluoriteBoolean.FALSE
                        a.values.indices.forEach { i ->
                            if (!a.values[i].fluoriteEquals(b.values[i]).value) return@FluoriteFunction FluoriteBoolean.FALSE
                        }
                        FluoriteBoolean.TRUE
>>>>>>> Stashed changes
                    },
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
                    "CONTAINS" to FluoriteFunction {
                        val array = it[0] as FluoriteArray
                        val targetItem = it[1]
                        array.values.forEach { item ->
                            if (item.fluoriteEquals(targetItem).value) return@FluoriteFunction FluoriteBoolean.TRUE
                        }
                        FluoriteBoolean.FALSE
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
<<<<<<< Updated upstream
                    "TO_STRING" to FluoriteFunction {
                        val sb = StringBuilder()
                        sb.append('{')
                        (it[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
                            if (i != 0) sb.append(';')
                            sb.append(key)
                            sb.append(':')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append('}')
                        sb.toString().toFluoriteString()
=======
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteObject
                        val b = it[1] as? FluoriteObject ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        if (a.map.size != b.map.size) return@FluoriteFunction FluoriteBoolean.FALSE
                        val aIterator = a.map.entries.sortedBy { it.key }.iterator()
                        val bIterator = b.map.entries.sortedBy { it.key }.iterator()
                        repeat(a.map.size) {
                            val aEntry = aIterator.next()
                            val bEntry = bIterator.next()
                            if (aEntry.key != bEntry.key) return@FluoriteFunction FluoriteBoolean.FALSE
                            if (!aEntry.value.fluoriteEquals(bEntry.value).value) return@FluoriteFunction FluoriteBoolean.FALSE
                        }
                        FluoriteBoolean.TRUE
>>>>>>> Stashed changes
                    },
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
                    "CONTAINS" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteObject).map).toFluoriteBoolean() },
                )
            )
        }
    }

    override fun toString() = "{${map.entries.joinToString(";") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend FluoriteFunction.(List<FluoriteValue>) -> FluoriteValue) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "EQUALS" to FluoriteFunction {
                        val a = it[0] as FluoriteFunction
                        val b = it[1] as? FluoriteFunction ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.function == b.function).toFluoriteBoolean()
                    },
                )
            )
        }
    }

    override val parent get() = fluoriteClass
}

suspend fun FluoriteFunction.call(arguments: List<FluoriteValue>) = this.function(this, arguments)


class FluoriteStream(val flowProvider: suspend FlowCollector<FluoriteValue>.() -> Unit) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "EQUALS" to FluoriteFunction {

                        if (it[0] is)

                        val a = it[0] as FluoriteFunction
                        val b = it[1] as? FluoriteFunction ?: return@FluoriteFunction FluoriteBoolean.FALSE
                        (a.function == b.function).toFluoriteBoolean()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { arguments ->
                        flow {
                            (arguments[0] as FluoriteStream).collect {
                                if (it.toBoolean()) emit(FluoriteBoolean.TRUE)
                            }
                            emit(FluoriteBoolean.FALSE)
                        }.first()
                    },
                    "TO_STRING" to FluoriteFunction {
                        val stream = it[0] as FluoriteStream
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
