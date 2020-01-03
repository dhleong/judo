package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.registerFrom

/**
 * @author dhleong
 */
fun ScriptInitContext.initFiles() {
    registerFrom(FilesScripting(this))
}
