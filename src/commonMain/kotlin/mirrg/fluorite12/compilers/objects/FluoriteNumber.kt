package mirrg.fluorite12.compilers.objects

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import mirrg.fluorite12.OperatorMethod
import mirrg.fluorite12.toFluoriteIntAsCompared
import kotlin.math.roundToInt

interface FluoriteNumber : FluoriteValue {
    fun toInt(): Int
    fun toDouble(): Double
    fun negate(): FluoriteNumber
    fun roundToInt(): Int
}

data class FluoriteInt(val value: Int) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    OperatorMethod.TO_NUMBER.methodName to FluoriteFunction { it[0] as FluoriteInt },
                    OperatorMethod.TO_BOOLEAN.methodName to FluoriteFunction { ((it[0] as FluoriteInt).value != 0).toFluoriteBoolean() },
                    OperatorMethod.PLUS.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteInt
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteInt(left.value + right.value)
                            is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                            else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                        }
                    },
                    OperatorMethod.COMPARE.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteInt
                        when (val right = arguments[1]) {
                            is FluoriteInt -> left.value.compareTo(right.value).toFluoriteIntAsCompared()
                            is FluoriteDouble -> left.value.compareTo(right.value).toFluoriteIntAsCompared()
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
    override val parent get() = fluoriteClass
    override fun toInt() = value
    override fun toDouble() = value.toDouble()
    override fun negate() = FluoriteInt(-value)
    override fun roundToInt() = value
}

data class FluoriteDouble(val value: Double) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf(
                    OperatorMethod.TO_NUMBER.methodName to FluoriteFunction { it[0] as FluoriteDouble },
                    OperatorMethod.TO_BOOLEAN.methodName to FluoriteFunction { ((it[0] as FluoriteDouble).value != 0.0).toFluoriteBoolean() },
                    OperatorMethod.PLUS.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteDouble
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteDouble(left.value + right.value)
                            is FluoriteDouble -> FluoriteDouble(left.value + right.value)
                            else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                        }
                    },
                    OperatorMethod.COMPARE.methodName to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteDouble
                        when (val right = arguments[1]) {
                            is FluoriteInt -> left.value.compareTo(right.value).toFluoriteIntAsCompared()
                            is FluoriteDouble -> left.value.compareTo(right.value).toFluoriteIntAsCompared()
                            else -> throw IllegalArgumentException("Can not convert to number: ${right::class}")
                        }
                    },
                )
            )
        }
        val ZERO = FluoriteDouble(0.0)
    }

    override fun toString() = value.toString()
    override val parent get() = fluoriteClass
    override fun toInt() = value.toBigDecimal().intValue(true)
    override fun toDouble() = value
    override fun negate() = FluoriteDouble(-value)
    override fun roundToInt() = value.roundToInt()
}

fun String.toFluoriteNumber(): FluoriteNumber {
    return when {
        "." !in this -> when (val int = this.toInt()) {
            0 -> FluoriteInt.ZERO
            1 -> FluoriteInt.ONE
            else -> FluoriteInt(int)
        }

        else -> when (val double = this.toDouble()) {
            0.0 -> FluoriteDouble.ZERO
            else -> FluoriteDouble(double)
        }
    }
}
