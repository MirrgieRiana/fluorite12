package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteBoolean
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.compilers.objects.toMutableList

fun createConvertMounts(): Map<String, FluoriteValue> {
    return mapOf(
        "TO_STRING" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                arguments[0].toFluoriteString()
            } else {
                usage("TO_STRING(value: VALUE): STRING")
            }
        },
        "TO_NUMBER" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                arguments[0].toFluoriteNumber()
            } else {
                usage("TO_NUMBER(value: VALUE): NUMBER")
            }
        },
        "TO_BOOLEAN" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                arguments[0].toFluoriteBoolean()
            } else {
                usage("TO_BOOLEAN(value: VALUE): BOOLEAN")
            }
        },
        "TO_ARRAY" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                val list = if (stream is FluoriteStream) {
                    stream.toMutableList()
                } else {
                    mutableListOf(stream)
                }
                FluoriteArray(list)
            } else {
                usage("ARRAY(stream: STREAM<VALUE>): ARRAY<VALUE>")
            }
        },
        "TO_OBJECT" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                val map = mutableMapOf<String, FluoriteValue>()
                if (stream is FluoriteStream) {
                    stream.collect { item ->
                        require(item is FluoriteArray)
                        require(item.values.size == 2)
                        map[item.values[0].toString()] = item.values[1]
                    }
                } else {
                    require(stream is FluoriteArray)
                    require(stream.values.size == 2)
                    map[stream.values[0].toString()] = stream.values[1]
                }
                FluoriteObject(FluoriteObject.fluoriteClass, map)
            } else {
                usage("OBJECT(stream: STREAM<ARRAY<STRING; VALUE>>): OBJECT")
            }
        },
    )
}
