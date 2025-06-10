package mirrg.fluorite12.mounts

import kotlinx.coroutines.delay
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
import mirrg.fluorite12.compilers.objects.compareTo
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteStream
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.compilers.objects.toMutableList
import mirrg.fluorite12.pop
import mirrg.fluorite12.toFluoriteValueAsJson
import mirrg.fluorite12.toJsonFluoriteValue
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
                1 -> FluoriteDouble(sqrt((arguments[0] as FluoriteNumber).toDouble()))
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
        "DISTINCT" to FluoriteFunction { arguments ->
            if (arguments.size == 1) {
                val stream = arguments[0]
                if (stream is FluoriteStream) {
                    FluoriteStream {
                        val set = mutableSetOf<FluoriteValue>()
                        stream.collect { item ->
                            if (set.add(item)) emit(item)
                        }
                    }
                } else {
                    stream
                }
            } else {
                usage("DISTINCT(stream: STREAM<VALUE>): STREAM<VALUE>")
            }
        },
        "JOIN" to FluoriteFunction { arguments ->
            val separator: String
            val stream: FluoriteValue
            when (arguments.size) {
                2 -> {
                    separator = arguments[0].toFluoriteString().value
                    stream = arguments[1]
                }

                1 -> {
                    separator = ","
                    stream = arguments[0]
                }

                else -> usage("JOIN([separator: STRING; ]stream: STREAM<STRING>): STRING")
            }

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
        },
        "SPLIT" to FluoriteFunction { arguments ->
            val separator: String
            val string: FluoriteValue
            when (arguments.size) {
                2 -> {
                    separator = arguments[0].toFluoriteString().value
                    string = arguments[1]
                }

                1 -> {
                    separator = ","
                    string = arguments[0]
                }

                else -> usage("SPLIT([separator: STRING; ]string: STRING): STREAM<STRING>")
            }

            if (separator.isEmpty()) {
                string.toFluoriteString().value.map { "$it".toFluoriteString() }.toFluoriteStream()
            } else {
                string.toFluoriteString().value.split(separator).map { it.toFluoriteString() }.toFluoriteStream()
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
                        sum += (value as FluoriteNumber).toDouble()
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
                        if (result2 == null || item.compareTo(result2).value < 0) {
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
                        if (result2 == null || item.compareTo(result2).value > 0) {
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
        "SORT" to createSortFunction("SORT", false),
        "SORTR" to createSortFunction("SORTR", true),
        "CHUNK" to FluoriteFunction { arguments ->
            if (arguments.size == 2) {
                val size = arguments[0].toFluoriteNumber().toInt()
                require(size > 0)
                val stream = arguments[1]
                FluoriteStream {
                    var buffer = mutableListOf<FluoriteValue>()
                    if (stream is FluoriteStream) {
                        stream.collect { item ->
                            buffer += item
                            if (buffer.size == size) {
                                emit(FluoriteArray(buffer))
                                buffer = mutableListOf()
                            }
                        }
                    } else {
                        buffer += stream
                    }
                    if (buffer.isNotEmpty()) emit(FluoriteArray(buffer))
                }
            } else {
                usage("CHUNK(size: NUMBER; stream: STREAM<VALUE>): STREAM<ARRAY<VALUE>>")
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
        "JSON" to FluoriteFunction { arguments ->
            fun usage(): Nothing = usage("""JSON(["indent": indent: STRING; ]value: VALUE | STREAM<VALUE>): STRING | STREAM<STRING>""")
            val (indent, value) = when (arguments.size) {
                1 -> Pair(null, arguments[0])
                2 -> {
                    val indentParameter = arguments[0] as? FluoriteArray ?: usage()
                    if (indentParameter.values.size != 2) usage()
                    val indentKey = indentParameter.values[0] as? FluoriteString ?: usage()
                    if (indentKey.value != "indent") usage()
                    Pair(indentParameter.values[1].toFluoriteString().value, arguments[1])
                }

                else -> usage()
            }
            value.toJsonFluoriteValue(indent = indent)
        },
        "JSOND" to FluoriteFunction { arguments ->
            fun usage(): Nothing = usage("JSOND(json: STRING | STREAM<STRING>): VALUE | STREAM<VALUE>")
            if (arguments.size != 1) usage()
            val json = arguments[0]
            json.toFluoriteValueAsJson()
        },
        "CSV" to FluoriteFunction { arguments ->
            fun usage(): Nothing = usage("""CSV(["separator": separator: STRING; ]["quote": quote: STRING; ]value: ARRAY<STRING> | STREAM<ARRAY<STRING>>): STRING | STREAM<STRING>""")
            if (arguments.isEmpty()) usage()
            val parameters = arguments.dropLast(1)
                .associate {
                    val array = it as? FluoriteArray ?: usage()
                    if (array.values.size != 2) usage()
                    val key = array.values[0] as? FluoriteString ?: usage()
                    val value = array.values[1]
                    key.value to value
                }
                .toMutableMap()
            val separator = parameters.pop("separator", {
                val string = it.toFluoriteString().value
                check(string.length == 1)
                string
            }) { "," } // StringでやらないとJSの謎バグでChar同士の比較が成功しない
            val quote = parameters.pop("quote", {
                val string = it.toFluoriteString().value
                check(string.length == 1)
                string
            }) { "\"" } // StringでやらないとJSの謎バグでChar同士の比較が成功しない
            if (parameters.isNotEmpty()) usage()
            val value = arguments.last()

            suspend fun toCsv(value: FluoriteValue): FluoriteString {
                val sb = StringBuilder()
                (value as FluoriteArray).values.forEachIndexed { index, value ->
                    if (index > 0) sb.append(separator)
                    val string = value.toFluoriteString().value
                    if (string.isEmpty()) return@forEachIndexed
                    val needQuote = run {
                        val first = string.first()
                        if (first == ' ') return@run true
                        if (first == '\t') return@run true
                        val last = string.last()
                        if (last == ' ') return@run true
                        if (last == '\t') return@run true
                        if (separator in string) return@run true
                        if (quote in string) return@run true
                        if ('\r' in string) return@run true
                        if ('\n' in string) return@run true
                        false
                    }
                    if (needQuote) {
                        sb.append(quote)
                        sb.append(string.replace(quote, "$quote$quote"))
                        sb.append(quote)
                    } else {
                        sb.append(string)
                    }
                }
                return sb.toString().toFluoriteString()
            }

            if (value is FluoriteStream) {
                FluoriteStream {
                    value.collect {
                        emit(toCsv(it))
                    }
                }
            } else {
                toCsv(value)
            }
        },
        "CSVD" to FluoriteFunction { arguments ->
            fun usage(): Nothing = usage("""CSVD(["separator": separator: STRING; ]["quote": quote: STRING; ]csv: STRING | STREAM<STRING>): ARRAY<STRING> | STREAM<ARRAY<STRING>>""")
            if (arguments.isEmpty()) usage()
            val parameters = arguments.dropLast(1)
                .associate {
                    val array = it as? FluoriteArray ?: usage()
                    if (array.values.size != 2) usage()
                    val key = array.values[0] as? FluoriteString ?: usage()
                    val value = array.values[1]
                    key.value to value
                }
                .toMutableMap()
            val separator = parameters.pop("separator", {
                val string = it.toFluoriteString().value
                check(string.length == 1)
                string
            }) { "," } // StringでやらないとJSの謎バグでChar同士の比較が成功しない
            val quote = parameters.pop("quote", {
                val string = it.toFluoriteString().value
                check(string.length == 1)
                string
            }) { "\"" } // StringでやらないとJSの謎バグでChar同士の比較が成功しない
            if (parameters.isNotEmpty()) usage()
            val value = arguments.last()

            suspend fun fromCsv(csv: FluoriteValue): FluoriteArray {
                val list = mutableListOf<FluoriteValue>()
                val sb = StringBuilder()
                val quoted = mutableListOf<Boolean>()

                val STATE_NOT_QUOTED = 0
                val STATE_QUOTED = 1
                val STATE_AFTER_QUOTED = 2
                var state = STATE_NOT_QUOTED

                fun flush() {
                    var head = 0
                    while (true) {
                        if (head >= sb.length) break
                        if (quoted[head]) break
                        if (sb[head] != ' ' && sb[head] != '\t') break
                        head++
                    }

                    var tail = sb.length - 1
                    while (true) {
                        if (tail + 1 <= head) break
                        if (quoted[tail]) break
                        if (sb[tail] != ' ' && sb[tail] != '\t') break
                        tail--
                    }

                    list += sb.substring(head, tail + 1).toFluoriteString()
                    sb.clear()
                    quoted.clear()
                }

                csv.toFluoriteString().value.forEach { ch2 ->
                    val ch = ch2.toString()
                    if (state == STATE_NOT_QUOTED) {
                        if (ch == quote) {
                            state = STATE_QUOTED
                        } else if (ch == separator) {
                            flush()
                        } else {
                            sb.append(ch)
                            quoted += false
                        }
                    } else if (state == STATE_QUOTED) {
                        if (ch == quote) {
                            state = STATE_AFTER_QUOTED
                        } else {
                            sb.append(ch)
                            quoted += true
                        }
                    } else if (state == STATE_AFTER_QUOTED) {
                        if (ch == quote) {
                            sb.append(ch)
                            quoted += false
                            state = STATE_QUOTED
                        } else if (ch == separator) {
                            flush()
                            state = STATE_NOT_QUOTED
                        } else {
                            sb.append(ch)
                            quoted += false
                            state = STATE_NOT_QUOTED
                        }
                    } else {
                        throw AssertionError()
                    }
                }
                flush()

                return FluoriteArray(list)
            }

            if (value is FluoriteStream) {
                FluoriteStream {
                    value.collect {
                        emit(fromCsv(it))
                    }
                }
            } else {
                fromCsv(value)
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
    )
}

private fun createSortFunction(name: String, isDescending: Boolean): FluoriteFunction {
    return FluoriteFunction { arguments ->
        run { // SORT(stream: STREAM<VALUE>): STREAM<VALUE>
            if (arguments.size != 1) return@run
            val stream = arguments[0]

            return@FluoriteFunction if (stream is FluoriteStream) {
                stream.toMutableList().mergeSort(isDescending) { a, b -> a.compareTo(b).value }.toFluoriteStream()
            } else {
                stream
            }
        }
        run { // SORT(by: key_getter: VALUE -> VALUE; stream: STREAM<VALUE>): STREAM<VALUE>
            if (arguments.size != 2) return@run
            val entry = arguments[0]
            if (entry !is FluoriteArray) return@run
            if (entry.values.size != 2) return@run
            val parameterName = entry.values[0]
            if (parameterName !is FluoriteString) return@run
            if (parameterName.value != "by") return@run
            val keyGetter = entry.values[1]
            val stream = arguments[1]

            return@FluoriteFunction if (stream is FluoriteStream) {
                stream.toMutableList().mergeSort(isDescending) { a, b -> keyGetter.invoke(arrayOf(a)).compareTo(keyGetter.invoke(arrayOf(b))).value }.toFluoriteStream()
            } else {
                stream
            }
        }
        run { // SORT(comparator: VALUE, VALUE -> INT; stream: STREAM<VALUE>): STREAM<VALUE>
            if (arguments.size != 2) return@run
            val comparator = arguments[0]
            val stream = arguments[1]

            return@FluoriteFunction if (stream is FluoriteStream) {
                stream.toMutableList().mergeSort(isDescending) { a, b -> (comparator.invoke(arrayOf(a, b)) as FluoriteInt).value }.toFluoriteStream()
            } else {
                stream
            }
        }
        usage(
            "$name(stream: STREAM<VALUE>): STREAM<VALUE>",
            "$name(comparator: VALUE, VALUE -> INT; stream: STREAM<VALUE>): STREAM<VALUE>",
            "$name(by: key_getter: VALUE -> VALUE; stream: STREAM<VALUE>): STREAM<VALUE>",
        )
    }
}

private suspend fun List<FluoriteValue>.mergeSort(isDescending: Boolean, comparator: suspend (FluoriteValue, FluoriteValue) -> Int): List<FluoriteValue> {
    if (this.size <= 1) return this

    val middle = this.size / 2
    val left = this.subList(0, middle).mergeSort(isDescending, comparator)
    val right = this.subList(middle, this.size).mergeSort(isDescending, comparator)

    return merge(isDescending, comparator, left, right)
}

private suspend fun merge(isDescending: Boolean, comparator: suspend (FluoriteValue, FluoriteValue) -> Int, left: List<FluoriteValue>, right: List<FluoriteValue>): List<FluoriteValue> {
    val result = mutableListOf<FluoriteValue>()
    var i = 0
    var j = 0

    while (i < left.size && j < right.size) {
        val compared = if (isDescending) {
            comparator(left[i], right[j]) >= 0
        } else {
            comparator(left[i], right[j]) <= 0
        }
        if (compared) {
            result.add(left[i])
            i++
        } else {
            result.add(right[j])
            j++
        }
    }
    while (i < left.size) {
        result.add(left[i])
        i++
    }
    while (j < right.size) {
        result.add(right[j])
        j++
    }

    return result
}
