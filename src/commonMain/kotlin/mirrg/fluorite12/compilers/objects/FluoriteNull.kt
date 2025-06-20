package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.OperatorMethod

object FluoriteNull : FluoriteValue {
    val fluoriteClass by lazy {
        FluoriteObject(
            FluoriteValue.fluoriteClass, mutableMapOf(
                OperatorMethod.TO_NUMBER.methodName to FluoriteFunction { FluoriteInt.ZERO },
                OperatorMethod.TO_BOOLEAN.methodName to FluoriteFunction { FluoriteBoolean.FALSE },
            )
        )
    }
    override val parent = fluoriteClass
    override fun toString() = "NULL"
}
