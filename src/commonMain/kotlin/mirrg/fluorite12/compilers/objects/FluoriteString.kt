package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.escapeJsonString

class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "_()" to FluoriteFunction { arguments ->
                        val string = (arguments[0] as FluoriteString).value
                        when (arguments.size) {
                            1 -> {
                                FluoriteStream {
                                    string.forEach {
                                        emit(it.toString().toFluoriteString())
                                    }
                                }
                            }

                            2 -> {
                                suspend fun get(index: FluoriteValue): FluoriteValue {
                                    val index2 = index.toFluoriteNumber().roundToInt()
                                    return string.getOrNull(if (index2 >= 0) index2 else index2 + string.length)?.toString()?.toFluoriteString() ?: FluoriteNull
                                }

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    FluoriteStream {
                                        argument.collect { index ->
                                            emit(get(index))
                                        }
                                    }
                                } else {
                                    get(argument)
                                }
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "_[]" to FluoriteFunction { arguments ->
                        val string = arguments[0] as FluoriteString
                        when (arguments.size) {
                            1 -> string

                            2 -> {
                                val kotlinString = string.value
                                val sb = StringBuilder()
                                suspend fun append(index: FluoriteValue) {
                                    val index2 = index.toFluoriteNumber().roundToInt()
                                    val index3 = if (index2 >= 0) index2 else index2 + kotlinString.length
                                    if (index3 >= 0 && index3 < kotlinString.length) {
                                        sb.append(kotlinString[index3])
                                    } else {
                                        sb.append("NULL")
                                    }
                                }

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    argument.collect { index ->
                                        append(index)
                                    }
                                } else {
                                    append(argument)
                                }
                                FluoriteString(sb.toString())
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "+_" to FluoriteFunction { arguments ->
                        val string = (arguments[0] as FluoriteString).value
                        if (string.all { c -> c in '0'..'9' }) {
                            when (val int = string.toInt()) {
                                0 -> FluoriteInt.ZERO
                                1 -> FluoriteInt.ONE
                                else -> FluoriteInt(int)
                            }
                        } else {
                            when (val double = string.toDouble()) {
                                0.0 -> FluoriteDouble.ZERO
                                else -> FluoriteDouble(double)
                            }
                        }
                    },
                    "?_" to FluoriteFunction { ((it[0] as FluoriteString).value != "").toFluoriteBoolean() },
                    "&_" to FluoriteFunction { it[0] as FluoriteString },
                    "$&_" to FluoriteFunction { arguments ->
                        val escaped = (arguments[0] as FluoriteString).value.escapeJsonString()
                        "\"$escaped\"".toFluoriteString()
                    },
                    "_@_" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteString).value).toFluoriteBoolean() },
                )
            )
        }
    }

    override fun toString() = value
    override fun equals(other: Any?) = other is FluoriteString && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
}

fun String.toFluoriteString() = FluoriteString(this)
