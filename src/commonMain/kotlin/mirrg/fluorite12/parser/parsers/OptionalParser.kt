package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser
import mirrg.fluorite12.parser.Tuple1

class OptionalParser<out T : Any>(val parser: Parser<T>) : Parser<Tuple1<T?>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple1<T?>> {
        val result = context.parseOrNull(parser, start)
        return if (result != null) {
            ParseResult(Tuple1(result.value), result.start, result.end)
        } else {
            ParseResult(Tuple1(null), start, start)
        }
    }
}

val <T : Any> Parser<T>.optional get() = OptionalParser(this)
