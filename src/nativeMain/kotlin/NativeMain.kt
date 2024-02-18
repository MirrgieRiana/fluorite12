import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteFunction
import mirrg.fluorite12.FluoriteNull
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.Frame
import mirrg.fluorite12.Variable
import mirrg.fluorite12.defineCommonBuiltinVariables
import mirrg.fluorite12.evaluate
import mirrg.fluorite12.toFluoriteString

fun main(args: Array<String>) = runBlocking {
    val src = args[0]
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val node = parseResult.value
    val frame = Frame()
    frame.defineCommonBuiltinVariables()
    frame.variables["ARGS"] = Variable(false, FluoriteArray(args.drop(1).map { it.toFluoriteString() }))
    frame.variables["IN"] = Variable(false, FluoriteStream(flow {
        while (true) {
            val line = readlnOrNull() ?: break
            emit(line.toFluoriteString())
        }
    }))
    frame.variables["OUT"] = Variable(false, FluoriteFunction { arguments ->
        arguments.forEach {
            println(it.toString())
        }
        FluoriteNull
    })
    when (val result = frame.evaluate(node)) {
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
