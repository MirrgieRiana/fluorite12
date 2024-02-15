package mirrg.fluorite12

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge


interface FluoriteValue


object FluoriteNull : FluoriteValue {
    override fun toString() = "NULL"
}


interface FluoriteNumber : FluoriteValue {
    val value: Number
    fun negate(): FluoriteNumber
}

class FluoriteInt(override val value: Int) : FluoriteNumber {
    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteInt && value == other.value
    override fun hashCode() = value
    override fun negate() = FluoriteInt(-value)
}


class FluoriteDouble(override val value: Double) : FluoriteNumber {
    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteDouble && value == other.value
    override fun hashCode() = value.hashCode()
    override fun negate() = FluoriteDouble(-value)
}


enum class FluoriteBoolean(val value: Boolean) : FluoriteValue {
    TRUE(true),
    FALSE(false),
    ;

    companion object {
        fun of(value: Boolean) = if (value) TRUE else FALSE
    }

    override fun toString() = value.toString()
    fun not() = if (value) FALSE else TRUE
}


class FluoriteString(val value: String) : FluoriteValue {
    override fun toString() = value
    override fun equals(other: Any?) = other is FluoriteString && value == other.value
    override fun hashCode() = value.hashCode()
}


class FluoriteArray(val values: List<FluoriteValue>) : FluoriteValue {
    override fun toString() = "[${values.joinToString(",") { "$it" }}]"
}


class FluoriteObject(val map: Map<String, FluoriteValue>) : FluoriteValue {
    override fun toString() = "{${map.entries.joinToString(",") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend (List<FluoriteValue>) -> FluoriteValue) : FluoriteValue


class FluoriteStream(val flow: Flow<FluoriteValue>) : FluoriteValue

fun streamOf(value: FluoriteValue) = FluoriteStream(flowOf(value))

operator fun FluoriteStream.plus(other: FluoriteStream) = FluoriteStream(merge(this.flow, other.flow))

fun Iterable<FluoriteStream>.concat() = FluoriteStream(flow {
    this@concat.forEach {
        emitAll(it.flow)
    }
})
