package mirrg.fluorite12.parser.parsers

import hasFreeze
import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

class CharParser(val char: Char) : Parser<Char> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Char>? {
        if (start >= context.src.length) return null
        if (context.src[start] != char) return null
        return ParseResult(char, start, start + 1)
    }

    companion object {
        val cache = mutableMapOf<Char, CharParser>()
    }
}

fun Char.toParser() = if (hasFreeze()) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }
operator fun Char.unaryPlus() = this.toParser()
operator fun Char.unaryMinus() = -+this
operator fun Char.not() = !+this
