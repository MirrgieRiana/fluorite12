package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.pop
import mirrg.fluorite12.toFluoriteValueAsJson
import mirrg.fluorite12.toJsonFluoriteValue

fun createDataConvertMounts(): List<Map<String, FluoriteValue>> {
    return mapOf(
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
    ).let { listOf(it) }
}
