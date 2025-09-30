package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString

fun createStringMounts(): List<Map<String, FluoriteValue>> {
    return mapOf(
        "UC" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val argument = arguments[0]
                if (argument is FluoriteStream) {
                    FluoriteStream {
                        argument.collect { item ->
                            emit(item.toFluoriteString().value.uppercase().toFluoriteString())
                        }
                    }
                } else {
                    argument.toFluoriteString().value.uppercase().toFluoriteString()
                }
            } else {
                usage("UC(string: STRING): STRING | UC(string: STREAM<STRING>): STREAM<STRING>")
            }
        },
        "LC" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val argument = arguments[0]
                if (argument is FluoriteStream) {
                    FluoriteStream {
                        argument.collect { item ->
                            emit(item.toFluoriteString().value.lowercase().toFluoriteString())
                        }
                    }
                } else {
                    argument.toFluoriteString().value.lowercase().toFluoriteString()
                }
            } else {
                usage("LC(string: STRING): STRING | LC(string: STREAM<STRING>): STREAM<STRING>")
            }
        },
    ).let { listOf(it) }
}
