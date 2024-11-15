package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.Formatter
import mirrg.fluorite12.FormatterConversion
import mirrg.fluorite12.FormatterFlag
import mirrg.fluorite12.removeExponent

suspend fun Formatter.format(value: FluoriteValue): String {
    val (sign, string) = if (this.conversion == FormatterConversion.STRING) {
        if (FormatterFlag.SIGNED in this.flags) throw RuntimeException("Invalid format: ${this.string}")
        if (FormatterFlag.SPACE_FOR_SIGN in this.flags) throw RuntimeException("Invalid format: ${this.string}")
        if (this.precision != null) throw RuntimeException("Invalid format: ${this.string}")
        Pair("", value.toFluoriteString().value)
    } else {
        if (FormatterFlag.SIGNED in this.flags && FormatterFlag.SPACE_FOR_SIGN in this.flags) throw RuntimeException("Invalid format: ${this.string}")
        if (this.conversion != FormatterConversion.FLOAT) {
            if (this.precision != null) throw RuntimeException("Invalid format: ${this.string}")
            val int = (value as FluoriteNumber).value.toInt()
            if (this.conversion == FormatterConversion.DECIMAL) {
                when {
                    int < 0 -> Pair("-", (-int).toString())
                    FormatterFlag.SIGNED in this.flags -> Pair("+", int.toString())
                    FormatterFlag.SPACE_FOR_SIGN in this.flags -> Pair(" ", int.toString())
                    else -> Pair("", "$int")
                }
            } else {
                when {
                    int < 0 -> Pair("-", (-int).toString(16))
                    FormatterFlag.SIGNED in this.flags -> Pair("+", int.toString(16))
                    FormatterFlag.SPACE_FOR_SIGN in this.flags -> Pair(" ", int.toString(16))
                    else -> Pair("", int.toString(16))
                }
            }
        } else {
            val double = (value as FluoriteNumber).value.toDouble()
            fun String.precision(): String {
                if (precision == null) return this // 精度の指定がなければ何もしない
                val index = this.indexOf('.')
                if (index == -1) { // もともと小数点が含まれない場合
                    return if (precision == 0) { // 精度0の場合
                        this
                    } else { // 精度が1以上の場合
                        "$this.${"0".repeat(precision)}"
                    }
                }
                val decimalLength = this.length - (index + 1)
                val lack = precision - decimalLength
                return if (lack == 0) { // 精度が丁度
                    this
                } else if (lack > 0) { // 精度が足りない
                    "$this${"0".repeat(lack)}"
                } else if (precision == 0) { // 精度が多すぎて、かつ指定精度が0
                    this.dropLast(-lack + 1)
                } else { // 精度が多すぎて、かつ指定精度が0以外
                    this.dropLast(-lack)
                }
            }
            when {
                double < 0 -> Pair("-", (-double).toString().removeExponent().precision())
                FormatterFlag.SIGNED in this.flags -> Pair("+", double.toString().removeExponent().precision())
                FormatterFlag.SPACE_FOR_SIGN in this.flags -> Pair(" ", double.toString().removeExponent().precision())
                else -> Pair("", double.toString().removeExponent().precision())
            }
        }
    }

    if (this.width == null) return sign + string
    val fillers = this.width - sign.length - string.length
    if (fillers <= 0) return sign + string
    return if (FormatterFlag.LEFT_ALIGNED in this.flags) {
        if (FormatterFlag.LEADING_ZEROS in this.flags) {
            sign + string + "0".repeat(fillers)
        } else {
            sign + string + " ".repeat(fillers)
        }
    } else {
        if (FormatterFlag.LEADING_ZEROS in this.flags) {
            sign + "0".repeat(fillers) + string
        } else {
            " ".repeat(fillers) + sign + string
        }
    }
}
