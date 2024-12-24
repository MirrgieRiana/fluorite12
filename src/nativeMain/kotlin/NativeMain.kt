import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.mounts.createCommonMount

fun main(args: Array<String>) = runBlocking {
    val evaluator = Evaluator()
    val defaultBuiltinMounts = listOf(
        createCommonMount(),
        createNativeMount(args),
    )
    evaluator.defineMounts(defaultBuiltinMounts)
    val result = evaluator.get(args[0])
    if (result is FluoriteStream) {
        result.collect {
            println(it.toFluoriteString())
        }
    } else {
        println(result.toFluoriteString())
    }
}
