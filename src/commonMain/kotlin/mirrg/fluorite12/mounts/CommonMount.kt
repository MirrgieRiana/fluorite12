package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteString
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sqrt

private fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun createCommonMount(): Map<String, FluoriteValue> {
    return mapOf(
        "VALUE_CLASS" to FluoriteValue.fluoriteClass,
        "NULL_CLASS" to FluoriteNull.fluoriteClass,
        "INT_CLASS" to FluoriteInt.fluoriteClass,
        "DOUBLE_CLASS" to FluoriteDouble.fluoriteClass,
        "BOOLEAN_CLASS" to FluoriteBoolean.fluoriteClass,
        "STRING_CLASS" to FluoriteString.fluoriteClass,
        "ARRAY_CLASS" to FluoriteArray.fluoriteClass,
        "OBJECT_CLASS" to FluoriteObject.fluoriteClass,
        "FUNCTION_CLASS" to FluoriteFunction.fluoriteClass,
        "STREAM_CLASS" to FluoriteStream.fluoriteClass,

        "NULL" to FluoriteNull,
        "TRUE" to FluoriteBoolean.TRUE,
        "FALSE" to FluoriteBoolean.FALSE,
        "EMPTY" to FluoriteStream.EMPTY,

        "PI" to FluoriteDouble(PI),
        "E" to FluoriteDouble(E),
        "FLOOR" to FluoriteFunction { arguments ->
            when (arguments.size) {
                1 -> when (val number = arguments[0]) {
                    is FluoriteDouble -> FluoriteInt(floor(number.value).toInt())
                    is FluoriteInt -> number
                    else -> usage("FLOOR(number: NUMBER): INTEGER")
                }

                else -> usage("FLOOR(number: NUMBER): INTEGER")
            }
        },
        "SQRT" to FluoriteFunction { arguments ->
            when (arguments.size) {
                1 -> FluoriteDouble(sqrt((arguments[0] as FluoriteNumber).value.toDouble()))
                else -> usage("SQRT(number: NUMBER): NUMBER")
            }
        },
        "ARRAY" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                val list = if (stream is FluoriteStream) {
                    val values = mutableListOf<FluoriteValue>()
                    stream.collect { value ->
                        values.add(value)
                    }
                    values
                } else {
                    mutableListOf(stream)
                }
                FluoriteArray(list)
            } else {
                usage("ARRAY(stream: STREAM<VALUE>): ARRAY<VALUE>")
            }
        },
        "OBJECT" to FluoriteFunction { arguments ->
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
        "REVERSE" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    val values = mutableListOf<FluoriteValue>()
                    stream.collect { value ->
                        values.add(value)
                    }
                    FluoriteStream(values.reversed())
                } else {
                    stream
                }
            } else {
                usage("REVERSE(stream: STREAM<VALUE>): STREAM<VALUE>")
            }
        },
        "JOIN" to FluoriteFunction { arguments ->
            if (arguments.size == 2) {
                val separator = arguments[0].toFluoriteString().value
                val stream = arguments[1]
                if (stream is FluoriteStream) {
                    val sb = StringBuilder()
                    var isFirst = true
                    stream.collect { value ->
                        if (isFirst) {
                            isFirst = false
                        } else {
                            sb.append(separator)
                        }
                        sb.append(value.toFluoriteString().value)
                    }
                    sb.toString().toFluoriteString()
                } else {
                    stream.toFluoriteString()
                }
            } else {
                usage("JOIN(separator: VALUE; stream: VALUE): STRING")
            }
        },
        "SPLIT" to FluoriteFunction { arguments ->
            if (arguments.size == 2) {
                val separator = arguments[0].toFluoriteString().value
                val string = arguments[1]
                if (separator.isEmpty()) {
                    FluoriteStream(string.toFluoriteString().value.map { "$it".toFluoriteString() })
                } else {
                    FluoriteStream(string.toFluoriteString().value.split(separator).map { it.toFluoriteString() })
                }
            } else {
                usage("SPLIT(separator: VALUE; string: VALUE): STREAM<STRING>")
            }
        },
        "KEYS" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val obj = arguments[0]
                if (obj is FluoriteObject) {
                    FluoriteStream(obj.map.keys.map { it.toFluoriteString() })
                } else {
                    usage("KEYS(object: OBJECT): STREAM<STRING>")
                }
            } else {
                usage("KEYS(object: OBJECT): STREAM<STRING>")
            }
        },
        "VALUES" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val obj = arguments[0]
                if (obj is FluoriteObject) {
                    FluoriteStream(obj.map.values)
                } else {
                    usage("VALUES(object: OBJECT): STREAM<VALUE>")
                }
            } else {
                usage("VALUES(object: OBJECT): STREAM<VALUE>")
            }
        },
        "SUM" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    var sum = 0.0
                    stream.collect { value ->
                        sum += (value as FluoriteNumber).value.toDouble()
                    }
                    if (sum.toInt().toDouble() == sum) {
                        FluoriteInt(sum.toInt())
                    } else {
                        FluoriteDouble(sum)
                    }
                } else {
                    stream
                }
            } else {
                usage("SUM(numbers: STREAM<NUMBER>): NUMBER")
            }
        },
        "MIN" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    var result: FluoriteValue? = null
                    stream.collect { item ->
                        val result2 = result
                        if (result2 == null || item.toFluoriteNumber().value.toDouble() < result2.toFluoriteNumber().value.toDouble()) { // TODO
                            result = item
                        }
                    }
                    result ?: FluoriteNull
                } else {
                    stream
                }
            } else {
                usage("MIN(numbers: STREAM<NUMBER>): NUMBER")
            }
        },
        "MAX" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    var result: FluoriteValue? = null
                    stream.collect { item ->
                        val result2 = result
                        if (result2 == null || item.toFluoriteNumber().value.toDouble() > result2.toFluoriteNumber().value.toDouble()) { // TODO
                            result = item
                        }
                    }
                    result ?: FluoriteNull
                } else {
                    stream
                }
            } else {
                usage("MAX(numbers: STREAM<NUMBER>): NUMBER")
            }
        },
    )
}
