import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.Frame
import mirrg.fluorite12.evaluate

fun main(args: Array<String>) = runBlocking {
    val src = args[0]
    when (val parseResult = Fluorite12Grammar().tryParseToEnd(src)) {
        is Parsed -> {
            val node = parseResult.value
            when (val result = Frame().evaluate(node)) {
                is FluoriteStream -> {
                    result.flow.collect {
                        println(it.toString())
                    }
                }

                else -> {
                    println(result.toString())
                }
            }
        }

        is ErrorResult -> {
            println(parseResult.toString())
        }
    }
}
