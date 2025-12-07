import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.mounts.createCommonMounts
import kotlin.js.Promise

private val scope = MainScope()

@OptIn(ExperimentalJsExport::class)
@JsExport
fun evaluate(src: String, quiet: Boolean, out: (dynamic) -> Promise<Unit>): Promise<dynamic> = scope.promise {
    val evaluator = Evaluator()
    val defaultBuiltinMounts = listOf(
        createCommonMounts { out(it).await() },
        createJsMounts(),
    ).flatten()
    evaluator.defineMounts(defaultBuiltinMounts)
    if (quiet) {
        evaluator.run(src)
        undefined
    } else {
        evaluator.get(src)
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun log(value: dynamic): Promise<Unit> = scope.promise {
    val value = value.unsafeCast<FluoriteValue>()
    if (value is FluoriteStream) {
        value.collect {
            console.log(it)
        }
    } else {
        console.log(value)
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun stringify(value: dynamic): Promise<String> = scope.promise {
    val value = value.unsafeCast<FluoriteValue>()
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
