package net.dhleong.judo.motions

import net.dhleong.judo.input.keys

/**
 * @author dhleong
 */

val ALL_MOTIONS = arrayListOf(
    // TODO counts?
    keys("b") to wordMotion(-1, false),
    keys("B") to wordMotion(-1, true),
    keys("w") to wordMotion(1, false),
    keys("W") to wordMotion(1, true),

    keys("e") to endOfWordMotion(1, false),
    keys("E") to endOfWordMotion(1, true),
    keys("ge") to endOfWordMotion(-1, false),
    keys("gE") to endOfWordMotion(-1, true),

    keys("f") to findMotion(1),
    keys("F") to findMotion(-1),

    keys("h") to charMotion(-1),
    keys("l") to charMotion(1),

    keys("iw") to innerWordObjectMotion(false),
    keys("iW") to innerWordObjectMotion(true),

    keys("t") to tilMotion(1),
    keys("T") to tilMotion(-1),

    keys("0") to toStartMotion(),
    keys("$") to toEndMotion()
)
