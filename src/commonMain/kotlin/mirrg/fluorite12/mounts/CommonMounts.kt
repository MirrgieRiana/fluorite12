package mirrg.fluorite12.mounts

import kotlinx.coroutines.CoroutineScope
import mirrg.fluorite12.compilers.objects.FluoriteValue

fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun createCommonMounts(coroutineScope: CoroutineScope, out: suspend (FluoriteValue) -> Unit): List<Map<String, FluoriteValue>> {
    return listOf(
        createClassMounts(),
        createLangMounts(coroutineScope, out),
        createMathMounts(),
        createConvertMounts(),
        createStreamMounts(),
        createDataConvertMounts(),
        createStringMounts(),
    ).flatten()
}
