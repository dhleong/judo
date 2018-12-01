package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import javax.script.Invocable

/**
 * A Javascript scripting engine using the JS scripting
 * support built into the Java Runtime
 * @author dhleong
 */
class JreJsScriptingEngine : Jsr223ScriptingEngine(extension = "js") {
    class Factory : Jsr223ScriptingEngine.Factory(forExtension = "js") {
        override fun create(): ScriptingEngine = JreJsScriptingEngine()
        override fun toString(): String = "JS ScriptingFactory"
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser {
        return { args ->
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript, *args
            ) as String? ?: ""
        }
    }

    override fun callableToFunction0(fromScript: Any): () -> Unit {
        return {
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript
            )
        }
    }
}