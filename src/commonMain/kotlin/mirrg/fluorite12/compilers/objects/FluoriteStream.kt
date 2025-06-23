package mirrg.fluorite12.compilers.objects

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import mirrg.fluorite12.OperatorMethod

class FluoriteStream(val flowProvider: suspend FlowCollector<FluoriteValue>.() -> Unit) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    OperatorMethod.TO_NUMBER.methodName to FluoriteFunction { arguments ->
                        var sum: FluoriteValue? = null
                        (arguments[0] as FluoriteStream).collect { item ->
                            val number = item.toFluoriteNumber()
                            sum = sum?.callMethod(OperatorMethod.PLUS.methodName, arrayOf(number)) ?: number
                        }
                        sum ?: FluoriteInt.ZERO
                    },
                    OperatorMethod.TO_BOOLEAN.methodName to FluoriteFunction { arguments ->
                        flow {
                            (arguments[0] as FluoriteStream).collect {
                                if (it.toBoolean()) emit(FluoriteBoolean.TRUE)
                            }
                            emit(FluoriteBoolean.FALSE)
                        }.first()
                    },
                    OperatorMethod.TO_STRING.methodName to FluoriteFunction { arguments ->
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

fun Iterable<FluoriteValue>.toFluoriteStream() = FluoriteStream(this)

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

suspend fun FluoriteStream.toMutableList(): MutableList<FluoriteValue> {
    val list = mutableListOf<FluoriteValue>()
    this.collect {
        list.add(it)
    }
    return list
}
