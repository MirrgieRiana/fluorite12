package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.Variable
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect

class ObjectEntryVariable(private val map: MutableMap<String, FluoriteValue>, private val key: String, private val getter: Getter) : Variable {
    override suspend fun get(env: Environment): FluoriteValue {
        val value = map[key]
        return if (value == null) {
            val newValue = getter.evaluate(env)
            map[key] = newValue
            newValue
        } else {
            value
        }
    }

    override suspend fun set(env: Environment, value: FluoriteValue) {
        map[key] = value
    }
}

class VariableDefinitionObjectInitializer(private val key: String, private val frameIndex: Int, private val variableIndex: Int, private val getter: Getter) : ObjectInitializer {
    override suspend fun initializeVariable(env: Environment, map: MutableMap<String, FluoriteValue>) {
        env.variableTable[frameIndex][variableIndex] = ObjectEntryVariable(map, key, getter)
    }

    override suspend fun evaluate(env: Environment, map: MutableMap<String, FluoriteValue>) {
        map[key] = getter.evaluate(env)
    }

    override val code get() = "VariableDefinition[$key;$frameIndex;$variableIndex;${getter.code}]"
}

class GetterObjectInitializer(private val entriesGetter: Getter) : ObjectInitializer {
    override suspend fun initializeVariable(env: Environment, map: MutableMap<String, FluoriteValue>) {

    }

    override suspend fun evaluate(env: Environment, map: MutableMap<String, FluoriteValue>) {
        val value = entriesGetter.evaluate(env)
        if (value is FluoriteStream) {
            value.collect { item ->
                require(item is FluoriteArray)
                require(item.values.size == 2)
                map[item.values[0].toString()] = item.values[1]
            }
        } else {
            require(value is FluoriteArray)
            require(value.values.size == 2)
            map[value.values[0].toString()] = value.values[1]
        }
    }

    override val code get() = "Getter[${entriesGetter.code}]"
}
