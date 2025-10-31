package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteRegex
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue

fun createClassMounts(): List<Map<String, FluoriteValue>> {
    return mapOf(
        "VALUE" to FluoriteValue.fluoriteClass,
        "NULL_CLASS" to FluoriteNull.fluoriteClass,
        "INT" to FluoriteInt.fluoriteClass,
        "DOUBLE" to FluoriteDouble.fluoriteClass,
        "BOOLEAN" to FluoriteBoolean.fluoriteClass,
        "STRING" to FluoriteString.fluoriteClass,
        "REGEX" to FluoriteRegex.fluoriteClass,
        "ARRAY" to FluoriteArray.fluoriteClass,
        "OBJECT" to FluoriteObject.fluoriteClass,
        "FUNCTION" to FluoriteFunction.fluoriteClass,
        "STREAM" to FluoriteStream.fluoriteClass,
    ).let { listOf(it) }
}
