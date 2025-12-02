package mirrg.fluorite12.parser.parsers

import mirrg.fluorite12.parser.Parser
import mirrg.fluorite12.parser.Tuple0

operator fun Parser<*>.unaryMinus() = this map { Tuple0 }
