package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.contains
import mirrg.fluorite12.compilers.objects.instanceOf

object EqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> a == b }
    override val code get() = "Equal"
}

object NotEqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> a != b }
    override val code get() = "NotEqual"
}

object GreaterComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() > (b as FluoriteNumber).value.toDouble() }
    override val code get() = "Greater"
}

object LessComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() < (b as FluoriteNumber).value.toDouble() }
    override val code get() = "Less"
}

object GreaterEqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() >= (b as FluoriteNumber).value.toDouble() }
    override val code get() = "GreaterEqual"
}

object LessEqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() <= (b as FluoriteNumber).value.toDouble() }
    override val code get() = "LessEqual"
}

object InstanceOfComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> a.instanceOf(b) }
    override val code get() = "InstanceOf"
}

object ContainsComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> b.contains(a).value }
    override val code get() = "Contains"
}
