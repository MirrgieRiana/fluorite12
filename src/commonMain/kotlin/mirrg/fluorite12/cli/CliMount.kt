package mirrg.fluorite12.cli

import getEnv
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString

fun createCliMounts(args: List<String>): Map<String, FluoriteValue> {
    return mapOf(
        "ARGS" to FluoriteArray(args.map { it.toFluoriteString() }.toMutableList()),
        "ENV" to FluoriteObject(FluoriteObject.fluoriteClass, getEnv().mapValues { it.value.toFluoriteString() }.toMutableMap()),
        "IN" to FluoriteStream {
            while (true) {
                val line = readlnOrNull() ?: break
                emit(line.toFluoriteString())
            }
        },
        "OUT" to FluoriteFunction { arguments ->
            arguments.forEach {
                if (it is FluoriteStream) {
                    it.collect { item ->
                        println(item.toFluoriteString().value)
                    }
                } else {
                    println(it.toFluoriteString().value)
                }
            }
            FluoriteNull
        },
    )
}
