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
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteStream
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.compilers.objects.toMutableList
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sqrt

private fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun createCommonMount(): Map<String, FluoriteValue> {
    return mapOf(
        "VALUE" to FluoriteValue.fluoriteClass,
        "NULL_CLASS" to FluoriteNull.fluoriteClass,
        "INT" to FluoriteInt.fluoriteClass,
        "DOUBLE" to FluoriteDouble.fluoriteClass,
        "BOOLEAN" to FluoriteBoolean.fluoriteClass,
        "STRING" to FluoriteString.fluoriteClass,
        "ARRAY" to FluoriteArray.fluoriteClass,
        "OBJECT" to FluoriteObject.fluoriteClass,
        "FUNCTION" to FluoriteFunction.fluoriteClass,
        "STREAM" to FluoriteStream.fluoriteClass,

        "NULL" to FluoriteNull,
        "N" to FluoriteNull,
        "TRUE" to FluoriteBoolean.TRUE,
        "T" to FluoriteBoolean.TRUE,
        "FALSE" to FluoriteBoolean.FALSE,
        "F" to FluoriteBoolean.FALSE,
        "EMPTY" to FluoriteStream.EMPTY,
        "E" to FluoriteStream.EMPTY,

        "MATH" to FluoriteObject(
            FluoriteObject.fluoriteClass, mutableMapOf(
                "PI" to FluoriteDouble(PI),
                "E" to FluoriteDouble(E),
            )
        ),
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
        "REVERSE" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    val list = stream.toMutableList()
                    list.reverse()
                    list.toFluoriteStream()
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
                    string.toFluoriteString().value.map { "$it".toFluoriteString() }.toFluoriteStream()
                } else {
                    string.toFluoriteString().value.split(separator).map { it.toFluoriteString() }.toFluoriteStream()
                }
            } else {
                usage("SPLIT(separator: VALUE; string: VALUE): STREAM<STRING>")
            }
        },
        "KEYS" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val obj = arguments[0]
                if (obj is FluoriteObject) {
                    obj.map.keys.map { it.toFluoriteString() }.toFluoriteStream()
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
                    obj.map.values.toFluoriteStream()
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
        "REDUCE" to FluoriteFunction { arguments ->
            if (arguments.size == 2) {
                val function = arguments[0]
                val stream = arguments[1]
                if (stream is FluoriteStream) {
                    var result: FluoriteValue? = null
                    stream.collect { item ->
                        val result2 = result
                        result = (if (result2 == null) item else function.invoke(arrayOf(result2, item)))
                    }
                    result ?: FluoriteNull
                } else {
                    stream
                }
            } else {
                usage("REDUCE(function: VALUE, VALUE -> VALUE; stream: STREAM<VALUE>): VALUE")
            }
        },
    )
}
