package net.dhleong.judo.script

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.IMapManagerPublic
import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.trigger.ITriggerManager
import net.dhleong.judo.util.PatternSpec
import java.io.InputStream
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.script.Bindings
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * @author dhleong
 */
abstract class Jsr223ScriptingEngine(
    extension: String = "js" // built in to JVM
) : ScriptingEngine {

    abstract class Factory(
        private val forExtension: String = "js" // built in to JVM
    ) : ScriptingEngine.Factory {

        override val supportsDecorators: Boolean
            get() = forExtension == "py"

        override fun supportsFileType(ext: String): Boolean = ext == forExtension
    }

    private val sandbox = ScriptExecutionSandbox()

    protected val engine: ScriptEngine = ScriptEngineManager()
        .getEngineByExtension(extension).also {
            if (it !is Invocable) {
                throw IllegalArgumentException(
                    "JSR223 ScriptEngine $it does not support Invocable"
                )
            }
        }

    override fun register(entity: JudoScriptingEntity) {
        when (entity) {
            is JudoScriptingEntity.Constant<*> -> {
                engine.put(entity.name, entity.value)
            }

            is JudoScriptingEntity.Function<*> -> {
                engine.put(entity.name, toScript(entity))
            }
        }
    }

    override fun interrupt() {
        sandbox.interrupt()
    }

    override fun execute(code: String) = sandbox.execute {
        engine.eval(code)
    }

    override fun readFile(fileName: String, stream: InputStream) = sandbox.execute {
        engine.eval(stream.bufferedReader())
    }

    override fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec {
        val flagsSet = patternSpecFlagsToEnums(flags)
        return when (fromScript) {
            is String -> compileSimplePatternSpec(fromScript, flagsSet)
            is Bindings -> {
                // try to compile it as a Java regex Pattern;
                // that will be much more efficient than delegating
                // to Python regex stuff (since we have to allocate
                // arrays for just about every call)
                val patternAsString = fromScript["source"] as? String
                try {
                    val javaPattern = Pattern.compile(patternAsString)

                    // if we got here, huzzah! no compile issues
                    JavaRegexPatternSpec(
                        patternAsString!!,
                        javaPattern,
                        flagsSet
                    )
                } catch (e: PatternSyntaxException) {
                    scriptRegexToPatternSpec(fromScript)
                }

            }

            else -> throw IllegalArgumentException(
                "Invalid alias type: $fromScript (${fromScript.javaClass})")
        }
    }

    override fun toJava(fromScript: Any): Any {
        if (fromScript is Bindings) {
            val length = fromScript["length"]
            if ("call" !in fromScript && length is Int) {
                return Jsr223List(fromScript, length)
            }
        }
        return super.toJava(fromScript)
    }

    override fun toScript(fromJava: Any): Any = when (fromJava) {
        is JudoScriptingEntity.Function<*> ->
            fromJava.toFunctionalInterface(this)

        // wrap core judo interfaces in delegates to prevent
        // access to private members
        is IScriptJudo -> object : IScriptJudo by fromJava {}
        is ICurrentJudoObjects -> object : ICurrentJudoObjects by fromJava {}
        is IAliasManager -> object : IAliasManager by fromJava {}
        is IEventManager -> object : IEventManager by fromJava {}
        is ILogManager -> object : ILogManager by fromJava {}
        is IMapManagerPublic -> object : IMapManagerPublic by fromJava {
            override var window: IJudoWindow?
                get() = fromJava.window
                set(value) {
                    // unpack for simplicity
                    fromJava.window = (value as? Jsr223Window)?.window ?: value
                }
        }
        is IPromptManager -> object : IPromptManager by fromJava {}
        is ITriggerManager -> object : ITriggerManager by fromJava {}
        is JudoRendererInfo -> object : JudoRendererInfo by fromJava {}
        is IJudoTabpage -> object : IJudoTabpage by fromJava {}
        is IJudoMap -> object : IJudoMap by fromJava {}

        else -> super.toScript(fromJava)
    }

    protected open fun scriptRegexToPatternSpec(fromScript: Bindings): PatternSpec {
        throw UnsupportedOperationException(
            "Unsupported regex: $fromScript / ${fromScript.javaClass}"
        )
    }

    override fun wrapCore(judo: IJudoCore): IScriptJudo = Jsr223JudoCore(this, judo)

    override fun wrapWindow(
        tabpage: IJudoTabpage,
        window: IJudoWindow
    ): IScriptWindow = Jsr223Window(this, tabpage, window)
}

class Jsr223List(
    private val fromScript: Bindings, length: Int
) : AbstractList<Any?>() {

    override val size: Int = length

    override fun get(index: Int): Any? = fromScript[index.toString()]

}

class Jsr223JudoCore(
    private val engine: Jsr223ScriptingEngine,
    private val judo: IJudoCore
) : IScriptJudo, ICurrentJudoObjects, IJudoScrollable by judo {
    override val mapper: IMapManagerPublic
        get() = engine.toScript(judo.mapper) as IMapManagerPublic

    override val current: ICurrentJudoObjects = this

    override var tabpage: Any
        get() = engine.toScript(judo.renderer.currentTabpage)
        set(value) {
            judo.renderer.currentTabpage = engine.toJava(value) as IJudoTabpage
        }
    override var window: Any
        get() = engine.toScript(judo.renderer.currentTabpage.currentWindow)
        set(value) {
            judo.renderer.currentTabpage.currentWindow = engine.toJava(value) as IJudoWindow
        }
    override var buffer: Any
//        get() = engine.toScript(judo.renderer.currentTabpage.currentWindow.currentBuffer)
        get() = judo.renderer.currentTabpage.currentWindow.let { w ->
            Jsr223Buffer(w.currentBuffer)
        }
        set(_) {
            throw UnsupportedOperationException("TODO change buffer in Window")
        }
}

class Jsr223Window(
    private val engine: ScriptingEngine,
    private val tabpage: IJudoTabpage,
    internal val window: IJudoWindow
) : IScriptWindow, IJudoWindow by window {

    override val id: Int get() = window.id
    override val width: Int get() = window.width
    override val height: Int get() = window.visibleHeight
    override val buffer: IScriptBuffer
        get() = Jsr223Buffer(window.currentBuffer)

    override var hidden: Boolean
        get() = window.isWindowHidden
        set(value) { window.isWindowHidden = value }

    @Suppress("UNCHECKED_CAST")
    override var onSubmit: Any?
        get() = window.onSubmitFn?.let { engine.toScript(it) }
        set(value) {
            window.onSubmitFn = value?.let { engine.callableToFunction1(it) as (String) -> Unit }
        }

    override fun close() {
        tabpage.close(window)
    }

    override fun resize(width: Int, height: Int) {
        window.resize(width, height)
    }

}

class Jsr223Buffer(
    private val buffer: IJudoBuffer
) : IScriptBuffer {
    override val id: Int = buffer.id

    override val size: Int
        get() = buffer.size

    override fun append(line: String) {
        line.appendAsFlavorableTo(buffer)
    }

    override fun clear() {
        buffer.clear()
    }

    override fun deleteLast() {
        buffer.deleteLast()
    }

    override fun get(index: Int, flags: String): String {
        val line = buffer[index]
        return if ("color" in flags) {
            line.toAnsi()
        } else line.toString()
    }

    override fun set(contents: List<String>) {
        buffer.set(contents.toFlavorableList())
    }

    override fun set(index: Int, contents: String) {
        buffer[index] = contents.toFlavorable()
    }

}


