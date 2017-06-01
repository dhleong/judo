package net.dhleong.judo.modes

import net.dhleong.judo.Mode
import net.dhleong.judo.input.KeyMapping

/**
 * @author dhleong
 */

interface MappableMode : Mode {
    val userMappings: KeyMapping
}
