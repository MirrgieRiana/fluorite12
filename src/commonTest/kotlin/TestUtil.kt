import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.Fluorite12Grammar
import mirrg.fluorite12.Frame
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
import mirrg.fluorite12.mounts.createCommonMounts
import mirrg.fluorite12.parser.parseAllOrThrow

fun parse(src: String): String {
    val parseResult = Fluorite12Grammar.rootParser.parseAllOrThrow(src)
    val frame = Frame()
    val getter = frame.compileToGetter(parseResult)
    return getter.code
}

suspend fun CoroutineScope.eval(src: String): FluoriteValue {
    val evaluator = Evaluator()
    evaluator.defineMounts(createCommonMounts(this) {})
    return evaluator.get(src)
}

val FluoriteValue.int get() = (this as FluoriteInt).value
val FluoriteValue.double get() = (this as FluoriteDouble).value
val FluoriteValue.boolean get() = (this as FluoriteBoolean).value
val FluoriteValue.string get() = (this as FluoriteString).value
val FluoriteValue.obj get() = (this as FluoriteObject).toString()
suspend fun FluoriteValue.array() = (this as FluoriteArray).toFluoriteString().value
suspend fun FluoriteValue.stream() = flow { (this@stream as FluoriteStream).flowProvider(this) }.toList(mutableListOf()).joinToString(",") { "$it" }
suspend fun FluoriteValue.empty() = flow { (this@empty as FluoriteStream).flowProvider(this) }.toList(mutableListOf()).isEmpty()
