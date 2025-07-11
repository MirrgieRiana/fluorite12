package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.OrCombinator
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.EmptyParser
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteBoolean
import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteString
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.callMethod
import mirrg.fluorite12.compilers.objects.collect
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import mirrg.fluorite12.compilers.objects.toFluoriteString

val List<TokenMatch>.text get() = this.joinToString("") { it.text }

fun <T> OrCombinator(vararg parsers: Parser<T>) = OrCombinator(parsers.toList())

object NoToken : ErrorResult()

object AnyParser : Parser<TokenMatch> {
    override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int): ParseResult<TokenMatch> {
        return tokens[fromPosition] ?: return NoToken
    }
}

object NotMismatch : ErrorResult()

class NotParser<T>(private val parser: Parser<T>) : Parser<Unit> {
    override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int): ParseResult<Unit> {
        val result = parser.tryParse(tokens, fromPosition)
        return when (result) {
            is ErrorResult -> EmptyParser.tryParse(tokens, fromPosition)
            is Parsed -> NotMismatch
        }
    }
}

fun String.escapeJsonString() = this
    .replace("\\", "\\\\")
    .replace("\n", "\\n")
    .replace("\"", "\\\"")

private val jsons = mutableMapOf<String, Json>()

suspend fun FluoriteValue.toJsonFluoriteValue(indent: String? = null): FluoriteValue {
    return if (this is FluoriteStream) {
        FluoriteStream {
            this@toJsonFluoriteValue.collect {
                emit(it.toJsonFluoriteValue(indent = indent))
            }
        }
    } else {
        suspend fun FluoriteValue.toJsonElement(): JsonElement = when (this) {
            is FluoriteObject -> JsonObject(this.map.mapValues { it.value.toJsonElement() })
            is FluoriteArray -> JsonArray(this.values.map { it.toJsonElement() })
            is FluoriteInt -> JsonPrimitive(this.value)
            is FluoriteDouble -> JsonPrimitive(this.value)
            is FluoriteString -> JsonPrimitive(this.value)
            is FluoriteBoolean -> JsonPrimitive(this.value)
            FluoriteNull -> JsonNull
            else -> this.callMethod("$&_").toJsonElement()
        }

        val jsonElement = this.toJsonElement()

        if (indent == null) return Json.encodeToString(jsonElement).toFluoriteString()
        val oldJson = jsons[indent]
        val json = if (oldJson != null) {
            oldJson
        } else {
            val newJson = Json {
                prettyPrint = true
                prettyPrintIndent = indent
            }
            if (jsons.size >= 10) jsons.clear()
            jsons[indent] = newJson
            newJson
        }
        json.encodeToString(jsonElement).toFluoriteString()
    }
}

suspend fun FluoriteValue.toFluoriteValueAsJson(): FluoriteValue {
    return if (this is FluoriteStream) {
        FluoriteStream {
            this@toFluoriteValueAsJson.collect {
                emit(it.toFluoriteValueAsJson())
            }
        }
    } else {
        fun JsonElement.toFluoriteValue(): FluoriteValue = when (this) {
            is JsonObject -> FluoriteObject(FluoriteObject.fluoriteClass, this.mapValues { it.value.toFluoriteValue() }.toMutableMap())
            is JsonArray -> FluoriteArray(this.map { it.toFluoriteValue() }.toMutableList())
            JsonNull -> FluoriteNull
            is JsonPrimitive -> when {
                this.isString -> this.content.toFluoriteString()
                this.content == "true" -> FluoriteBoolean.TRUE
                this.content == "false" -> FluoriteBoolean.FALSE
                else -> this.content.toFluoriteNumber()
            }
        }

        Json.decodeFromString<JsonElement>(this.toFluoriteString().value).toFluoriteValue()
    }
}

class CachedParser<T>(private val parser: Parser<T>) : Parser<T> {
    private val cacheTable = mutableMapOf<Int, ParseResult<T>>()

    override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int): ParseResult<T> {
        val result = cacheTable[fromPosition]
        return if (result != null) {
            result
        } else {
            val newResult = parser.tryParse(tokens, fromPosition)
            cacheTable[fromPosition] = newResult
            newResult
        }
    }

    fun clear() = cacheTable.clear()
}

/**
 * 指数表記の文字列を小数表記の文字列にします。
 * 指数表記にはeもしくはEを使うことができます。
 * 先頭と末尾の不要な0は常に削除されます。
 */
fun String.removeExponent(): String {

    // 指数表記の分割
    var mantissa: String
    val exponent: Int
    run a@{

        // eを含む指数表記
        run {
            val index = this.indexOf('e')
            if (index != -1) {
                mantissa = this.take(index)
                exponent = this.drop(index + 1).toInt()
                return@a
            }
        }

        // Eを含む指数表記
        run {
            val index = this.indexOf('E')
            if (index != -1) {
                mantissa = this.take(index)
                exponent = this.drop(index + 1).toInt()
                return@a
            }
        }

        // 指数表記ではない
        mantissa = this
        exponent = 0
    }

    // 仮数部から符号を除去
    val sign = when {
        mantissa.startsWith('-') -> '-'
        mantissa.startsWith('+') -> '+'
        else -> null
    }
    if (sign != null) mantissa = mantissa.drop(1)

    // 仮数部を小数点で分離
    var integer: String
    var decimal: String
    run {
        val index = mantissa.indexOf('.')
        if (index != -1) {
            // 小数点があった
            integer = mantissa.take(index)
            decimal = mantissa.drop(index + 1)
        } else {
            // 小数点がなかった
            integer = mantissa
            decimal = ""
        }
    }

    // 小数点の移動
    if (exponent > 0) {
        // 指数部が正
        // 数字を小数部から整数部に移動
        val amount = exponent

        // 小数部の右に0を補充
        val lack = amount - decimal.length
        if (lack > 0) decimal = (decimal + "0".repeat(lack))

        integer = (integer + decimal.take(amount))
        decimal = decimal.drop(amount)
    } else if (exponent < 0) {
        // 指数部が負
        // 数字を整数部から小数部に移動
        val amount = -exponent

        // 整数部の左に0を補充
        val lack = amount - integer.length
        if (lack > 0) integer = ("0".repeat(lack) + integer)

        decimal = (integer.takeLast(amount) + decimal)
        integer = integer.dropLast(amount)
    }

    // 余分な0の除去
    run {
        var i = 0
        while (i < integer.length) {
            if (integer[i] != '0') break
            i++
        }
        if (i > 0) integer = integer.drop(i)
    }
    run {
        var i = 0
        while (i < decimal.length) {
            if (decimal[decimal.length - 1 - i] != '0') break
            i++
        }
        if (i > 0) decimal = decimal.dropLast(i)
    }

    // 文字列化
    if (integer.isEmpty()) integer = "0" // 整数部が空だった場合、0を補填
    val real = if (decimal.isEmpty()) integer else "$integer.$decimal" // 小数部が空だった場合、小数点を追加しない
    val result = if (sign != null) "$sign$real" else real

    return result
}

// これを使わない場合、js環境で　null?.let { a(it) } ?: b　みたいに書くと a が実行されてしまうKotlin JSのバグを踏む
suspend fun <T : Any> MutableMap<String, FluoriteValue>.pop(key: String, block: suspend (FluoriteValue) -> T, or: suspend () -> T): T {
    return this.remove(key)?.let { block(it) } ?: or() // しかしなぜここの ?.let がうまくいくのかは謎
}

fun Int.toFluoriteIntAsCompared(): FluoriteInt {
    return when {
        this < 0 -> FluoriteInt.MINUS_ONE
        this == 0 -> FluoriteInt.ZERO
        else -> FluoriteInt.ONE
    }
}

object IterationAborted : Throwable()
