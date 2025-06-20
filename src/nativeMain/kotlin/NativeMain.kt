import kotlinx.cinterop.get
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.cli.cliMain
import platform.posix.__environ

actual fun getEnv(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var index = 0
    while (true) {
        val entryStringPointer = __environ?.get(index) ?: break
        val entryString = entryStringPointer.toKString()
        val keyLength = entryString.indexOf('=')
        if (keyLength >= 0) {
            val key = entryString.take(keyLength)
            val value = entryString.drop(keyLength + 1)
            result[key] = value
        }
        index++
    }
    return result
}

fun main(args: Array<String>) {
    cliMain(args) {
        runBlocking {
            it()
        }
    }
}
