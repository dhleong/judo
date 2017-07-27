package net.dhleong.judo.register

/**
 * @author dhleong
 */
interface IRegisterManager {

    /**
     * Convenient shortcut to the unnamed register;
     *  in the future we may add a setting that would overwrite this
     *  (so unnamed register writes to/reads from the clipboard or whatever)
     */
    val unnamed: IRegister
        get() = this['"']

    var current: IRegister

    operator fun get(register: Char): IRegister
}

interface IRegister {
    var value: CharSequence
    fun copyFrom(sequence: CharSequence, start: Int, end: Int) {
        value = sequence.subSequence(start, end)
    }
}
