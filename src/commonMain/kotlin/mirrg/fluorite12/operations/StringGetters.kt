package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.Formatter
import mirrg.fluorite12.escapeJsonString
import mirrg.fluorite12.format
import mirrg.fluorite12.toFluoriteString

class LiteralStringGetter(private val string: String) : StringGetter {
    override suspend fun evaluate(env: Environment) = string
    override val code get() = "Literal[${string.escapeJsonString()}]"
}

class ConversionStringGetter(private val getter: Getter) : StringGetter {
    override suspend fun evaluate(env: Environment) = getter.evaluate(env).toFluoriteString().value
    override val code get() = "Conversion[${getter.code}]"
}

class FormattedStringGetter(private val formatter: Formatter, private val getter: Getter) : StringGetter {
    override suspend fun evaluate(env: Environment) = formatter.format(getter.evaluate(env))
    override val code get() = "Formatted[${formatter.string.escapeJsonString()};${getter.code}]"
}
