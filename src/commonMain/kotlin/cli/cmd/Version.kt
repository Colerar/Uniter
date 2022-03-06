package cli.cmd

import cli.BuildKonfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Version: CliktCommand(
    "Show version info",
    name = "version",
) {
    val long by option("-l", "--long", help = "Show Long Version, default: false")
        .flag("-s", "--short", default = false)

    override fun run() {
        if (long) echo("uniter version ${BuildKonfig.versionLong}")
        else echo("uniter version ${BuildKonfig.version}")
    }
}
