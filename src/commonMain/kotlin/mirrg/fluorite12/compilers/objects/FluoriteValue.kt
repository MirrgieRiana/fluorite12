package mirrg.fluorite12.compilers.objects

sealed interface FluoriteValue {
    companion object {
        // fluoriteクラスはlazyにしなければJSで初期化順序によるエラーが出る
        // https://youtrack.jetbrains.com/issue/KT-25796
        // 他の同様のプロパティも同じ
        val fluoriteClass by lazy {
            FluoriteObject(
                null, mutableMapOf(
                    "TO_STRING" to FluoriteFunction { "${it[0]}".toFluoriteString() },
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

fun FluoriteValue.getMethod(name: String): FluoriteValue? {
    var currentObject = parent
    while (true) {
        if (currentObject == null) return null

        val value = currentObject.map[name]
        if (value != null) return value

        currentObject = currentObject.parent
    }
}

suspend fun FluoriteValue.callMethod(name: String, arguments: Array<FluoriteValue> = arrayOf()): FluoriteValue {
    val method = this.getMethod(name) ?: throw RuntimeException("Method not found: $this::$name")
    return if (method is FluoriteFunction) {
        method.function(arrayOf(this, *arguments))
    } else {
        method.invoke(arrayOf(this, *arguments))
    }
}

suspend fun FluoriteValue.invoke(arguments: Array<FluoriteValue>) = this.callMethod("INVOKE", arguments)
suspend fun FluoriteValue.bind(arguments: Array<FluoriteValue>) = this.callMethod("BIND", arguments)
suspend fun FluoriteValue.toJson() = this.callMethod("TO_JSON")
suspend fun FluoriteValue.toFluoriteNumber(): FluoriteNumber = this.callMethod("TO_NUMBER").let { if (it is FluoriteNumber) it else it.toFluoriteNumber() }
suspend fun FluoriteValue.toFluoriteString(): FluoriteString = this.callMethod("TO_STRING").let { if (it is FluoriteString) it else it.toFluoriteString() }
suspend fun FluoriteValue.toFluoriteBoolean(): FluoriteBoolean = this.callMethod("TO_BOOLEAN").let { if (it is FluoriteBoolean) it else it.toFluoriteBoolean() }
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value
suspend fun FluoriteValue.contains(value: FluoriteValue) = this.callMethod("CONTAINS", arrayOf(value)).toFluoriteBoolean()
