package cli.cmd

import cli.BuildKonfig.version
import cli.CliConfig.COMMAND_NAME
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kotlinx.coroutines.CoroutineScope

fun CoroutineScope.runUniter(args: Array<String>) {
    CliCommand(this)
        .subcommands(
            Version()
        )
        .main(args)
}

class CliCommand(
    scope: CoroutineScope,
) : CliktCommand(
    help = """
       Uniter - A useful unit converter for cli - v$version
    """.trimIndent(),
    name = COMMAND_NAME,
    invokeWithoutSubcommand = true,
) {
    init {
        completionOption()
    }

    val expr by argument("expr", "Expr to calculate").optional()

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        if (expr.isNullOrBlank()) {
            echo("Expr is null")
            echo(getFormattedHelp())
        }
    }
}
