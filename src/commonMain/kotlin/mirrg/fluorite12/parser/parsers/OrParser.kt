package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

class OrParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        for (parser in parsers) {
            val result = context.parseOrNull(parser, start)
            if (result != null) return result
        }
        return null
    }
}

fun <T : Any> or(vararg parsers: Parser<T>) = OrParser(parsers.toList())
operator fun <T : Any> Parser<T>.plus(other: Parser<T>) = OrParser(listOf(this, other))
operator fun <T : Any> OrParser<T>.plus(other: Parser<T>) = OrParser(this.parsers + other)
