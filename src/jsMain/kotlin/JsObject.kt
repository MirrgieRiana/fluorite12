import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.compilers.objects.toFluoriteBoolean
import mirrg.fluorite12.compilers.objects.toFluoriteString

class FluoriteJsObject(val value: dynamic) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "_()" to FluoriteFunction { arguments ->
                        val jsObject = arguments[0] as FluoriteJsObject
                        val actualArguments = arguments.drop(1).map { it.toJsObject() }.toTypedArray()
                        convertToFluoriteValue(jsObject.value.apply(undefined, actualArguments))
                    },
                    "_._" to FluoriteFunction { arguments ->
                        if (arguments.size != 2) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val jsObject = arguments[0] as FluoriteJsObject
                        val key: dynamic = run {
                            when (val key = arguments[1]) {
                                is FluoriteNumber -> key.roundToInt()
                                else -> key.toFluoriteString().value
                            }
                        }
                        convertToFluoriteValue(jsObject.value[key])
                    },
                    "_._=" to FluoriteFunction { arguments ->
                        if (arguments.size != 3) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
                        val jsObject = arguments[0] as FluoriteJsObject
                        val key: dynamic = run {
                            when (val key = arguments[1]) {
                                is FluoriteNumber -> key.roundToInt()
                                else -> key.toFluoriteString().value
                            }
                        }
                        val value = arguments[2].toJsObject()
                        jsObject.value[key] = value
                        FluoriteNull
                    },
                    "_::_" to FluoriteFunction { arguments ->
                        val jsObject = arguments[0] as FluoriteJsObject
                        val method = arguments[1] as FluoriteString
                        FluoriteFunction { arguments2 ->
                            val actualArguments = arguments2.map { it.toJsObject() }.toTypedArray()
                            convertToFluoriteValue(jsObject.value[method].apply(jsObject.value, actualArguments))
                        }
                    },
                    "new" to FluoriteFunction { arguments ->
                        val jsObject = arguments[0] as FluoriteJsObject
                        val actualArguments = arguments.drop(1).map { it.toJsObject() }.toTypedArray()
                        convertToFluoriteValue(js("Reflect.construct")(jsObject.value, actualArguments))
                    },
                )
            )
        }
    }

    override fun toString() = value.toString()
    override val parent get() = fluoriteClass
}

fun FluoriteFunction.toJsFunction(): dynamic {
    val functionCreator = js(
        """
            (function(f) {
                return function() {
                    return f(arguments);
                };
            })
        """
    )
    return functionCreator { arguments: Array<dynamic> ->
        var finished = false
        var result: dynamic = undefined
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            val result2 = this@toJsFunction.invoke(arguments.map { convertToFluoriteValue(it) }.toTypedArray()).toJsObject()
            finished = true
            result = result2
        }
        if (!finished) {
            job.cancel()
            throw IllegalStateException("Illegal suspension")
        }
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun FluoriteFunction.toJsAsyncFunction(): dynamic {
    val functionCreator = js(
        """
            (function(f) {
                return eval("(async function() {" +
                    "return await f(arguments);" + // こうしないと下のjsマクロでawaitがシンタックスエラーになる
                "})");
            })
        """
    )
    return functionCreator { arguments: Array<dynamic> ->
        GlobalScope.promise {
            this@toJsAsyncFunction.invoke(arguments.map { convertToFluoriteValue(it) }.toTypedArray()).toJsObject()
        }
    }
}

fun FluoriteValue.toJsObject(): dynamic {
    return when (this) {
        is FluoriteJsObject -> this.value
        is FluoriteInt -> this.value
        is FluoriteDouble -> this.value
        is FluoriteString -> this.value
        is FluoriteBoolean -> this.value
        is FluoriteArray -> this.values.map { it.toJsObject() }.toTypedArray()
        is FluoriteNull -> null
        is FluoriteFunction -> this.toJsFunction()
        else -> throw IllegalArgumentException("Invalid argument: $this : ${this::class}") // TODO
    }
}

@Suppress("USELESS_CAST")
fun convertToFluoriteValue(value: dynamic): FluoriteValue {
    return when (value) {
        is Number -> {
            if (js("Number").isInteger(value) as Boolean) {
                FluoriteInt(value as Int)
            } else {
                FluoriteDouble(value as Double)
            }
        }

        is String -> FluoriteString(value as String)
        is Boolean -> (value as Boolean).toFluoriteBoolean()
        is Array<*> -> FluoriteArray((value as Array<*>).map { convertToFluoriteValue(it) }.toMutableList())
        null -> FluoriteNull
        undefined -> FluoriteNull
        else -> FluoriteJsObject(value)
    }
}
