package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseContext
import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

class RegexParser(val regex: Regex) : Parser<MatchResult> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<MatchResult>? {
        @OptIn(ExperimentalStdlibApi::class)
        val matchResult = regex.matchAt(context.src, start) ?: return null
        return ParseResult(matchResult, start, matchResult.range.last + 1)
    }
}

fun Regex.toParser() = RegexParser(this)
operator fun Regex.unaryPlus() = this.toParser()
operator fun Regex.unaryMinus() = -+this
operator fun Regex.not() = !+this
