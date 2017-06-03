package net.dhleong.judo.alias

import java.util.regex.Pattern

/**
 * @author dhleong
 */
class Alias(val original: String, private val pattern: Pattern, private val process: AliasProcesser) {
    companion object {
        fun compile(spec: String, processor: AliasProcesser): Alias {
            // TODO variables
            // TODO do we need whitespace boundaries instead of word boundaries?
            return Alias(spec, Pattern.compile("\\b($spec)\\b"), processor)
        }
    }

    /** @return True if we did anything */
    fun apply(input: StringBuilder): Boolean {
        var matched = false
        val matcher = pattern.matcher(input)

        while (matcher.find()) {
            matched = true

            // TODO variables
            val processed = process(emptyArray())
            input.replace(matcher.start(), matcher.end(), processed)
        }

        return matched
    }
}
