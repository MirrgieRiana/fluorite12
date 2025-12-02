package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser
import mirrg.fluorite12.parser.Tuple0

object UnitParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        return ParseResult(Tuple0, start, start)
    }
}

fun <T : Any> unit(value: T) = UnitParser map { value }
