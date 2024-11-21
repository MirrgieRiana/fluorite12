package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.escapeJsonString

class FluoriteObject(override val parent: FluoriteObject?, val map: MutableMap<String, FluoriteValue>) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "_()" to FluoriteFunction { arguments ->
                        val obj = arguments[0] as FluoriteObject
                        when (arguments.size) {
                            1 -> {
                                FluoriteStream {
                                    obj.map.entries.forEach {
                                        emit(FluoriteArray(mutableListOf(it.key.toFluoriteString(), it.value)))
                                    }
                                }
                            }

                            2 -> {
                                suspend fun get(key: FluoriteValue) = obj.map[key.toFluoriteString().value] ?: FluoriteNull

                                val argument = arguments[1]
                                if (argument is FluoriteStream) {
                                    FluoriteStream {
                                        argument.collect { key ->
                                            emit(get(key))
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
                        // TODO
                        val obj = arguments[0] as FluoriteObject
                        val arguments1 = arguments.sliceArray(1 until arguments.size)
                        FluoriteFunction { arguments2 ->
                            obj.invoke(arguments1 + arguments2)
                        }
                    },
                    "&_" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('{')
                        (arguments[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
                            if (i != 0) sb.append(';')
                            sb.append(key)
                            sb.append(':')
                            sb.append(value.toFluoriteString().value)
                        }
                        sb.append('}')
                        sb.toString().toFluoriteString()
                    },
                    "?_" to FluoriteFunction { (it[0] as FluoriteObject).map.isNotEmpty().toFluoriteBoolean() },
                    "$&_" to FluoriteFunction { arguments ->
                        val sb = StringBuilder()
                        sb.append('{')
                        (arguments[0] as FluoriteObject).map.entries.forEachIndexed { i, (key, value) ->
                            if (i != 0) sb.append(',')
                            sb.append('"')
                            sb.append(key.escapeJsonString())
                            sb.append('"')
                            sb.append(':')
                            sb.append((value.toJson() as FluoriteString).value)
                        }
                        sb.append('}')
                        sb.toString().toFluoriteString()
                    },
                    "_@_" to FluoriteFunction { (it[1].toFluoriteString().value in (it[0] as FluoriteObject).map).toFluoriteBoolean() },
                )
            )
        }
    }

    override fun toString() = "{${map.entries.joinToString(";") { "${it.key}:${it.value}" }}}"
}
