import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Environment
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.Frame
import mirrg.fluorite12.compilers.compileToGetter
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.defineCommonBuiltinConstants
import mirrg.fluorite12.defineConstant

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
    val env = Environment(null, frame.nextVariableIndex, frame.mountCount)
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
