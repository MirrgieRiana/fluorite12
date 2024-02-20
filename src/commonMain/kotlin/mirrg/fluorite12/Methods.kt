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

suspend fun FluoriteValue.toJson(frame: Frame) = (this.getMethod(frame, "TO_JSON") as FluoriteFunction).function(listOf(this))

suspend fun FluoriteValue.toJson() = (this.getMethod("TO_JSON") as FluoriteFunction).function(listOf(this))
