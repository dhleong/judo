package net.dhleong.judo.render

/**
 * @author dhleong
 */
interface BufferStorage<T> : MutableList<T> {
    fun removeLast(): T
}