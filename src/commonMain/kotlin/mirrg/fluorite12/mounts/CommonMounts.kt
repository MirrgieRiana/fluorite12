package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteValue

fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun createCommonMounts(out: suspend (FluoriteValue) -> Unit): List<Map<String, FluoriteValue>> {
    return listOf(
        createClassMounts(),
        createLangMounts(out),
        createMathMounts(),
        createConvertMounts(),
        createStreamMounts(),
        createDataConvertMounts(),
        createStringMounts(),
    ).flatten()
}
