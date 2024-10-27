package mirrg.fluorite12

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

fun FluoriteValue.instanceOf(clazz: FluoriteValue): Boolean {
    var currentObject: FluoriteValue? = this
    while (true) {
        if (currentObject == null) return false
        if (currentObject === clazz) return true
        currentObject = currentObject.parent
    }
}

fun FluoriteValue.getMethod(env: Environment, name: String): FluoriteValue? {
    var currentObject = parent
    while (true) {
        if (currentObject == null) return null

        val override = env.getOverride(Signature(currentObject, name))
        if (override != null) return override

        val value = currentObject.map[name]
        if (value != null) return value

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

suspend fun FluoriteValue.callMethod(env: Environment, name: String, vararg arguments: FluoriteValue) = (this.getMethod(env, name) as FluoriteFunction).call(listOf(this, *arguments))
suspend fun FluoriteValue.callMethod(name: String, vararg arguments: FluoriteValue) = (this.getMethod(name) as FluoriteFunction).call(listOf(this, *arguments))

suspend fun FluoriteValue.toJson(env: Environment) = this.callMethod(env, "TO_JSON")
suspend fun FluoriteValue.toJson() = this.callMethod("TO_JSON")

suspend fun FluoriteValue.toFluoriteString(env: Environment): FluoriteString = this.callMethod(env, "TO_STRING").let { if (it is FluoriteString) it else it.toFluoriteString() }
suspend fun FluoriteValue.toFluoriteString(): FluoriteString = this.callMethod("TO_STRING").let { if (it is FluoriteString) it else it.toFluoriteString() }

suspend fun FluoriteValue.toFluoriteBoolean(env: Environment) = (if (this is FluoriteBoolean) this else this.callMethod(env, "TO_BOOLEAN") as FluoriteBoolean)
suspend fun FluoriteValue.toFluoriteBoolean() = (if (this is FluoriteBoolean) this else this.callMethod("TO_BOOLEAN") as FluoriteBoolean)

suspend fun FluoriteValue.toBoolean(env: Environment) = this.toFluoriteBoolean(env).value
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value

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

suspend fun FluoriteValue.fluoriteEquals(env: Environment, value: FluoriteValue): FluoriteBoolean {
    if (this is FluoriteStream) {
        if (value is FluoriteStream) {
            val thisIterable = flow(this.flowProvider)
            val valueIterable = flow(value.flowProvider)
            thisIterable.take(1)

        } else {
            return this.fluoriteEquals(env, FluoriteStream(value))
        }
    } else {
        if (value is FluoriteStream) {

        } else {

        }
    }
    return this.callMethod(env, "EQUALS", value) as FluoriteBoolean
}

suspend fun FluoriteValue.fluoriteEquals(value: FluoriteValue): FluoriteBoolean {
    return this.callMethod("EQUALS", value) as FluoriteBoolean
}
