package net.dhleong.judo.script

import kotlinx.coroutines.runBlocking
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.modes.CmdMode
import java.io.File

/**
 * @author dhleong
 */
class ScriptInitContext(
    val judo: IJudoCore,
    val engine: ScriptingEngine,
    val userConfigFile: File,
    val mode: CmdMode,
    val completionSource: CompletionSource = DumbCompletionSource(normalize = false),
    val myRegisteredFns: MutableSet<String> = mutableSetOf(),
    val myRegisteredVars: MutableMap<String, JudoScriptingEntity> = mutableMapOf()
)

internal fun <T> ScriptInitContext.registerConst(name: String, doc: JudoScriptDoc, value: T) {
    registerVar(JudoScriptingEntity.Constant(name, doc, value))
}

internal fun <R> ScriptInitContext.registerFn(name: String, doc: JudoScriptDoc, fn: Function<R>) {
    registerVar(JudoScriptingEntity.Function(name, doc, fn))
    myRegisteredFns.add(name)
}

internal fun ScriptInitContext.registerVar(entity: JudoScriptingEntity) {
    engine.register(entity)
    myRegisteredVars[entity.name] = entity
    completionSource.process(entity.name)
}

internal fun ScriptInitContext.compilePatternSpec(fromScript: Any, flags: String) =
    engine.compilePatternSpec(fromScript, flags)

/**
 * Adapt a suspending call into JudoCore as for the synchronous
 * scripting API.
 */
internal inline fun <R> ScriptInitContext.adaptSuspend(crossinline block: suspend () -> R): R =
// NOTE: we do not run on the JudoCore.dispatcher, since feedKey ensures
// keys are processed there, and explicitly providing the dispatcher to
    // runBlocking can cause deadlocks
    runBlocking {
        block()
    }

