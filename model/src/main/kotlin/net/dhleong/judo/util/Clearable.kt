package net.dhleong.judo.util

/**
 * @author dhleong
 */
interface Clearable<in T> {
    fun clear()
    fun clear(entry: T)
}