package net.dhleong.judo

import net.dhleong.judo.modes.CmdMode

private val modesField by lazy {
    JudoCore::class.java.getDeclaredField("modes").apply {
        isAccessible = true
    }
}

/**
 * @author dhleong
 */
val JudoCore.cmdMode: CmdMode
    get() = (modesField.get(this) as Map<*, *>).let { modes ->
        modes["cmd"] as CmdMode
    }


