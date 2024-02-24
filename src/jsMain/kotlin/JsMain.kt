import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.FluoriteValue
import mirrg.fluorite12.Frame
import mirrg.fluorite12.Node
import mirrg.fluorite12.collect
import mirrg.fluorite12.compileToGetter
import mirrg.fluorite12.defineCommonBuiltinVariables
import kotlin.js.Promise

@Suppress("unused")
@JsName("parse")
fun parse(src: String) = Fluorite12Grammar().tryParseToEnd(src)

@Suppress("unused")
@JsName("evaluate")
fun evaluate(node: Node) = GlobalScope.promise {
    val frame = Frame()
    frame.defineCommonBuiltinVariables()
    frame.compileToGetter(node)
}

@Suppress("unused")
@JsName("log")
fun log(value: FluoriteValue) = GlobalScope.promise {
    when (value) {
        is FluoriteStream -> {
            value.collect {
                console.log(it)
            }
        }

        else -> {
            console.log(value)
        }
    }
}

@Suppress("unused")
@JsName("stringify")
fun stringify(value: FluoriteValue): Promise<String> = GlobalScope.promise {
    suspend fun f(value: FluoriteValue): String = when (value) {
        is FluoriteStream -> {
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
        }

        else -> value.toString()
    }
    f(value)
}
