package mirrg.fluorite12.compilers.objects

class FluoriteArray(val values: MutableList<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "_._" to FluoriteFunction { arguments ->
                        val array = arguments[0] as FluoriteArray
                        val index = arguments[1].toFluoriteNumber().roundToInt()
                        array.values.getOrNull(index) ?: FluoriteNull
                    },
                    "_._=" to FluoriteFunction { arguments ->
                        val array = arguments[0] as FluoriteArray
                        val index = arguments[1].toFluoriteNumber().roundToInt()
                        val value = arguments[2]
                        array.values[index] = value
                        FluoriteNull
                    },
                    "_()" to FluoriteFunction { arguments ->
                        val array = arguments[0] as FluoriteArray
                        when (arguments.size) {
                            1 -> array.values.toFluoriteStream()

                            2 -> {
                                suspend fun get(index: FluoriteValue): FluoriteValue {
                                    val index2 = index.toFluoriteNumber().roundToInt()
                                    return array.values.getOrNull(if (index2 >= 0) index2 else index2 + array.values.size) ?: FluoriteNull
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
                        val array = arguments[0] as FluoriteArray
                        when (arguments.size) {
                            1 -> FluoriteArray(array.values.toMutableList())

                            2 -> {
                                suspend fun get(index: FluoriteValue): FluoriteValue {
                                    val index2 = index.toFluoriteNumber().roundToInt()
                                    return array.values.getOrNull(if (index2 >= 0) index2 else index2 + array.values.size) ?: FluoriteNull
                                }

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    val items = mutableListOf<FluoriteValue>()
                                    argument.collect { index ->
                                        items += get(index)
                                    }
                                    FluoriteArray(items)
                                } else {
                                    FluoriteArray(mutableListOf(get(argument)))
                                }
                            }

                            else -> throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        }
                    },
                    "&_" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('[')
                        (arguments[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(';')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
                    "?_" to FluoriteFunction { (it[0] as FluoriteArray).values.isNotEmpty().toFluoriteBoolean() },
                    "_+_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteArray
                        val right = arguments[1] as FluoriteArray
                        val list = mutableListOf<FluoriteValue>()
                        list += left.values
                        list += right.values
                        FluoriteArray(list)
                    },
                    "_@_" to FluoriteFunction { (it[1] in (it[0] as FluoriteArray).values).toFluoriteBoolean() }, // TODO EQUALSメソッドの使用
                    "push" to FluoriteFunction { arguments ->
                        if (arguments.size != 2) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val array = arguments[0] as FluoriteArray
                        val value = arguments[1]
                        if (value is FluoriteStream) {
                            value.collect { item ->
                                array.values.add(item)
                            }
                        } else {
                            array.values.add(value)
                        }
                        FluoriteNull
                    },
                    "pop" to FluoriteFunction { arguments ->
                        if (arguments.size != 1) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val array = arguments[0] as FluoriteArray
                        array.values.removeLast()
                        FluoriteNull
                    },
                    "unshift" to FluoriteFunction { arguments ->
                        if (arguments.size != 2) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val array = arguments[0] as FluoriteArray
                        val value = arguments[1]
                        if (value is FluoriteStream) {
                            var index = 0
                            value.collect { item ->
                                array.values.add(index, item)
                                index++
                            }
                        } else {
                            array.values.add(0, value)
                        }
                        FluoriteNull
                    },
                    "shift" to FluoriteFunction { arguments ->
                        if (arguments.size != 1) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val array = arguments[0] as FluoriteArray
                        array.values.removeFirst()
                        FluoriteNull
                    },
                )
            )
        }
    }

    override fun toString() = "[${values.joinToString(";") { "$it" }}]"
    override val parent get() = fluoriteClass
}
