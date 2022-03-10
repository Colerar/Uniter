package cli.cmd

import cli.BuildKonfig.version
import cli.CliConfig.COMMAND_NAME
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors.black
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
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

  private val expr by argument("expr", "Expression to calculate").optional()

  override fun run() {
    if (currentContext.invokedSubcommand != null) return
    if (expr.isNullOrBlank()) {
      with(Terminal()) {
        println((yellow)("v$version"))
        println("Use ${(brightYellow + bold on black)("uniter -h")} for help")
      }
      throw ProgramResult(0)
    }
  }
}
