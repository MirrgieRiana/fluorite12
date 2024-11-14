package mirrg.fluorite12

import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue

fun FluoriteValue.instanceOf(clazz: FluoriteValue): Boolean {
    var currentObject: FluoriteValue? = this
    while (true) {
        if (currentObject == null) return false
        if (currentObject === clazz) return true
        currentObject = currentObject.parent
    }
}

fun FluoriteValue.getMethod(name: String): FluoriteValue? {
    var currentObject = parent
    while (true) {
        if (currentObject == null) return null

        val value = currentObject.map[name]
        if (value != null) return value

        currentObject = currentObject.parent
    }
}

suspend fun FluoriteValue.callMethod(name: String, arguments: Array<FluoriteValue> = arrayOf()): FluoriteValue {
    val method = this.getMethod(name) ?: throw RuntimeException("Method not found: $this::$name")
    return if (method is FluoriteFunction) {
        method.function(arrayOf(this, *arguments))
    } else {
        method.invoke(arrayOf(this, *arguments))
    }
}

suspend fun FluoriteValue.invoke(arguments: Array<FluoriteValue>) = this.callMethod("INVOKE", arguments)
suspend fun FluoriteValue.bind(arguments: Array<FluoriteValue>) = this.callMethod("BIND", arguments)
suspend fun FluoriteValue.toJson() = this.callMethod("TO_JSON")
suspend fun FluoriteValue.toFluoriteNumber(): FluoriteNumber = this.callMethod("TO_NUMBER").let { if (it is FluoriteNumber) it else it.toFluoriteNumber() }
suspend fun FluoriteValue.toFluoriteString(): FluoriteString = this.callMethod("TO_STRING").let { if (it is FluoriteString) it else it.toFluoriteString() }
suspend fun FluoriteValue.toFluoriteBoolean(): FluoriteBoolean = this.callMethod("TO_BOOLEAN").let { if (it is FluoriteBoolean) it else it.toFluoriteBoolean() }
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value
suspend fun FluoriteValue.contains(value: FluoriteValue) = this.callMethod("CONTAINS", arrayOf(value)).toFluoriteBoolean()

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
