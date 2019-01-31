package net.dhleong.judo.util

/**
 * Can be used in conjunction with [letelse] when you're
 * not in a proper function and can't return from a regular
 * let {} block. Looks like this:
 *
 * ```
 * iflet (map[key]) {
 *   somethingWith(it)
 * } ?: letelse {
 *   somethingElse
 * }
 * ```
 *
 * @author dhleong
 */
inline fun <T, R> iflet(value: T?, block: (T) -> R): R? {
    return value?.let {
        return block(it)
    }
}

inline fun <R> letelse(block: () -> R): R = block()

/**
 * Another functional construct inspired by Clojure's `(when)`
 */
inline fun <R> whenTrue(value: Boolean, factory: () -> R): R? =
    if (value) factory()
    else null
