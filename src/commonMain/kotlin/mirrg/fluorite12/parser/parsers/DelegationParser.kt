package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

class DelegationParser<out T : Any>(val parserGetter: () -> Parser<T>) : Parser<T> {
    private val parser by lazy { parserGetter() }
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

fun <T : Any> parser(getter: () -> Parser<T>) = DelegationParser(getter)
