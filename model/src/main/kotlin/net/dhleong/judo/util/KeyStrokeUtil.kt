package net.dhleong.judo.util

import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

fun KeyStroke.hasCtrl(): Boolean =
    hasModifiers(KeyEvent.CTRL_DOWN_MASK)

fun KeyStroke.hasShift(): Boolean =
    hasModifiers(KeyEvent.SHIFT_DOWN_MASK)

fun KeyStroke.hasModifiers(mask: Int): Boolean =
    (modifiers and mask) != 0
