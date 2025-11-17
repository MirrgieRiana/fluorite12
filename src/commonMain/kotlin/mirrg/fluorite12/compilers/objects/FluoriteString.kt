package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.OperatorMethod
import mirrg.fluorite12.toFluoriteIntAsCompared

data class FluoriteString(val value: String) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    OperatorMethod.PROPERTY.methodName to FluoriteFunction { arguments ->
                        val string = arguments[0] as FluoriteString
                        val index = arguments[1].toFluoriteNumber().roundToInt()
                        string.value.getOrNull(index)?.toString()?.toFluoriteString() ?: FluoriteNull
                    },
                    OperatorMethod.SET_PROPERTY.methodName to FluoriteFunction { arguments ->
                        throw IllegalArgumentException("Cannot set item to string") // TODO
                    },
                    OperatorMethod.CALL.methodName to FluoriteFunction { arguments ->
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
                    OperatorMethod.BIND.methodName to FluoriteFunction { arguments ->
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
                    OperatorMethod.TO_NUMBER.methodName to FluoriteFunction { arguments ->
                        val string = (arguments[0] as FluoriteString).value
                        string.toFluoriteNumber()
                    },
                    OperatorMethod.TO_BOOLEAN.methodName to FluoriteFunction { ((it[0] as FluoriteString).value != "").toFluoriteBoolean() },
                    OperatorMethod.TO_STRING.methodName to FluoriteFunction { it[0] as FluoriteString },
                    OperatorMethod.PLUS.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteString
                        val right = arguments[1]
                        FluoriteString(left.value + right.toFluoriteString().value)
                    },
                    OperatorMethod.COMPARE.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteString
                        val right = arguments[1].toFluoriteString()
                        left.value.compareTo(right.value).toFluoriteIntAsCompared()
                    },
                    OperatorMethod.CONTAINS.methodName to FluoriteFunction { arguments ->
                        val right = arguments[0] as FluoriteString
                        val left = arguments[1]
                        when (left) {
                            is FluoriteRegex -> (left.regexCache.find(right.value) != null).toFluoriteBoolean()
                            else -> (left.toFluoriteString().value in right.value).toFluoriteBoolean()
                        }
                    },
                    "replace" to FluoriteFunction { arguments ->
                        if (arguments.size != 3) throw IllegalArgumentException("STRING::replace(old: STRING; new: STRING): STRING")
                        val string = arguments[0] as FluoriteString
                        val old = arguments[1]
                        val new = arguments[2]

                        val source = string.value

                        // 置換後文字列を得る関数（将来: 関数置換に対応させやすい形）
                        suspend fun replacementText(matchedText: String): String {
                            // new が関数なら呼び出して文字列化、そうでなければ単純に文字列化
                            return if (new is FluoriteFunction) {
                                val result = new.invoke(arrayOf(matchedText.toFluoriteString()))
                                result.toFluoriteString().value
                            } else {
                                new.toFluoriteString().value
                            }
                        }

                        val result: String = when (old) {
                            is FluoriteRegex -> {
                                // 正規表現置換: g フラグなら全置換、なければ最初のみ
                                val regex = old.regexCache
                                if (old.flagData.global) {
                                    // 全置換。関数置換対応
                                    val sb = StringBuilder()
                                    var lastIndex = 0
                                    regex.findAll(source).forEach { mr ->
                                        sb.append(source, lastIndex, mr.range.first)
                                        sb.append(replacementText(mr.value))
                                        lastIndex = mr.range.last + 1
                                    }
                                    if (lastIndex < source.length) sb.append(source.substring(lastIndex))
                                    sb.toString()
                                } else {
                                    val mr = regex.find(source)
                                    if (mr == null) source else buildString {
                                        append(source.substring(0, mr.range.first))
                                        append(replacementText(mr.value))
                                        append(source.substring(mr.range.last + 1))
                                    }
                                }
                            }
                            else -> {
                                val oldText = old.toFluoriteString().value
                                if (oldText.isEmpty()) {
                                    // 空文字列: 文字間（両端含む）に new を挿入
                                    val rep = replacementText("")
                                    if (source.isEmpty()) {
                                        rep // 両端のみ -> rep
                                    } else {
                                        buildString {
                                            append(rep)
                                            source.forEach { ch ->
                                                append(ch)
                                                append(rep)
                                            }
                                        }
                                    }
                                } else {
                                    // 非空文字列: 非重複で全出現を左から順に置換
                                    val rep = replacementText(oldText)
                                    var idx = 0
                                    val sb = StringBuilder()
                                    while (true) {
                                        val next = source.indexOf(oldText, idx)
                                        if (next < 0) {
                                            sb.append(source.substring(idx))
                                            break
                                        }
                                        sb.append(source, idx, next)
                                        sb.append(rep)
                                        idx = next + oldText.length
                                    }
                                    sb.toString()
                                }
                            }
                        }

                        result.toFluoriteString()
                    },
                )
            )

        }
        val EMPTY = "".toFluoriteString()
    }

    override fun toString() = value
    override val parent get() = fluoriteClass
}

fun String.toFluoriteString() = FluoriteString(this)
