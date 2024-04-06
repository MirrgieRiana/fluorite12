import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Environment
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteFunction
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.Frame
import mirrg.fluorite12.collect
import mirrg.fluorite12.compileToGetter
import mirrg.fluorite12.defineCommonBuiltinVariables
import mirrg.fluorite12.defineConstant
import mirrg.fluorite12.toFluoriteString

fun main(args: Array<String>) = runBlocking {
    val src = args[0]
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runners = listOf(
        *frame.defineCommonBuiltinVariables().toTypedArray(),
        frame.defineConstant("ARGS", FluoriteArray(args.drop(1).map { it.toFluoriteString() })),
        frame.defineConstant("IN", FluoriteStream {
            while (true) {
                val line = readlnOrNull() ?: break
                emit(line.toFluoriteString())
            }
        }),
        frame.defineConstant("OUT", FluoriteFunction { arguments ->
            arguments.forEach {
                println(it.toString())
            }
            this
        }),
    )
    val getter = frame.compileToGetter(parseResult.value)
    val env = Environment(null, frame.nextVariableIndex)
    runners.forEach {
        it.evaluate(env)
    }
    val result = getter.evaluate(env)
    if (result is FluoriteStream) {
        result.collect {
            println(it.toString())
        }
    } else {
        println(result.toString())
    }
}
