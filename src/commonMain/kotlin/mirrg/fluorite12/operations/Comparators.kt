package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.contains
import mirrg.fluorite12.compilers.objects.instanceOf

// TODO
object EqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> a == b }
    override val code get() = "Equal"
}

// TODO
object NotEqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> a != b }
    override val code get() = "NotEqual"
}

// TODO
object GreaterComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() > (b as FluoriteNumber).value.toDouble() }
    override val code get() = "Greater"
}

// TODO
object LessComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() < (b as FluoriteNumber).value.toDouble() }
    override val code get() = "Less"
}

// TODO
object GreaterEqualComparator : Comparator {
    override suspend fun evaluate(env: Environment): suspend (FluoriteValue, FluoriteValue) -> Boolean = { a, b -> (a as FluoriteNumber).value.toDouble() >= (b as FluoriteNumber).value.toDouble() }
    override val code get() = "GreaterEqual"
}

// TODO
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
