package net.dhleong.judo.util

/**
 * Another functional construct inspired by Clojure's `(when)`
 */
inline fun <R> whenTrue(value: Boolean, factory: () -> R): R? =
    if (value) factory()
    else null
