package mirrg.fluorite12

import com.github.h0tk3y.betterParse.lexer.TokenMatch

val List<TokenMatch>.text get() = this.joinToString("") { it.text }
