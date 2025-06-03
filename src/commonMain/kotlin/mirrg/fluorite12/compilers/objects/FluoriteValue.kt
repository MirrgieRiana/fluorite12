package mirrg.fluorite12.compilers.objects

import mirrg.fluorite12.operations.FluoriteException

interface FluoriteValue {
    companion object {
        // fluoriteクラスはlazyにしなければJSで初期化順序によるエラーが出る
        // https://youtrack.jetbrains.com/issue/KT-25796
        // 他の同様のプロパティも同じ
        val fluoriteClass by lazy {
            FluoriteObject(
                null, mutableMapOf(
                    "&_" to FluoriteFunction { "${it[0]}".toFluoriteString() },
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
        val fallbackMethod = this.getPureMethod("_::_") ?: return null
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

suspend fun FluoriteValue.invoke(arguments: Array<FluoriteValue>) = this.callMethod("_()", arguments)
suspend fun FluoriteValue.bind(arguments: Array<FluoriteValue>) = this.callMethod("_[]", arguments)
suspend fun FluoriteValue.toFluoriteNumber(): FluoriteNumber = this.callMethod("+_").let { if (it is FluoriteNumber) it else it.toFluoriteNumber() }
suspend fun FluoriteValue.toFluoriteString(): FluoriteString = this.callMethod("&_").let { if (it is FluoriteString) it else it.toFluoriteString() }
suspend fun FluoriteValue.toFluoriteBoolean(): FluoriteBoolean = this.callMethod("?_").let { if (it is FluoriteBoolean) it else it.toFluoriteBoolean() }
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value
suspend fun FluoriteValue.contains(value: FluoriteValue) = this.callMethod("_@_", arrayOf(value)).toFluoriteBoolean()
suspend fun FluoriteValue.plus(value: FluoriteValue) = this.callMethod("_+_", arrayOf(value))
suspend fun FluoriteValue.compareTo(value: FluoriteValue) = this.callMethod("_<=>_", arrayOf(value)) as FluoriteInt
