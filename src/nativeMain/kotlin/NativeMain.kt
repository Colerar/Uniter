import cli.cmd.runUniter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
  GlobalScope.runUniter(args)
}
