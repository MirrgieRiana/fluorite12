package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.ParseResult
import mirrg.fluorite12.parser.Parser

infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(result.value), result.start, result.end)
}

infix fun <I : Any, O : Any> Parser<I>.mapPositioned(function: (ParseResult<I>) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(result), result.start, result.end)
}
