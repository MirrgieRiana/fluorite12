package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.collect

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

class TryCatchRunner(private val leftRunners: List<Runner>, private val newFrameIndex: Int, private val argumentVariableIndex: Int, private val rightRunners: List<Runner>) : Runner {
    override suspend fun evaluate(env: Environment) {
        try {
            leftRunners.forEach {
                it.evaluate(env)
            }
        } catch (e: FluoriteException) {
            val newEnv = Environment(env, 1, 0)
            newEnv.variableTable[newFrameIndex][argumentVariableIndex] = e.value
            rightRunners.forEach {
                it.evaluate(newEnv)
            }
        }
    }

    override val code get() = "TryCatch[${leftRunners.code};$newFrameIndex;$argumentVariableIndex;${rightRunners.code}]"
}

class MountRunner(private val frameIndex: Int, private val mountIndex: Int, private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        env.mountTable[frameIndex][mountIndex] = (getter.evaluate(env) as FluoriteObject).map.toMap()
    }

    override val code get() = "Mount[$frameIndex;$mountIndex;${getter.code}]"
}

class AdditiveMountRunner(private val oldFrameIndex: Int, private val oldMountIndex: Int, private val newFrameIndex: Int, private val newMountIndex: Int, private val getter: Getter) : Runner {
    override suspend fun evaluate(env: Environment) {
        val old = env.mountTable[oldFrameIndex][oldMountIndex]!!
        val obj = (getter.evaluate(env) as FluoriteObject).map
        env.mountTable[newFrameIndex][newMountIndex] = old + obj
    }

    override val code get() = "AdditiveMount[$oldFrameIndex;$oldMountIndex;$newFrameIndex;$newMountIndex;${getter.code}]"
}
