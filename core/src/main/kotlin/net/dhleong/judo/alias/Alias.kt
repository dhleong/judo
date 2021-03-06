package net.dhleong.judo.alias

import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.util.PatternMatcher
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import java.util.EnumSet
import java.util.regex.Matcher
import java.util.regex.Pattern

internal val VAR_REGEX = Regex("\\$(\\d+|\\{\\w+})")

internal class RegexAliasSpec(
    override val original: String,
    private val pattern: Pattern,
    private val groupNames: List<String>,
    override val flags: EnumSet<PatternProcessingFlags> = PatternProcessingFlags.NONE
) : PatternSpec {

    override fun matcher(input: CharSequence): PatternMatcher =
        RegexAliasMatcher(pattern.matcher(input), groupNames)

    override fun toString(): String = "RegexAliasSpec($original)"
}

internal class RegexAliasMatcher(
    private val matcher: Matcher,
    private val groupNames: List<String>
) : PatternMatcher {
    override val groups: Int
        get() = groupNames.size

    override fun find(): Boolean = matcher.find()

    override fun group(index: Int): String =
        matcher.group(groupNames[index])

    override val start: Int
        get() = matcher.start()
    override fun start(index: Int): Int =
        matcher.start(groupNames[index])

    override val end: Int
        get() = matcher.end()
    override fun end(index: Int): Int =
        matcher.end(groupNames[index])
}

/**
 * Given a string representing a simple input spec (that is,
 *  $1/$2-style variable placeholders and no regex except
 *  for `^`), compile a PatternSpec
 */
fun compileSimplePatternSpec(
    spec: String, flags: EnumSet<PatternProcessingFlags> = PatternProcessingFlags.NONE
): PatternSpec {

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
            spec.substring(lastEnd, matchResult.range.first)
        ))
        lastEnd = matchResult.range.last + 1

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

    return RegexAliasSpec(spec, pattern, groups, flags)
}

/**
 * @author dhleong
 */
class Alias(
    override val original: String,
    private val originalOutput: String?,
    private val spec: PatternSpec,
    private val process: AliasProcesser
) : IAlias {

    companion object {
        fun compile(spec: String, outputSpec: String?, processor: AliasProcesser): Alias {
            val compiledSpec = compileSimplePatternSpec(spec)
            return Alias(spec, outputSpec, compiledSpec, processor)
        }
    }

    private val emptyStringArray = emptyArray<String>()

    /** @return True if we did anything */
    fun apply(input: FlavorableStringBuilder): Boolean {
        return parse(input) { it }
    }

    fun parse(input: FlavorableStringBuilder, postProcess: (String?) -> String?): Boolean {
        val matcher = spec.matcher(input)
        val groups = matcher.groups

        val vars =
            if (groups == 0) emptyStringArray
            else Array(groups) { "" }

        if (!matcher.find()) return false

        // extract variables
        for (i in 0 until groups) {
            vars[i] = when {
                PatternProcessingFlags.KEEP_COLOR in spec.flags -> {
                    input.subSequence(matcher.start(i), matcher.end(i))
                        .toAnsi()
                }

                else -> input.substring(matcher.start(i), matcher.end(i))
            }
        }

        val processed = postProcess(process(vars))
        input.replace(matcher.start, matcher.end, processed ?: "")

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
