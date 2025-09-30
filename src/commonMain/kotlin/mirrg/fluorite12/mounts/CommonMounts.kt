package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteValue

fun usage(vararg usages: String): Nothing = throw IllegalArgumentException(listOf("Usage:", *usages.map { "  $it" }.toTypedArray()).joinToString("\n"))

fun createCommonMounts(): Map<String, FluoriteValue> {
    return listOf(
        createClassMounts(),
        createLangMounts(),
        createMathMounts(),
        createConvertMounts(),
        createStreamMounts(),
        createDataConvertMounts(),
        createStringMounts(),
    ).flatMap { it.entries }.associate { it.toPair() }
}
