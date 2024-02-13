package mirrg.fluorite12

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge


class FluoriteArray(val values: List<Any?>) {
    override fun toString() = "[${values.joinToString(",") { "$it" }}]"
}


class FluoriteObject(val map: Map<String, Any?>) {
    override fun toString() = "{${map.entries.joinToString(",") { "${it.key}:${it.value}" }}}"
}


class FluoriteFunction(val function: suspend (List<Any?>) -> Any?)


class FluoriteStream(val flow: Flow<Any?>)

fun streamOf(value: Any?) = FluoriteStream(flowOf(value))

operator fun FluoriteStream.plus(other: FluoriteStream) = FluoriteStream(merge(this.flow, other.flow))

fun Iterable<FluoriteStream>.concat() = FluoriteStream(flow {
    this@concat.forEach {
        emitAll(it.flow)
    }
})
