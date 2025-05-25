import kotlinx.coroutines.runBlocking
import mirrg.fluorite12.cli.cliMain

fun main(args: Array<String>) {
    cliMain(args) {
        runBlocking {
            it()
        }
    }
}
