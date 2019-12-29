package net.dhleong.judo.render.flavor

import net.dhleong.judo.render.JudoColor

/**
 * [WrappedFlavor] is a memory-efficient [Flavor] implementation
 * for when a foreground or background color is full RGB
 *
 * @author dhleong
 */
data class WrappedFlavor(
    private val base: IntFlavor,
    private val fg: JudoColor? = null,
    private val bg: JudoColor? = null
) : Flavor by base {
    override val foreground: JudoColor
        get() = fg ?: base.foreground
    override val background: JudoColor
        get() = bg ?: base.background

    override fun equals(other: Any?): Boolean = areFlavorsEqual(this, other)
    override fun hashCode(): Int = hash()
}