import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteValue

class FluoriteJsObject(val value: dynamic) : FluoriteValue {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(

                )
            )
        }
    }

    override fun toString() = value.toString()
    override val parent get() = fluoriteClass
}
