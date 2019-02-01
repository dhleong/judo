package net.dhleong.judo

import net.dhleong.judo.net.createURI
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/**
 * @author dhleong
 */
sealed class InitStrategy(
    val config: ScriptingConfig
) {
    fun perform(judo: JudoCore): Boolean {

        // if they have a global init.py, read it
        if (config.userConfigFile.exists()) {
            judo.readFile(config.userConfigFile)
        }

        return doPerform(judo)
    }

    abstract fun doPerform(judo: JudoCore): Boolean

    /**
     * An Init strategy that does nothing extra
     */
    class Nop(config: ScriptingConfig) : InitStrategy(config) {
        override fun doPerform(judo: JudoCore): Boolean = true
    }

    class WorldScript(
        config: ScriptingConfig,
        val worldScriptFile: File
    ) : InitStrategy(config) {
        override fun doPerform(judo: JudoCore): Boolean {
            if (!(worldScriptFile.exists() && worldScriptFile.canRead())) {
                judo.quit()
                judo.renderer.close()

                System.err.println("Unable to open world file $worldScriptFile")
                System.exit(2)
                return false
            }

            judo.readFile(worldScriptFile)
            return true
        }
    }

    class Uri(
        config: ScriptingConfig,
        val uri: URI
    ) : InitStrategy(config) {
        override fun doPerform(judo: JudoCore): Boolean {
            judo.connect(uri)
            return true
        }
    }

    companion object {
        fun pick(args: List<String>): InitStrategy {
            // TODO we should extract flags here
            val worldScriptFile = if (args.size == 1) {
                File(
                    args[0].replace("^~", USER_HOME)
                ).absoluteFile
            } else null

            // figure out what our scripting config should be
            val config = ScriptingConfig.pick(USER_CONFIG_DIR, worldScriptFile)

            return when (args.size) {
                0 -> Nop(config)

                1 -> {
                    // try URI first
                    val uri = try {

                        val uri = createURI(args[0])
                        when {
                            uri.port < 0 && ":" !in args[0] -> null
                            uri.port < 0 -> throw URISyntaxException(args[0], "Invalid port")
                            else -> Uri(config, uri)
                        }

                    } catch (e: URISyntaxException) {

                        if (":" in args[0]) {
                            // this cannot be a script file; just raise the ex
                            throw e
                        }

                        null
                    }

                    if (uri != null) {
                        uri
                    } else {
                        // read world.(script)
                        if (worldScriptFile == null) {
                            throw IllegalStateException()
                        }
                        WorldScript(config, worldScriptFile)
                    }
                }

                2 -> {
                    // connect directly
                    val host = args[0]
                    val port = try {
                        args[1].toInt()
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Invalid port: ${args[1]}")
                    }

                    Uri(config, createURI("$host:$port"))
                }

                else -> throw IllegalArgumentException("Unexpected number of arguments")
            }
        }
    }
}