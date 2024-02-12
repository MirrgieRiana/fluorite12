package mirrg.fluorite12

import com.github.h0tk3y.betterParse.combinators.OrCombinator
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.Parser

val List<TokenMatch>.text get() = this.joinToString("") { it.text }

fun <T> OrCombinator(vararg parsers: Parser<T>) = OrCombinator(parsers.toList())
