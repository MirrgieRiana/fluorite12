package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.compilers.objects.FluoriteArray
import mirrg.fluorite12.compilers.objects.FluoriteStream
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.collect

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
