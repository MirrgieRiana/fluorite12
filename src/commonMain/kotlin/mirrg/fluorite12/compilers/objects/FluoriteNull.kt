package mirrg.fluorite12.compilers.objects

object FluoriteNull : FluoriteValue {
    val fluoriteClass by lazy {
        FluoriteObject(
            FluoriteValue.fluoriteClass, mutableMapOf(
                "TO_NUMBER" to FluoriteFunction { FluoriteInt.ZERO },
                "TO_BOOLEAN" to FluoriteFunction { FluoriteBoolean.FALSE },
                "TO_JSON" to FluoriteFunction { "null".toFluoriteString() },
            )
        )
    }
    override val parent = fluoriteClass
    override fun toString() = "NULL"
}
