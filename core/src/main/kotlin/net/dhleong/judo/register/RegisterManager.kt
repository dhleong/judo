package net.dhleong.judo.register

/**
 * @author dhleong
 */
class RegisterManager : IRegisterManager {

    private val registers = ClipboardRegister().let { clipboardRegister ->
        mutableMapOf(
            '"' to SimpleRegister(), // the unnamed register is just a Simple one
            '_' to BlackHoleRegister(),

            // NOTE: this is not strictly correct; I think * is supposed to be
            // the "selection" register on X11 systems....
            '*' to clipboardRegister,
            '+' to clipboardRegister
        )
    }

    override var current: IRegister = unnamed

    override fun get(register: Char): IRegister {
        return registers[register] ?: SimpleRegister().let {
            registers[register] = it
            it
        }
    }
}

