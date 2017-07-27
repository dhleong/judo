package net.dhleong.judo.register

/**
 * @author dhleong
 */
class RegisterManager : IRegisterManager {
    private val registers = mutableMapOf<Char, IRegister>(
        // this is not strictly correct, but good enough for now
        '*' to ClipboardRegister(),
        '+' to ClipboardRegister()
    )

    override var current: IRegister = unnamed

    override fun get(register: Char): IRegister {
        return registers[register] ?: SimpleRegister().let {
            registers[register] = it
            it
        }
    }

}

