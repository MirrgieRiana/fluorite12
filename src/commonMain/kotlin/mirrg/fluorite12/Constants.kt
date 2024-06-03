package mirrg.fluorite12

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.sqrt

private fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun Frame.defineCommonBuiltinConstants() = listOf(
    defineConstant("VALUE_CLASS", FluoriteValue.fluoriteClass),
    defineConstant("NULL_CLASS", FluoriteNull.fluoriteClass),
    defineConstant("INT_CLASS", FluoriteInt.fluoriteClass),
    defineConstant("DOUBLE_CLASS", FluoriteDouble.fluoriteClass),
    defineConstant("BOOLEAN_CLASS", FluoriteBoolean.fluoriteClass),
    defineConstant("STRING_CLASS", FluoriteString.fluoriteClass),
    defineConstant("ARRAY_CLASS", FluoriteArray.fluoriteClass),
    defineConstant("OBJECT_CLASS", FluoriteObject.fluoriteClass),
    defineConstant("FUNCTION_CLASS", FluoriteFunction.fluoriteClass),
    defineConstant("STREAM_CLASS", FluoriteStream.fluoriteClass),

    defineConstant("NULL", FluoriteNull),
    defineConstant("TRUE", FluoriteBoolean.TRUE),
    defineConstant("FALSE", FluoriteBoolean.FALSE),
    defineConstant("EMPTY", FluoriteStream.EMPTY),

    defineConstant("PI", FluoriteDouble(PI)),
    defineConstant("E", FluoriteDouble(E)),
    defineConstant("SQRT", FluoriteFunction { arguments ->
        when (arguments.size) {
            1 -> FluoriteDouble(sqrt((arguments[0] as FluoriteNumber).value.toDouble()))
            else -> throw IllegalArgumentException("Arguments mismatch: SQRT[${arguments.size}]")
        }
    }),
    defineConstant("ARRAY", FluoriteFunction { arguments ->
        if (arguments.size == 1) {
            val stream = arguments[0]
            val list = if (stream is FluoriteStream) {
                val values = mutableListOf<FluoriteValue>()
                stream.collect { value ->
                    values.add(value)
                }
                values
            } else {
                listOf(stream)
            }
            FluoriteArray(list)
        } else {
            usage("ARRAY(stream: STREAM<VALUE>): ARRAY")
        }
    }),
    defineConstant("OBJECT", FluoriteFunction { arguments ->
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
            usage("OBJECT(stream: STREAM<ARRAY<STRING, VALUE>>): OBJECT")
        }
    }),
    defineConstant("JOIN", FluoriteFunction { arguments ->
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
    }),
    defineConstant("SPLIT", FluoriteFunction { arguments ->
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
    }),
    defineConstant("KEYS", FluoriteFunction { arguments ->
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
    }),
    defineConstant("VALUES", FluoriteFunction { arguments ->
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
    }),
)
