import kotlinx.browser.window
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString

fun createJsMount(out: suspend (FluoriteValue) -> Unit): Map<String, FluoriteValue> {
    return mapOf(
        "JS_OBJECT" to FluoriteJsObject.fluoriteClass,
        "OUT" to FluoriteFunction { arguments ->
            arguments.forEach {
                if (it is FluoriteStream) {
                    it.collect { item ->
                        out(item)
                    }
                } else {
                    out(it)
                }
            }
            FluoriteNull
        },
        "WINDOW" to try {
            FluoriteJsObject(window)
        } catch (_: Throwable) {
            FluoriteNull
        },
        "JS" to FluoriteFunction { arguments ->
            if (arguments.size != 1) throw IllegalArgumentException("Invalid number of arguments: ${arguments.size}")
            val js = arguments[0].toFluoriteString()
            convertToFluoriteValue(eval(js.value))
        },
    )
}
