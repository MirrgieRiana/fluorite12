package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.FluoriteStream
import mirrg.fluorite12.collect

class GetterRunner(private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        val result = getter.evaluate(env)
        if (result is FluoriteStream) {
            result.collect {
                // イテレーションは行うがその結果は握りつぶす
            }
        }
    }

    override val code get() = "Getter[${getter.code}]"
}

class AssignmentRunner(private val setter: Setter, private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        val left = setter.evaluate(env)
        val right = getter.evaluate(env)
        left.invoke(right)
    }

    override val code get() = "Assignment[${setter.code};${getter.code}]"
}
