package net.dhleong.judo.render.flavor

/**
 * @author dhleong
 */
operator fun Flavor?.plus(other: Flavor?): Flavor? =
    if (this == null) other
    else this + other
