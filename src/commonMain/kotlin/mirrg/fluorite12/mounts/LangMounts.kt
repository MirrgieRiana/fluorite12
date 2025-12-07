package mirrg.fluorite12.mounts

import kotlinx.coroutines.delay
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.invoke

fun createLangMounts(out: suspend (FluoriteValue) -> Unit): List<Map<String, FluoriteValue>> {
    return mapOf(
        "NULL" to FluoriteNull,
        "N" to FluoriteNull,
        "TRUE" to FluoriteBoolean.TRUE,
        "T" to FluoriteBoolean.TRUE,
        "FALSE" to FluoriteBoolean.FALSE,
        "F" to FluoriteBoolean.FALSE,
        "EMPTY" to FluoriteStream.EMPTY,
        "E" to FluoriteStream.EMPTY,
        "LOOP" to FluoriteStream {
            while (true) {
                emit(FluoriteNull)
            }
        },
        "SLEEP" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val time = arguments[0] as FluoriteNumber
                delay(time.toInt().toLong())
                FluoriteNull
            } else {
                usage("SLEEP(milliseconds: NUMBER): NULL")
            }
        },
        "CALL" to FluoriteFunction { arguments ->
            if (arguments.size != 2) usage("CALL(function: FUNCTION; arguments: ARRAY<VALUE>): VALUE")
            val function = arguments[0]
            val argumentsArray = arguments[1] as FluoriteArray
            function.invoke(argumentsArray.values.toTypedArray())
        },
        "GENERATE" to FluoriteFunction { arguments ->
            if (arguments.size != 1) usage("GENERATE(generator: (yield: (value: VALUE) -> NULL) -> NULL | STREAM): STREAM<VALUE>")
            val generator = arguments[0]
            FluoriteStream {
                val yieldFunction = FluoriteFunction { arguments2 ->
                    if (arguments2.size != 1) usage("yield(value: VALUE): NULL")
                    val value = arguments2[0]
                    emit(value)
                    FluoriteNull
                }
                val result = generator.invoke(arrayOf(yieldFunction))
                if (result is FluoriteStream) {
                    result.collect {
                        // イテレーションは行うがその結果は握りつぶす
                    }
                }
            }
        },
        "OUT" to FluoriteFunction { arguments ->
            arguments.forEach {
                if (it is FluoriteStream) {
                    it.collect { item ->
                        out(item)
                    }
                } else {
                    out(it)
                }
            }
            FluoriteNull
        },
    ).let { listOf(it) }
}
