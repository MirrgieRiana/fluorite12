package mirrg.fluorite12.cli

import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.mounts.createCommonMounts

fun cliMain(args: Array<String>, runBlocking: (suspend () -> Unit) -> Unit) {
    val options = try {
        parseArguments(args)
    } catch (_: ShowUsage) {
        showUsage()
        return
    }
    runBlocking {
        main(options)
    }
}

private suspend fun main(options: Options) {
    val evaluator = Evaluator()
    val defaultBuiltinMounts = listOf(
        createCommonMounts(),
        createCliMounts(options.arguments),
    ).flatten()
    evaluator.defineMounts(defaultBuiltinMounts)
    if (options.quiet) {
        evaluator.run(options.src)
    } else {
        val result = evaluator.get(options.src)
        if (result is FluoriteStream) {
            result.collect {
                println(it.toFluoriteString())
            }
        } else {
            println(result.toFluoriteString())
        }
    }
}
