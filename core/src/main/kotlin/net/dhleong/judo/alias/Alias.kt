package net.dhleong.judo.alias

import net.dhleong.judo.util.IStringBuilder
import net.dhleong.judo.util.stripAnsi
import java.util.regex.Pattern

internal val VAR_REGEX = Regex("\\$(\\d+|\\{\\w+})")

/**
 * @author dhleong
 */
class Alias(
    val original: String,
    private val originalOutput: String?,
    private val pattern: Pattern,
    private val groups: List<String>,
    private val process: AliasProcesser
) {

    companion object {
        fun compile(spec: String, outputSpec: String?, processor: AliasProcesser): Alias {
            val groups = mutableListOf<String>()
            var lastEnd = 0
            if (spec[0] == '^') {
                lastEnd = 1
            }

            val withVars = StringBuilder()
            VAR_REGEX.findAll(spec).forEach { matchResult ->
                val raw = matchResult.groups[1]!!.value
                if (raw[0] == '{') {
                    // TODO support named variables
                    throw IllegalArgumentException("Named variables not yet supported")
                }

                withVars.append(Regex.escape(
                    spec.substring(lastEnd, matchResult.range.start)
                ))
                lastEnd = matchResult.range.endInclusive + 1

                val baseName =
                    if (raw[0] == '{') raw.substring(1..raw.length - 2)
                    else raw
                val name = "VAR$baseName"
                groups.add(name)
                withVars.append("(?<$name>.+?)")
            }

            withVars.append(Regex.escape(
                spec.substring(lastEnd, spec.length)
            ))

            // do we need whitespace boundaries instead of word boundaries?
            val pattern =
                if (spec[0] == '^') Pattern.compile("^$withVars(?=\\b|\\s|$)", Pattern.MULTILINE)
                else Pattern.compile("\\b($withVars)(?=\\b|\\s|$)", Pattern.MULTILINE)

            return Alias(spec, outputSpec, pattern, groups, processor)
        }
    }

    /** @return True if we did anything */
    fun apply(input: IStringBuilder): Boolean {
        // `keepAnsi = true` is a minor hax optimization;
        // we know we don't need to bother with that here
        return parse(input, { it }, keepAnsi = true)
    }

    fun parse(input: IStringBuilder, postProcess: (String) -> String, keepAnsi: Boolean = false): Boolean {
        val matcher = pattern.matcher(input)

        val vars =
            if (groups.isEmpty()) emptyArray<String>()
            else Array(groups.size) { "" }

        if (!matcher.find()) return false

        // extract variables
        for (i in 0 until groups.size) {
            val value = matcher.group(groups[i])
            vars[i] =
                if (keepAnsi) value
                else stripAnsi(value)
        }

        val processed = postProcess(process(vars))
        input.replace(matcher.start(), matcher.end(), processed)

        return true
    }

    fun describeTo(out: Appendable) {
        val aliasTo = originalOutput?.let { it } ?: "<function>"
        out.apply {
            append(original)
            append('\t')
            append(aliasTo)
        }
    }
}
