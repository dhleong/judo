package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerConst

/**
 * @author dhleong
 */
fun ScriptInitContext.initConsts() = with(engine) {
    registerConst(
        "MYJUDORC",
        doc {
            body {
                "Path to the Judo config file"
            }
        },
        userConfigFile.absolutePath
    )

    registerConst(
        "judo",
        doc {
            body { "Reference to the core Judo scripting object" }
        },
        wrapCore(judo)
    )
}
