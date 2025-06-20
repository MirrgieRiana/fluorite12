package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.OperatorMethod

class FluoriteFunction(val function: suspend (Array<FluoriteValue>) -> FluoriteValue) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    OperatorMethod.CALL.methodName to FluoriteFunction { arguments ->
                        (arguments[0] as FluoriteFunction).function(arguments.sliceArray(1 until arguments.size))
                    },
                    OperatorMethod.BIND.methodName to FluoriteFunction { arguments ->
                        val function = arguments[0] as FluoriteFunction
                        val arguments1 = arguments.sliceArray(1 until arguments.size)
                        FluoriteFunction { arguments2 ->
                            function.function(arguments1 + arguments2)
                        }
                    },
                )
            )
        }
    }

    override val parent get() = fluoriteClass
}
