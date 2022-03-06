package cli

import cli.cmd.runUniter
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        runUniter(args)
    }
}
