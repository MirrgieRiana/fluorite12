package mirrg.fluorite12.compilers.objects

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import kotlin.math.roundToInt

interface FluoriteNumber : FluoriteValue {
    fun toInt(): Int
    fun toLong(): Long
    fun toDouble(): Double
    fun negate(): FluoriteNumber
    fun roundToInt(): Int
}

class FluoriteInt(val value: Int) : FluoriteNumber {
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
                    "_<=>_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteInt
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteInt(left.value.compareTo(right.value))
                            is FluoriteDouble -> FluoriteInt(left.value.compareTo(right.value))
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
    override fun toInt() = value
    override fun toLong() = value.toLong()
    override fun toDouble() = value.toDouble()
    override fun negate() = FluoriteInt(-value) // TODO 範囲を超える可能性がある
    override fun roundToInt() = value
}

class FluoriteLong(val value: Long) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf()
            )
        }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteLong && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
    override fun toInt() = if (value >= Int.MIN_VALUE.toLong() && value <= Int.MAX_VALUE.toLong()) value.toInt() else throw IllegalArgumentException("Can not convert to Int: $value")
    override fun toLong() = value
    override fun toDouble() = value.toDouble()
    override fun negate() = FluoriteLong(-value) // TODO 範囲を超える可能性がある
    override fun roundToInt() = value.toInt()
}

class FluoriteBig(val value: BigInteger) : FluoriteNumber {
    companion object {
        val fluoriteClass by lazy {
            FluoriteObject(
                FluoriteValue.fluoriteClass, mutableMapOf()
            )
        }
    }

    override fun toString() = value.toString()
    override fun equals(other: Any?) = other is FluoriteBig && value == other.value
    override fun hashCode() = value.hashCode()
    override val parent get() = fluoriteClass
    override fun toInt() = value.intValue(true)
    override fun toLong() = value.longValue(true)
    override fun toDouble() = value.doubleValue()
    override fun negate() = FluoriteBig(-value)
    override fun roundToInt() = value.intValue(true)
}

class FluoriteDouble(val value: Double) : FluoriteNumber {
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
                    "_<=>_" to FluoriteFunction { arguments ->
                        val left = arguments[0] as FluoriteDouble
                        when (val right = arguments[1]) {
                            is FluoriteInt -> FluoriteInt(left.value.compareTo(right.value))
                            is FluoriteDouble -> FluoriteInt(left.value.compareTo(right.value))
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
    override fun toInt() = value.toBigDecimal().intValue(true)
    override fun toLong() = value.toBigDecimal().longValue(true)
    override fun toDouble() = value
    override fun negate() = FluoriteDouble(-value)
    override fun roundToInt() = value.roundToInt()
}


private const val INT_POSITIVE_MAX_ABS = "2147483647"
private const val INT_NEGATIVE_MAX_ABS = "2147483648"
private const val INT_STRING_LENGTH = INT_POSITIVE_MAX_ABS.length
private const val LONG_POSITIVE_MAX_ABS = "9223372036854775807"
private const val LONG_NEGATIVE_MAX_ABS = "9223372036854775808"
private const val LONG_STRING_LENGTH = LONG_POSITIVE_MAX_ABS.length

fun String.toFluoriteNumber(): FluoriteValue {
    if ('.' in this || 'e' in this || 'E' in this) return FluoriteDouble(this.toDouble())

    var string = this
    var isNegative = false

    // 符号を消す
    run sign@{
        if (string.isNotEmpty() && string[0] == '+') {
            string = string.drop(1)
            return@sign
        }
        if (string.isNotEmpty() && string[0] == '-') {
            isNegative = true
            string = string.drop(1)
            return@sign
        }
    }

    // 先頭のゼロを消す
    while (string.length >= 2 && string[0] == '0') {
        string = string.drop(1)
    }

    return if (!isNegative) {
        when {
            string.length < INT_STRING_LENGTH -> FluoriteInt(string.toInt())
            string.length == INT_STRING_LENGTH && string <= INT_POSITIVE_MAX_ABS -> FluoriteInt(string.toInt())
            string.length < LONG_STRING_LENGTH -> FluoriteLong(string.toLong())
            string.length == LONG_STRING_LENGTH && string <= LONG_POSITIVE_MAX_ABS -> FluoriteLong(string.toLong())
            else -> FluoriteBig(string.toBigInteger())
        }
    } else {
        when {
            string.length < INT_STRING_LENGTH -> FluoriteInt("-$string".toInt())
            string.length == INT_STRING_LENGTH && string <= INT_NEGATIVE_MAX_ABS -> FluoriteInt("-$string".toInt())
            string.length < LONG_STRING_LENGTH -> FluoriteLong("-$string".toLong())
            string.length == LONG_STRING_LENGTH && string <= LONG_NEGATIVE_MAX_ABS -> FluoriteLong("-$string".toLong())
            else -> FluoriteBig("-$string".toBigInteger())
        }
    }
}
