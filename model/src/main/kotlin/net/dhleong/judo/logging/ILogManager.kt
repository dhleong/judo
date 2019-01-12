package net.dhleong.judo.logging

import net.dhleong.judo.render.FlavorableCharSequence
import java.io.File

/**
 * @author dhleong
 */
interface ILogManager {
    enum class Format {
        RAW,
        PLAIN,
        HTML
    }

    enum class Mode {
        APPEND,
        REPLACE
    }

    /**
     * Start logging with the given settings to the given File
     */
    fun configure(destination: File, format: Format, mode: Mode)

    /**
     * Disable logging
     */
    fun unconfigure()

    fun log(line: FlavorableCharSequence)
}