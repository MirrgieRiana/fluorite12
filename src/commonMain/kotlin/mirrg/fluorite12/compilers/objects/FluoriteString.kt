package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.toFluoriteIntAsCompared

class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "_._" to FluoriteFunction { arguments ->
                        val string = arguments[0] as FluoriteString
                        val index = arguments[1].toFluoriteNumber().roundToInt()
                        string.value.getOrNull(index)?.toString()?.toFluoriteString() ?: FluoriteNull
                    },
                    "_._=" to FluoriteFunction { arguments ->
                        throw IllegalArgumentException("Cannot set item to string") // TODO
                    },
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
                        string.toFluoriteNumber()
                    },
                    "?_" to FluoriteFunction { ((it[0] as FluoriteString).value != "").toFluoriteBoolean() },
                    "&_" to FluoriteFunction { it[0] as FluoriteString },
                    "_+_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteString
                        val right = arguments[1]
                        FluoriteString(left.value + right.toFluoriteString().value)
                    },
                    "_<=>_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteString
                        val right = arguments[1].toFluoriteString()
                        left.value.compareTo(right.value).toFluoriteIntAsCompared()
                    },
                    "_@_" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteString).value).toFluoriteBoolean() },
                    "replace" to FluoriteFunction { arguments ->
                        if (arguments.size != 3) throw IllegalArgumentException("STRING::replace(old: STRING; new: STRING): STRING")
                        val string = arguments[0] as FluoriteString
                        val old = arguments[1].toFluoriteString().value
                        val new = arguments[2].toFluoriteString().value
                        FluoriteString(string.value.replace(old, new))
                    },
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
