package cli

import cli.cmd.runUniter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(ExperimentalJsExport::class, DelicateCoroutinesApi::class)
@JsExport @JsName("gitStandup")
fun main(args: Array<String>) {
    GlobalScope.promise {
        runUniter(args)
    }
}
