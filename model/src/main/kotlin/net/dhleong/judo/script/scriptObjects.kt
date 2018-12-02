package net.dhleong.judo.script

/**
 * Interface to [net.dhleong.judo.render.IJudoWindow] as
 * exposed to scripting languages
 * @author dhleong
 */
interface IScriptWindow {
    val id: Int
    val width: Int
    val height: Int

    val buffer: IScriptBuffer

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
    fun set(contents: List<String>)
}