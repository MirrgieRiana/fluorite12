import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Environment
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.FluoriteArray
import mirrg.fluorite12.FluoriteFunction
import mirrg.fluorite12.FluoriteNull
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.Frame
import mirrg.fluorite12.collect
import mirrg.fluorite12.compilers.compileToGetter
import mirrg.fluorite12.defineCommonBuiltinConstants
import mirrg.fluorite12.defineConstant
import mirrg.fluorite12.toFluoriteString

fun main(args: Array<String>) = runBlocking {
    val src = args[0]
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runners = listOf(
        *frame.defineCommonBuiltinConstants().toTypedArray(),
        frame.defineConstant("ARGS", FluoriteArray(args.drop(1).map { it.toFluoriteString() }.toMutableList())),
        frame.defineConstant("IN", FluoriteStream {
            while (true) {
                val line = readlnOrNull() ?: break
                emit(line.toFluoriteString())
            }
        }),
        frame.defineConstant("OUT", FluoriteFunction { arguments ->
            arguments.forEach {
                if (it is FluoriteStream) {
                    it.collect { item ->
                        println(item.toFluoriteString().value)
                    }
                } else {
                    println(it.toFluoriteString().value)
                }
            }
            FluoriteNull
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
            println(it.toFluoriteString())
        }
    } else {
        println(result.toFluoriteString())
    }
}
