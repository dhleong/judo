package net.dhleong.judo.jline

import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer

/**
 * @author dhleong
 */
class JLineBuffer(
    renderer: JLineRenderer,
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : JudoBuffer(
    ids,
    renderer.settings,
    scrollbackSize
)