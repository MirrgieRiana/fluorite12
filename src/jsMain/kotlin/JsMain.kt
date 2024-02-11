import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.promise
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.Frame
import mirrg.fluorite12.Node
import mirrg.fluorite12.evaluate
import kotlin.js.Promise

@Suppress("unused")
@JsName("parse")
fun parse(src: String) = Fluorite12Grammar().tryParseToEnd(src)

@Suppress("unused")
@JsName("evaluate")
fun evaluate(node: Node) = GlobalScope.promise { Frame().evaluate(node) }

@Suppress("unused")
@JsName("log")
fun log(value: Any?) = GlobalScope.promise {
    when (value) {
        is FluoriteStream -> {
            value.flow.collect {
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
fun stringify(value: Any?): Promise<String> = GlobalScope.promise {
    suspend fun f(value: Any?): String = when (value) {
        is FluoriteStream -> value.flow.map { f(it) }.toList().joinToString("\n")
        else -> value.toString()
    }
    f(value)
}
