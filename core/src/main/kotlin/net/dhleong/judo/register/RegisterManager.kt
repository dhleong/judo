package net.dhleong.judo.register

import net.dhleong.judo.CLIPBOARD
import net.dhleong.judo.StateMap

/**
 * @author dhleong
 */
class RegisterManager(private val settings: StateMap) : IRegisterManager {

    // NOTE: this is not strictly correct; I think * is supposed to be
    // the "selection" register on X11 systems....
    private val clipboardRegister = ClipboardRegister()
    private val plusRegister = clipboardRegister

    private val registers = mutableMapOf(
        '"' to SimpleRegister(), // the unnamed register is just a Simple one
        '_' to BlackHoleRegister(),

        '*' to clipboardRegister,
        '+' to plusRegister
    )

    override val unnamed: IRegister
        get() = settings[CLIPBOARD].let { clipboardSetting ->
            when {
                "unnamedplus" in clipboardSetting -> plusRegister
                "unnamed" in clipboardSetting -> clipboardRegister
                else -> super.unnamed
            }
        }

    override var current: IRegister = unnamed

    override fun get(register: Char): IRegister {
        return registers[register] ?: SimpleRegister().let {
            registers[register] = it
            it
        }
    }
}

