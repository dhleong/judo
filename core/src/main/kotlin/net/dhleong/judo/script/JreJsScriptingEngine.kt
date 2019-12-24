package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import javax.script.Bindings
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

    override fun callableArgsCount(fromScript: Any): Int =
        (fromScript as Bindings)["length"] as Int

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser {
        return { args ->
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript, *args
            ) as String?
        }
    }

    override fun <R> callableToFunction0(fromScript: Any): () -> R {
        return {
            @Suppress("UNCHECKED_CAST")
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript
            ) as R
        }
    }

    override fun callableToFunction1(fromScript: Any): (Any?) -> Any? {
        return { arg ->
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript,
                arg?.let { toScript(it) }
            )
        }
    }

    override fun callableToFunctionN(fromScript: Any): (Array<Any?>) -> Any? {
        return { args ->
            (engine as Invocable).invokeMethod(
                fromScript, "call", fromScript, *toScript(args)
            )
        }
    }
}