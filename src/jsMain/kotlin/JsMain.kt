import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.mounts.createCommonMount
import kotlin.js.Promise

@Suppress("unused")
@JsName("evaluate")
fun evaluate(src: String, quiet: Boolean, out: suspend (FluoriteValue) -> Unit) = GlobalScope.promise {
    val evaluator = Evaluator()
    val defaultBuiltinMounts = listOf(
        createCommonMount(),
        createJsMount(out),
    )
    evaluator.defineMounts(defaultBuiltinMounts)
    if (quiet) {
        evaluator.run(src)
        undefined
    } else {
        evaluator.get(src)
    }
}

@Suppress("unused")
@JsName("log")
fun log(value: FluoriteValue) = GlobalScope.promise {
    if (value is FluoriteStream) {
        value.collect {
            console.log(it)
        }
    } else {
        console.log(value)
    }
}

@Suppress("unused")
@JsName("stringify")
fun stringify(value: FluoriteValue): Promise<String> = GlobalScope.promise {
    suspend fun f(value: FluoriteValue): String {
        return if (value is FluoriteStream) {
            val sb = StringBuilder()
            var isFirst = true
            value.collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    sb.append('\n')
                }
                sb.append(f(it))
            }
            sb.toString()
        } else {
            value.toFluoriteString().value
        }
    }
    f(value)
}
