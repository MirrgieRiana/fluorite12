package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.OperatorMethod
import mirrg.fluorite12.operations.FluoriteException

interface FluoriteValue {
    companion object {
        // fluoriteクラスはlazyにしなければJSで初期化順序によるエラーが出る
        // https://youtrack.jetbrains.com/issue/KT-25796
        // 他の同様のプロパティも同じ
        val fluoriteClass by lazy {
            FluoriteObject(
                null, mutableMapOf(
                    OperatorMethod.TO_STRING.methodName to FluoriteFunction { "${it[0]}".toFluoriteString() },
                )
            )
        }
    }

    val parent: FluoriteObject?
}

fun FluoriteValue.instanceOf(clazz: FluoriteValue): Boolean {
    var currentObject: FluoriteValue? = this
    while (true) {
        if (currentObject == null) return false
        if (currentObject === clazz) return true
        currentObject = currentObject.parent
    }
}

fun interface Callable {
    suspend fun call(arguments: Array<FluoriteValue>): FluoriteValue
}

private fun FluoriteValue.getPureMethod(name: String): FluoriteValue? {
    var currentObject = this.parent
    while (true) {
        if (currentObject == null) return null

        val value = currentObject.map[name]
        if (value != null) return value

        currentObject = currentObject.parent
    }
}

suspend fun FluoriteValue.getMethod(name: String): Callable? {
    val method = this.getPureMethod(name) ?: run {
        val fallbackMethod = this.getPureMethod(OperatorMethod.METHOD.methodName) ?: return null
        val actualMethod = this.callMethod(fallbackMethod, arrayOf(name.toFluoriteString()))
        if (actualMethod == FluoriteNull) return null
        return Callable { arguments ->
            actualMethod.invoke(arguments)
        }
    }
    return Callable { arguments ->
        this.callMethod(method, arguments)
    }
}

suspend fun FluoriteValue.callMethod(name: String, arguments: Array<FluoriteValue> = arrayOf()): FluoriteValue {
    val callable = this.getMethod(name) ?: throw FluoriteException("Method not found: $this::$name".toFluoriteString())
    return callable.call(arguments)
}

suspend fun FluoriteValue.callMethod(method: FluoriteValue, arguments: Array<FluoriteValue> = arrayOf()): FluoriteValue {
    return if (method is FluoriteFunction) {
        method.function(arrayOf(this, *arguments))
    } else {
        method.invoke(arrayOf(this, *arguments))
    }
}

suspend fun FluoriteValue.invoke(arguments: Array<FluoriteValue>) = this.callMethod(OperatorMethod.CALL.methodName, arguments)
suspend fun FluoriteValue.bind(arguments: Array<FluoriteValue>) = this.callMethod(OperatorMethod.BIND.methodName, arguments)
suspend fun FluoriteValue.toFluoriteNumber(): FluoriteNumber = this.callMethod(OperatorMethod.TO_NUMBER.methodName).let { if (it is FluoriteNumber) it else it.toFluoriteNumber() }
suspend fun FluoriteValue.toFluoriteString(): FluoriteString = this.callMethod(OperatorMethod.TO_STRING.methodName).let { if (it is FluoriteString) it else it.toFluoriteString() }
suspend fun FluoriteValue.toFluoriteBoolean(): FluoriteBoolean = this.callMethod(OperatorMethod.TO_BOOLEAN.methodName).let { if (it is FluoriteBoolean) it else it.toFluoriteBoolean() }
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value
suspend fun FluoriteValue.contains(value: FluoriteValue) = this.callMethod(OperatorMethod.CONTAINS.methodName, arrayOf(value)).toFluoriteBoolean()
suspend fun FluoriteValue.plus(value: FluoriteValue) = this.callMethod(OperatorMethod.PLUS.methodName, arrayOf(value))
suspend fun FluoriteValue.compareTo(value: FluoriteValue) = this.callMethod(OperatorMethod.COMPARE.methodName, arrayOf(value)) as FluoriteInt
