package net.dhleong.judo.modes

import kotlinx.coroutines.withContext
import net.dhleong.judo.ALL_SETTINGS
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.Setting
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.action
import net.dhleong.judo.input.keys
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.script.JudoScriptInvocation
import net.dhleong.judo.script.JudoScriptingEntity
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec
import net.dhleong.judo.util.SingleThreadDispatcher
import net.dhleong.judo.util.VisibleForTesting
import net.dhleong.judo.util.hash
import java.io.File
import java.io.InputStream

/**
 * @author dhleong
 */

abstract class BaseCmdMode(
    judo: IJudoCore,
    buffer: InputBuffer,
    private val rendererInfo: JudoRendererInfo,
    @VisibleForTesting
    internal val history: IInputHistory,
    private val userConfigDir: File,
    val userConfigFile: File
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    StatusBufferProvider {

    override val userMappings = KeyMapping()
    override val name = CmdMode.NAME

    private val mapClearable = MapClearable(judo)
    private val clearQueue = ArrayList<QueuedClear<*>>()
    private var currentClearableContext: String? = null

    protected val completionSource: CompletionSource = DumbCompletionSource(normalize = false)
    private val suggester = CompletionSuggester(completionSource)

    protected abstract val registeredFns: MutableSet<String>
    protected abstract val registeredVars: MutableMap<String, JudoScriptingEntity>

    private val dispatcher = SingleThreadDispatcher("judo:cmd")

    val mapping = KeyMapping(
        keys("<up>") to action { history.scroll(-1, clampCursor = false) },
        keys("<down>") to action { history.scroll(1, clampCursor = false) },

        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion())
    )
    private val input = MutableKeys()

    protected var lastReadFile: File? = null

    override fun onEnter() {
        clearBuffer()
    }

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                val code = buffer.toString().trim()
                handleEnteredCommand(code)
                return
            }

            key.char == 'c' && key.hasCtrl() -> {
                clearBuffer()
                exitMode()
                return
            }

            key.char == 'f' && key.hasCtrl() -> {
                val result = judo.readCommandLineInput(':', history, buffer.toString())
                if (result != null) {
                    handleEnteredCommand(result)
                }
                return
            }

            key.isTab() -> {
                performTabCompletionFrom(key, suggester)
                return
            }
        }

        // input changed; suggestions go away
        suggester.reset()

        // handle key mappings
        if (tryMappings(key, remap, input, mapping, userMappings)) {
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        insertChar(key)
    }

    private suspend fun handleEnteredCommand(code: String) {
        when (code) {
            "q", "q!", "qa", "qa!" -> {
                judo.quit()
                return
            }
        }

        clearBuffer()
        exitMode()

        // always add to history (if not empty)
        if (code.isNotBlank()) {
            history.push(code)
        }

        // some special no-arg "commands"
        if (handleNoArgListingCommand(code)) {
            return
        }

        if (code.startsWith("help")) {
            showHelp(code.substring(5))
        } else if (!(code.contains('(') && code.contains(')')) && code !in registeredFns) {
            showHelp(code)
        } else if (code in registeredFns) {
            handlingInterruption {
                // no args needed, so just implicitly handle for convenience
                executeImplicit(code)
            }
        } else {
            handlingInterruption {
                execute(code)
            }
        }
    }

    private suspend inline fun handlingInterruption(
        crossinline block: suspend () -> Unit
    ) = withContext(dispatcher) {
        try {
            block()
        } catch (e: InterruptedException) {
            judo.print("Script execution interrupted")
        }
    }

    override fun renderStatusBuffer() = FlavorableStringBuilder.withDefaultFlavor(":$buffer")
    override fun getCursor(): Int = buffer.cursor + 1

    fun load(pathToFile: String) {
        val file = File(pathToFile)
        readFile(file)
        judo.print("Loaded $file")
    }

    fun logToFile(path: String, options: String = "append plain") {
        val mode = when {
            options.contains("append", ignoreCase = true) -> ILogManager.Mode.APPEND
            else -> ILogManager.Mode.REPLACE
        }
        val format = when {
            options.contains("html", ignoreCase = true) -> ILogManager.Format.HTML
            options.contains("raw", ignoreCase = true) -> ILogManager.Format.RAW
            else -> ILogManager.Format.PLAIN
        }

        // TODO resolve relative paths relative to the lastLoaded file?
        judo.logging.configure(File(path), format, mode)
    }

    fun persistInput() {
        judo.connection?.let {
            val fileName = hash(it.toString())
            val historyDir = File(userConfigDir, "input-history")
            judo.persistInput(File(historyDir, fileName))
            return
        }

        throw IllegalStateException("You must be connected to use persistInput() without args")
    }

    fun readFile(file: File) {
        if (!(file.exists() && file.canRead())) {
            throw IllegalArgumentException("Unable to load $file")
        }

        file.inputStream().use {
            readFile(file, it)
        }
    }

    open fun readFile(file: File, inputStream: InputStream) {
        if (file != userConfigFile) {
            lastReadFile = file
        }

        val context = file.absolutePath
        withClearableContext(context) {
            clearQueuedForContext(context)

            readFile(file.name, inputStream)
        }
    }

    open fun reload() {
        lastReadFile?.let {
            // clear any split windows
            judo.tabpage.unsplit()

            readFile(it)
            judo.print("Reloaded $it")
            return
        }

        judo.print("No files read; nothing to reload")
    }

    internal fun config(args: Array<Any>) =
        when (args.size) {
            1 -> printSettingValue(args[0] as String)
            2 -> {
                val settingName = args[0] as String
                config(settingName, args[1])
                printSettingValue(settingName)
            }

            else -> {
                judo.print("Settings")
                judo.print("========")

                ALL_SETTINGS
                    .filter { it.value.description.isNotEmpty() }
                    .map { it.key }
                    .forEach(this::printSettingValue)
            }
        }

    private fun printSettingValue(settingName: String) =
        withSetting(settingName) { setting ->
            val value = judo.state[setting]
            val isDefaultFlag =
                if (value == setting.default) " (default)"
                else ""

            val valueDisp =
                if (value is String) """"$value""""
                else value

            judo.print("${setting.userName} = $valueDisp$isDefaultFlag")
        }

    private fun config(settingName: String, value: Any) {
        withSetting(settingName) {
            if (!it.type.isAssignableFrom(value.javaClass)) {
                throw ScriptExecutionException(
                    "$value is invalid for setting `$settingName` (requires: ${it.type})")
            }

            judo.state[it] = it.type.cast(value) as Any
        }
    }

    internal fun defineAlias(alias: PatternSpec, handler: Any) {
        queueAlias(alias.original)
        if (handler is String) {
            judo.aliases.define(alias, handler)
        } else {
            judo.aliases.define(alias, callableToAliasProcessor(handler))
        }
    }

    abstract fun callableToAliasProcessor(fromScript: Any): AliasProcesser

    private inline fun withSetting(settingName: String, block: (Setting<Any>) -> Unit) {
        ALL_SETTINGS[settingName]?.let {
            @Suppress("UNCHECKED_CAST")
            block(it as Setting<Any>)
            return
        }

        throw ScriptExecutionException("No such setting `$settingName`")
    }

    private fun handleNoArgListingCommand(command: String): Boolean =
        when (command) {
            "alias" -> {
                judo.printRaw()
                judo.printRaw(judo.aliases)
                true
            }

            "help" -> {
                showHelp()
                true
            }

            "cmap" -> {
                judo.printRaw()
                judo.printMappings("cmd")
                true
            }

            "imap" -> {
                judo.printRaw()
                judo.printMappings("insert")
                true
            }

            "nmap" -> {
                judo.printRaw()
                judo.printMappings("normal")
                true
            }

            "trigger" -> {
                judo.printRaw()
                judo.printRaw(judo.triggers)
                true
            }

            else -> false
        }

    internal fun showHelp() {
        val commands = registeredVars.values.asSequence()
            .sortedBy {
                // sort functions first, vars last
                if (it is JudoScriptingEntity.Function<*>) {
                    "0 - ${it.name}"
                } else {
                    "1 - ${it.name}"
                }
            }
            .map { it.name }
            .toList()
        val longest = commands.asSequence().map { it.length }.max()!!
        val colWidth = longest + 2

        if (colWidth >= rendererInfo.windowWidth) {
            // super small renderer (whaaat?)
            // just be lazy
            commands.forEach { judo.printRaw(it) }
            return
        }

        val cols = rendererInfo.windowWidth / colWidth
        val line = StringBuilder(cols * colWidth)
        var word = 0
        for (name in commands) {
            line.append(name)
            for (i in 0 until colWidth - name.length) {
                line.append(' ')
            }

            ++word

            if (word >= cols) {
                // end of the line; dump it and start over
                word = 0
                judo.printRaw(line.toString())
                line.setLength(0)
            }
        }

        if (line.isNotEmpty()) {
            judo.printRaw(line.toString())
        }
    }

    internal fun showHelp(command: String) {
        registeredVars[command]?.let { help ->
            help.formatHelp().forEach { judo.printRaw(it) }
            return
        }

        judo.printRaw("No such command: $command")
    }

    private fun exitMode() {
        judo.exitMode()
    }

    /**
     * Insert a key stroke at the current cursor position
     */
    private fun insertChar(key: Key) {
        val wasEmpty = buffer.isEmpty()
        buffer.type(key)
        if (buffer.isEmpty() && wasEmpty) {
            exitMode()
        }
    }

    abstract suspend fun execute(code: String)
    abstract suspend fun executeImplicit(fnName: String)

    protected abstract val supportsDecorators: Boolean

    private fun JudoScriptingEntity.formatHelp(): List<String> {
        val result = mutableListOf<String>()
        val maxUsageLength = doc.invocations?.let { invocations ->
            var max = 0
            invocations.forEach { usage ->
                result.add(format(usage, asDecorator = false).also {
                    max = maxOf(max, it.length)
                })

                if (supportsDecorators && usage.canBeDecorator) {
                    // NOTE the decorator version is never longer than
                    // the non-decorator, because it has less args!
                    result.add(format(usage, asDecorator = true))
                }
            }
            max
        } ?: name.let {
            result.add(it)
            it.length
        }

        result.add("=".repeat(maxUsageLength))

        result.addAll(doc.text.split("\n"))
        return result
    }

    protected open fun JudoScriptingEntity.format(
        usage: JudoScriptInvocation,
        asDecorator: Boolean
    ): String = StringBuilder().apply {

        if (asDecorator) {
            append("@")
        }

        append(name)
        append("(")

        val last = usage.args.lastIndex
        usage.args.forEachIndexed { index, arg ->
            if (index == last && asDecorator) return@forEachIndexed
            if (arg.isOptional) append("[")
            if (index > 0) append(", ")

            append(arg.name)
            append(": ")
            append(arg.type)

            if (arg.isOptional) append("]")
        }

        append(")")

        usage.returnType?.let {
            append(" -> ")
            append(it)
        }

    }.toString()

    protected abstract fun readFile(fileName: String, stream: InputStream)

    override fun clearBuffer() {
        super.clearBuffer()
        input.clear()
        suggester.reset()
        history.resetHistoryOffset()
    }

    private fun clearQueuedForContext(context: String) {
        val iter = clearQueue.iterator()
        while (iter.hasNext()) {
            val candidate = iter.next()
            if (candidate.context == context) {
                candidate.clear()
                iter.remove()
            }
        }
    }

    private fun queueAlias(specOriginal: String) =
        enqueueClear(judo.aliases, specOriginal)

    protected fun queueEvent(event: String, handler: EventHandler) =
        enqueueClear(judo.events, event to handler)

    protected fun queueMap(mode: String, keysFrom: String) =
        enqueueClear(mapClearable, mode to keysFrom)

    protected fun queuePrompt(spec: String) =
        enqueueClear(judo.prompts, spec)

    protected fun queueTrigger(spec: String) =
        enqueueClear(judo.triggers, spec)

    private fun <T> enqueueClear(source: Clearable<T>, entry: T) =
        currentClearableContext?.let {
            clearQueue.add(QueuedClear(it, source, entry))
        }

    private inline fun withClearableContext(context: String, block: () -> Unit) {
        val oldContext = currentClearableContext
        currentClearableContext = context
        try {
            block()
        } finally {
            currentClearableContext = oldContext
        }
    }

}

class ScriptExecutionException(
    traceback: String,
    override val cause: Throwable? = null
) : RuntimeException(traceback)

internal class MapClearable(private val judo: IJudoCore) : Clearable<Pair<String, String>> {
    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun clear(entry: Pair<String, String>) {
        val (mode, keys) = entry
        judo.unmap(mode, keys)
    }
}

internal class QueuedClear<T>(
    val context: String,
    private val source: Clearable<T>,
    private val entry: T
) {
    fun clear() {
        source.clear(entry)
    }
}
