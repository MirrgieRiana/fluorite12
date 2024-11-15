package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.toFluoriteNumber
import mirrg.fluorite12.toFluoriteString
import mirrg.fluorite12.toJson

class FluoriteArray(val values: MutableList<FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "INVOKE" to FluoriteFunction { arguments ->
                        val array = arguments[0] as FluoriteArray
                        when (arguments.size) {
                            1 -> FluoriteStream(array.values)

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
                    "BIND" to FluoriteFunction { arguments ->
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
                    "TO_STRING" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('[')
                        (arguments[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(';')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
                    "TO_BOOLEAN" to FluoriteFunction { (it[0] as FluoriteArray).values.isNotEmpty().toFluoriteBoolean() },
                    "TO_JSON" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('[')
                        (arguments[0] as FluoriteArray).values.forEachIndexed { i, value ->
                            if (i != 0) sb.append(',')
                            sb.append((value.toJson() as FluoriteString).value)
                        }
                        sb.append(']')
                        sb.toString().toFluoriteString()
                    },
                    "CONTAINS" to FluoriteFunction { (it[1] in (it[0] as FluoriteArray).values).toFluoriteBoolean() }, // TODO EQUALSメソッドの使用
                )
            )
        }
    }

    override fun toString() = "[${values.joinToString(";") { "$it" }}]"
    override val parent get() = fluoriteClass
}
