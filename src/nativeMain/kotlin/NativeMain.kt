import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.Evaluator
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteString
import mirrg.fluorite12.mounts.createCommonMount

fun main(args: Array<String>) {
    val list = args.toMutableList()
    var src: String? = null
    val arguments = mutableListOf<String>()
    var quiet = false

    try {
        run {
            while (true) {

                if (list.firstOrNull() == "--") {
                    list.removeFirst()

                    if (list.isEmpty()) throw ShowUsage

                    src = list.removeFirst()
                    arguments += list
                    list.clear()
                    return@run
                }

                if (list.firstOrNull() == "-h") throw ShowUsage
                if (list.firstOrNull() == "--help") throw ShowUsage

                if (list.firstOrNull() == "-q") {
                    if (quiet) throw ShowUsage
                    quiet = true
                    list.removeFirst()
                    continue
                }

                if (list.isEmpty()) throw ShowUsage

                src = list.removeFirst()
                arguments += list
                list.clear()
                return@run
            }
        }
    } catch (_: ShowUsage) {
        println("Usage: flc [-h|--help] [-q] [--] <code> <arguments...>")
        println("Options:")
        println("  -h, --help               Show this help")
        println("  -q                       Run script as a runner")
        return
    }

    runBlocking {
        main(src!!, arguments, quiet)
    }
}

private object ShowUsage : Throwable()

suspend fun main(src: String, arguments: List<String>, quiet: Boolean) {
    val evaluator = Evaluator()
    val defaultBuiltinMounts = listOf(
        createCommonMount(),
        createNativeMount(arguments),
    )
    evaluator.defineMounts(defaultBuiltinMounts)
    if (quiet) {
        evaluator.run(src)
    } else {
        val result = evaluator.get(src)
        if (result is FluoriteStream) {
            result.collect {
                println(it.toFluoriteString())
            }
        } else {
            println(result.toFluoriteString())
        }
    }
}
