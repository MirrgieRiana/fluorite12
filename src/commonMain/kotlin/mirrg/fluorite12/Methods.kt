package mirrg.fluorite12

fun Frame.getMethod(receiver: FluoriteValue, name: String): FluoriteValue? {
    var currentObject = if (receiver is FluoriteObject) receiver else receiver.parent
    while (true) {
        if (currentObject == null) return null

        val override = this.getOverride(Signature(currentObject, name))
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

suspend fun Frame.toJson(value: FluoriteValue) = (this.getMethod(value, "TO_JSON") as FluoriteFunction).function(listOf(value))

suspend fun FluoriteValue.toJson() = (this.getMethod("TO_JSON") as FluoriteFunction).function(listOf(this))
