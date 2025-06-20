package mirrg.fluorite12

enum class OperatorMethod(val methodName: String) {
    PROPERTY("_._"),
    SET_PROPERTY("_._="),
    METHOD("_::_"),
    CALL("_()"),
    BIND("_[]"),
    TO_NUMBER("+_"),
    TO_BOOLEAN("?_"),
    TO_STRING("&_"),
    PLUS("_+_"),
    COMPARE("_<=>_"),
    CONTAINS("_@_"),
}
