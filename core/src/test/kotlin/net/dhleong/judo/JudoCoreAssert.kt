package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show

/**
 * @author dhleong
 */
fun Assert<JudoCore>.isInMode(modeName: String) = given { actual ->
    val actualMode = actual.currentMode.name
    if (actualMode == modeName) return
    expected("to be in mode ${show(modeName)} but was ${show(actualMode)}")
}
