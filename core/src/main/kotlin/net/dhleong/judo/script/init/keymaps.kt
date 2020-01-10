package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.LabeledAs
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject

/**
 * @author dhleong
 */
fun ScriptInitContext.initKeymaps() = sequence {
    val context = this@initKeymaps

    // mapping functions
    yieldAll(sequenceOf(
        "" to "",
        "c" to "cmd",
        "i" to "insert",
        "n" to "normal"
    ).map { (letter, modeName) ->
        PrefixedMapScripting(
            context, letter, modeName
        )
    })

    yield(GlobalMapScripting(context))
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class GlobalMapScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Create a mapping in a specific mode from inputKeys to outputKeys.
        If remap is provided and True, the outputKeys can trigger other mappings.
        Otherwise, they will be sent as-is.
    """)
    fun createMap(modeName: String, inputKeys: String, @LabeledAs("String/Fn") output: Any) = createMap(
        modeName, inputKeys, output, true
    )
    fun createMap(
        modeName: String,
        inputKeys: String,
        @LabeledAs("String/Fn") output: Any,
        remap: Boolean
    ) = with(context) {
        mode.createMap(modeName, inputKeys, output, remap)
    }

    @Doc("Delete a mapping in the specific mode with inputKeys")
    fun deleteMap(modeName: String, inputKeys: String) = with(context) {
        judo.unmap(modeName, inputKeys)
    }
}

@Suppress("unused")
class PrefixedMapScripting(
    private val context: ScriptInitContext,
    override val methodPrefix: String,
    private val modeName: String
) : ScriptingObject {

    @Doc("""
        Create a mapping in a specific mode from inputKeys to output
    """)
    fun map() = context.judo.printMappings(modeName)
    fun map(modeName: String) = context.judo.printMappings(modeName)
    fun map(
        inputKeys: String,
        // TODO union type?
        @LabeledAs("String/Fn") output: Any
    ) = with(context) {
        mode.createMap(modeName, inputKeys, output, remap = true)
    }

    @Doc("""
        Create a mapping in a specific mode from inputKeys to output
    """)
    fun noremap(
        inputKeys: String,
        // TODO union type?
        @LabeledAs("String/Fn") output: Any
    ) = with(context) {
        mode.createMap(modeName, inputKeys, output, remap = false)
    }

    @Doc("""
        Delete a mapping in the specific mode with inputKeys
    """)
    fun unmap(inputKeys: String) = with(context) {
        judo.unmap(modeName, inputKeys)
    }
}
