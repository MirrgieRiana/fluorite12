package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.FluoriteValue

interface Operation {
    val code: String
}

val List<Operation>.code get() = this.joinToString(",") { it.code }

interface Getter : Operation {
    suspend fun evaluate(env: Environment): FluoriteValue
}

interface StringGetter : Operation {
    suspend fun evaluate(env: Environment): String
}

interface Runner : Operation {
    suspend fun evaluate(env: Environment)
}
