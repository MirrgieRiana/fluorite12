package mirrg.fluorite12

fun FluoriteValue.instanceOf(clazz: FluoriteValue): Boolean {
    var currentObject: FluoriteValue? = this
    while (true) {
        if (currentObject == null) return false
        if (currentObject === clazz) return true
        currentObject = currentObject.parent
    }
}

fun FluoriteValue.getMethod(env: Environment, name: String): FluoriteValue? {
    var currentObject = if (this is FluoriteObject) this else parent
    while (true) {
        if (currentObject == null) return null

        val override = env.getOverride(Signature(currentObject, name))
        if (override != null) return override

        val value = currentObject.map[name]
        if (value != null) return value

        currentObject = currentObject.parent
    }
}

fun FluoriteValue.getMethod(name: String): FluoriteValue? {
    var currentObject = if (this is FluoriteObject) this else parent
    while (true) {
        if (currentObject == null) return null

        val value = currentObject.map[name]
        if (value != null) return value

        currentObject = currentObject.parent
    }
}

suspend fun FluoriteValue.callMethod(env: Environment, name: String, vararg arguments: FluoriteValue) = (this.getMethod(env, name) as FluoriteFunction).function(listOf(this, *arguments))
suspend fun FluoriteValue.callMethod(name: String, vararg arguments: FluoriteValue) = (this.getMethod(name) as FluoriteFunction).function(listOf(this, *arguments))

suspend fun FluoriteValue.toJson(env: Environment) = this.callMethod(env, "TO_JSON")
suspend fun FluoriteValue.toJson() = this.callMethod("TO_JSON")

suspend fun FluoriteValue.toFluoriteString(env: Environment) = this.callMethod(env, "TO_STRING") as FluoriteString
suspend fun FluoriteValue.toFluoriteString() = this.callMethod("TO_STRING") as FluoriteString

suspend fun FluoriteValue.toFluoriteBoolean(env: Environment) = (if (this is FluoriteBoolean) this else this.callMethod(env, "TO_BOOLEAN") as FluoriteBoolean)
suspend fun FluoriteValue.toFluoriteBoolean() = (if (this is FluoriteBoolean) this else this.callMethod("TO_BOOLEAN") as FluoriteBoolean)

suspend fun FluoriteValue.toBoolean(env: Environment) = this.toFluoriteBoolean(env).value
suspend fun FluoriteValue.toBoolean() = this.toFluoriteBoolean().value
