package mirrg.fluorite12

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import mirrg.fluorite12.compilers.compileToGetter
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.toFluoriteString

fun parse(src: String): String {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val getter = frame.compileToGetter(parseResult.value)
    return getter.code
}

suspend fun run(src: String): FluoriteValue {
    val parseResult = Fluorite12Grammar().tryParseToEnd(src).toParsedOrThrow()
    val frame = Frame()
    val runner = frame.defineBuiltinMount(createCommonBuiltinMount())
    val getter = frame.compileToGetter(parseResult.value)
    val env = Environment(null, frame.nextVariableIndex, frame.mountCount)
    runner.evaluate(env)
    return getter.evaluate(env)
}

val FluoriteValue.int get() = (this as FluoriteInt).value
val FluoriteValue.double get() = (this as FluoriteDouble).value
val FluoriteValue.boolean get() = (this as FluoriteBoolean).value
val FluoriteValue.string get() = (this as FluoriteString).value
val FluoriteValue.obj get() = (this as FluoriteObject).toString()
suspend fun FluoriteValue.array() = (this as FluoriteArray).toFluoriteString().value
suspend fun FluoriteValue.stream() = flow { (this@stream as FluoriteStream).flowProvider(this) }.toList(mutableListOf()).joinToString(",") { "$it" }
