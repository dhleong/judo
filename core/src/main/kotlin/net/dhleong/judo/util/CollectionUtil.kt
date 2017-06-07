package net.dhleong.judo.util

/**
 * @author dhleong
 */
fun <E> MutableIterable<E>.removeWhile(filter: (E) -> Boolean): Boolean {
    var removed = false
    val each = iterator()
    while (each.hasNext()) {
        if (filter(each.next())) {
            each.remove()
            removed = true
        } else {
            break
        }
    }
    return removed
}
