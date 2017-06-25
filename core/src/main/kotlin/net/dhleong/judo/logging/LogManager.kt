package net.dhleong.judo.logging

import java.io.File

/**
 * @author dhleong
 */
class LogManager : ILogManager {
    override fun configure(destination: File, format: ILogManager.Format, mode: ILogManager.Mode) {
        TODO("not implemented")
    }

    override fun unconfigure() {
        TODO("not implemented")
    }

    override fun log(rawOutput: CharSequence) {
        TODO("not implemented")
    }

}