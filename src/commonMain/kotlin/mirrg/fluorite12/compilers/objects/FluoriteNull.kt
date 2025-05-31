package mirrg.fluorite12.compilers.objects

object FluoriteNull : FluoriteValue {
    val fluoriteClass by lazy {
        FluoriteObject(
            FluoriteValue.fluoriteClass, mutableMapOf(
                "+_" to FluoriteFunction { FluoriteInt.ZERO },
                "?_" to FluoriteFunction { FluoriteBoolean.FALSE },
            )
        )
    }
    override val parent = fluoriteClass
    override fun toString() = "NULL"
}
