package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.OrCombinator
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.EmptyParser
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser

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
    .replace("\n", "\\n")
    .replace("\"", "\\\"")

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
