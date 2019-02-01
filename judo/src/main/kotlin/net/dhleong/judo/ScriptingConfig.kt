package net.dhleong.judo

import net.dhleong.judo.script.JreJsScriptingEngine
import net.dhleong.judo.script.JythonScriptingEngine
import net.dhleong.judo.script.ScriptingEngine
import java.io.File

/**
 * @author dhleong
 */
class ScriptingConfig(
    val engineFactory: ScriptingEngine.Factory,
    val userConfigFile: File
) {
    companion object {

        private val scriptingTypes = listOf(
            "init.py" to JythonScriptingEngine.Factory(),
            "init.js" to JreJsScriptingEngine.Factory()
        )

        fun pick(
            configDir: File,
            requestedScriptFile: File?
        ): ScriptingConfig {
            if (requestedScriptFile != null) {
                // if there's no extension, or it's weirdly long, it's
                // probably actually a URI and not a world script file
                val ext = requestedScriptFile.extension
                if (!(ext.isEmpty() || ext.length > 4)) {
                    // choose scripting file and engine by provided script,
                    // even if it doesn't exist or we can't read it
                    return byExtension(configDir, ext)
                }
            }

            // no script file? pick based on the contents of configDir
            for ((fileName, engineFactory) in scriptingTypes) {
                val file = File(configDir, fileName)
                if (file.exists()) {
                    return ScriptingConfig(engineFactory, file)
                }
            }

            // fall back to the default
            return ScriptingConfig(
                scriptingTypes[0].second,
                File(configDir, scriptingTypes[0].first)
            )
        }

        private fun byExtension(configDir: File, ext: String) = when (ext) {
            "js" -> ScriptingConfig(
                JreJsScriptingEngine.Factory(),
                File(configDir, "init.js")
            )

            "py" -> ScriptingConfig(
                JythonScriptingEngine.Factory(),
                File(configDir, "init.py")
            )

            else -> throw IllegalArgumentException(
                "Unsupported scripting type: `$ext`"
            )
        }
    }
}