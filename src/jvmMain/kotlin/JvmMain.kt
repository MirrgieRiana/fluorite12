import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.cli.cliMain

actual fun getEnv(): Map<String, String> = System.getenv()

fun main(args: Array<String>) {
    cliMain(args) {
        runBlocking {
            it()
        }
    }
}
