package mirrg.fluorite12

fun FluoriteValue.getMethod(frame: Frame, name: String): FluoriteValue? {
    var currentObject = if (this is FluoriteObject) this else parent
    while (true) {
        if (currentObject == null) return null

        val override = frame.getOverride(Signature(currentObject, name))
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

suspend fun FluoriteValue.callMethod(frame: Frame, name: String, vararg arguments: FluoriteValue) = (this.getMethod(frame, name) as FluoriteFunction).function(listOf(this, *arguments))
suspend fun FluoriteValue.callMethod(name: String, vararg arguments: FluoriteValue) = (this.getMethod(name) as FluoriteFunction).function(listOf(this, *arguments))

suspend fun FluoriteValue.toJson(frame: Frame) = this.callMethod(frame, "TO_JSON")
suspend fun FluoriteValue.toJson() = this.callMethod("TO_JSON")
