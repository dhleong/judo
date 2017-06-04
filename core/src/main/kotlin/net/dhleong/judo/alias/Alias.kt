package net.dhleong.judo.alias

import java.util.regex.Pattern

internal val VAR_REGEX = Regex("\\$(\\d+|\\{\\w+})")

/**
 * @author dhleong
 */
class Alias(
    val original: String,
    private val pattern: Pattern,
    private val groups: List<String>,
    private val process: AliasProcesser
) {

    companion object {
        fun compile(spec: String, processor: AliasProcesser): Alias {
            val groups = mutableListOf<String>()
            val withVars = VAR_REGEX.replace(spec, { matchResult ->
                val raw = matchResult.groups[1]!!.value
                if (raw[0] == '{') {
                    // TODO support named variables
                    throw IllegalArgumentException("Named variables not yet supported")
                }

                val baseName =
                    if (raw[0] == '{') raw.substring(1..raw.length - 2)
                    else raw
                val name = "VAR$baseName"
                groups.add(name)
                "(?<$name>.+?)"
            })

            // TODO do we need whitespace boundaries instead of word boundaries?
            return Alias(spec, Pattern.compile("\\b($withVars)\\b"), groups, processor)
        }
    }

    /** @return True if we did anything */
    fun apply(input: StringBuilder): Boolean {
        var matched = false
        val matcher = pattern.matcher(input)

        val vars =
            if (groups.isEmpty()) emptyArray<String>()
            else Array(groups.size) { "" }

        while (matcher.find()) {
            matched = true

            // extract variables
            for (i in 0 until groups.size) {
                val value = matcher.group(groups[i])
                vars[i] = value
            }

            val processed = process(vars)
            input.replace(matcher.start(), matcher.end(), processed)
        }

        return matched
    }
}
