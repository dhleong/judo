package net.dhleong.judo.script

import net.dhleong.judo.script.init.initAliases
import net.dhleong.judo.script.init.initColors
import net.dhleong.judo.script.init.initConnection
import net.dhleong.judo.script.init.initConsts
import net.dhleong.judo.script.init.initCore
import net.dhleong.judo.script.init.initEvents
import net.dhleong.judo.script.init.initFiles
import net.dhleong.judo.script.init.initKeymaps
import net.dhleong.judo.script.init.initModes
import net.dhleong.judo.script.init.initMultiTriggers
import net.dhleong.judo.script.init.initPrompts
import net.dhleong.judo.script.init.initTriggers
import net.dhleong.judo.script.init.initUtil
import net.dhleong.judo.script.init.initWindows

/**
 * @author dhleong
 */
fun ScriptInitContext.initObjects() = sequence {
    yieldAll(initConsts())
    yieldAll(initCore())

    yieldAll(initAliases())
    yieldAll(initColors())
    yieldAll(initConnection())
    yieldAll(initEvents())
    yieldAll(initFiles())
    yieldAll(initKeymaps())
    yieldAll(initModes())
    yieldAll(initMultiTriggers())
    yieldAll(initPrompts())
    yieldAll(initTriggers())
    yieldAll(initUtil())
    yieldAll(initWindows())
}