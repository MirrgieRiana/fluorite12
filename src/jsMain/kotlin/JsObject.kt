import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.invoke
import mirrg.fluorite12.compilers.objects.toFluoriteBoolean

class FluoriteJsObject(val value: dynamic) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(

                )
            )
        }
    }

    override fun toString() = value.toString()
    override val parent get() = fluoriteClass
}

fun FluoriteFunction.toJsFunction(): dynamic {
    val js = """
        (function(f) {
            return function() {
                return f(arguments);
            };
        })
    """
    val functionCreator = js(js)
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
