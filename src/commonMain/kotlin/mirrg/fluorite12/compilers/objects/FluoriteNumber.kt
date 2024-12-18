package mirrg.fluorite12.compilers.objects

import kotlin.math.roundToInt

interface FluoriteNumber : FluoriteValue {
    val value: Number
    fun negate(): FluoriteNumber
    fun roundToInt(): Int
}

class FluoriteInt(override val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "+_" to FluoriteFunction { it[0] as FluoriteInt },
                    "?_" to FluoriteFunction { ((it[0] as FluoriteInt).value != 0).toFluoriteBoolean() },
                    "$&_" to FluoriteFunction { "${(it[0] as FluoriteInt).value}".toFluoriteString() },
                    "_+_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteInt
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteInt(left.value + right.value)
                            is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                            else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                        }
                    },
                )
            )
        }
        val MINUS_ONE = FluoriteInt(-1)
        val ZERO = FluoriteInt(0)
        val ONE = FluoriteInt(1)
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteInt && value == other.value
    override fun hashCode() = value
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteInt(-value)
    override fun roundToInt() = value
}

class FluoriteDouble(override val value: Double) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    "+_" to FluoriteFunction { it[0] as FluoriteDouble },
                    "?_" to FluoriteFunction { ((it[0] as FluoriteDouble).value != 0.0).toFluoriteBoolean() },
                    "$&_" to FluoriteFunction { "${(it[0] as FluoriteDouble).value}".toFluoriteString() },
                    "_+_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteDouble
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteDouble(left.value + right.value)
                            is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                            else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                        }
                    },
                )
            )
        }
        val ZERO = FluoriteDouble(0.0)
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteDouble && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
    override fun negate() = FluoriteDouble(-value)
    override fun roundToInt() = value.roundToInt()
}
