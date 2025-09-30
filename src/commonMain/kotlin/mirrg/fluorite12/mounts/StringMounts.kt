package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.colon
import mirrg.fluorite12.compilers.objects.fluoriteArrayOf
import mirrg.fluorite12.compilers.objects.toFluoriteString

fun createStringMounts(): List<Map<String, FluoriteValue>> {
    val mounts = mutableMapOf<String, FluoriteValue>()

    FluoriteFunction { arguments ->
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
    }.also {
        mounts["UC"] = it
        mounts["::UC"] = fluoriteArrayOf(
            FluoriteString.fluoriteClass colon it,
        )
    }

    FluoriteFunction { arguments ->
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
    }.also {
        mounts["LC"] = it
        mounts["::LC"] = fluoriteArrayOf(
            FluoriteString.fluoriteClass colon it,
        )
    }

    return listOf(mounts)
}
