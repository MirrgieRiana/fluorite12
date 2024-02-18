package mirrg.fluorite12

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

// fluoriteクラスはlazyにしなければJSで初期か順序によるエラーが出る
// https://youtrack.jetbrains.com/issue/KT-25796


val fluoriteValueClass by lazy {
    FluoriteObject(
        null, mutableMapOf(
            "TO_STRING" to FluoriteFunction { "${it[0]}".toFluoriteString() },
        )
    )
}

interface FluoriteValue {
    val parent: FluoriteObject?
}


object FluoriteNull : FluoriteValue {
    val fluoriteNullClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    override val parent = fluoriteNullClass
    override fun toString() = "NULL"
}


interface FluoriteNumber : FluoriteValue {
    val value: Number
    fun negate(): FluoriteNumber
}


class FluoriteInt(override val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteIntClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteInt && value == other.value
    override fun hashCode() = value
    override val parent get() = fluoriteIntClass
    override fun negate() = FluoriteInt(-value)
}


class FluoriteDouble(override val value: Double) : FluoriteNumber {
    companion object {
        val fluoriteDoubleClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteDouble && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteDoubleClass
    override fun negate() = FluoriteDouble(-value)
}


enum class FluoriteBoolean(val value: Boolean) : FluoriteValue {
    TRUE(true),
    FALSE(false),
    ;

    companion object {
        val fluoriteBooleanClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
        fun of(value: Boolean) = if (value) TRUE else FALSE
    }

    override fun toString() = if (value) "TRUE" else "FALSE"
    override val parent get() = fluoriteBooleanClass
    fun not() = if (value) FALSE else TRUE
}


class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteStringClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override fun toString() = value
    override fun equals(other: Any?) = other is FluoriteString && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteStringClass
}

fun String.toFluoriteString() = FluoriteString(this)


class FluoriteArray(val values: List<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteArrayClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override fun toString() = "[${values.joinToString(",") { "$it" }}]"
    override val parent get() = fluoriteArrayClass
}


class FluoriteObject(override val parent: FluoriteObject?, val map: MutableMap<String, FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteObjectClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override fun toString() = "{${map.entries.joinToString(",") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend (List<FluoriteValue>) -> FluoriteValue) : FluoriteValue {
    companion object {
        val fluoriteFunctionClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override val parent get() = fluoriteFunctionClass
}


class FluoriteStream(val flow: Flow<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteStreamClass by lazy { FluoriteObject(fluoriteValueClass, mutableMapOf()) }
    }

    override val parent get() = fluoriteStreamClass
}

fun streamOf(value: FluoriteValue) = FluoriteStream(flowOf(value))

operator fun FluoriteStream.plus(other: FluoriteStream) = FluoriteStream(merge(this.flow, other.flow))

fun Iterable<FluoriteStream>.concat() = FluoriteStream(flow {
    this@concat.forEach {
        emitAll(it.flow)
    }
})
