package net.dhleong.judo.script

/**
 * Interface to [net.dhleong.judo.render.IJudoTabpage] as
 * exposed to scripting languages
 *
 * @author dhleong
 */
interface IScriptTabpage {
    val id: Int
    val width: Int
    val height: Int
}

/**
 * Interface to [net.dhleong.judo.render.IJudoWindow] as
 * exposed to scripting languages
 * @author dhleong
 */
interface IScriptWindow {
    val id: Int
    val width: Int
    val height: Int
    var hidden: Boolean

    val buffer: IScriptBuffer

    /** *Should* be `(String) -> Unit)` */
    var onSubmit: Any?

    fun close()
    fun resize(width: Int, height: Int)
}

/**
 * Interface to [net.dhleong.judo.render.IJudoBuffer] as
 * exposed to scripting languages
 */
interface IScriptBuffer {
    val id: Int

    /**
     * The number of lines currently in this buffer
     */
    val size: Int

    fun append(line: String)
    fun clear()
    fun get(index: Int, flags: String = ""): String
    fun deleteLast()
    fun set(contents: List<String>)
    fun set(index: Int, contents: String)
}