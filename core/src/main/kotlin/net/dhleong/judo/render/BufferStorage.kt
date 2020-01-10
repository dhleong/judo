package net.dhleong.judo.render

/**
 * @author dhleong
 */
interface BufferStorage<T> : MutableList<T> {
    fun removeLast(): T

    /**
     * Persist the storage, if applicable
     */
    fun save() { /* nop, by default*/ }
}