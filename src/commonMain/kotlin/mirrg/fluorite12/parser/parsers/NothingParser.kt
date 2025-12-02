package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

object NothingParser : Parser<Nothing> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Nothing>? {
        return null
    }
}

val nothing get() = NothingParser
