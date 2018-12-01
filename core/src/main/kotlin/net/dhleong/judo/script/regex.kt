package net.dhleong.judo.script

import net.dhleong.judo.util.PatternMatcher
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import java.util.EnumSet
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Java Regex-based PatternSpec, for use with Java Regex-compatible
 * scripting engines
 * @author dhleong
 */
class JavaRegexPatternSpec(
    override val original: String,
    private val pattern: Pattern,
    override val flags: EnumSet<PatternProcessingFlags>
) : PatternSpec {
    override fun matcher(input: CharSequence): PatternMatcher =
        JavaRegexAliasMatcher(pattern.matcher(input))
}

internal class JavaRegexAliasMatcher(
    private val matcher: Matcher
) : PatternMatcher {
    override val groups: Int
        get() = matcher.groupCount()

    override fun find(): Boolean = matcher.find()

    override fun group(index: Int): String =
    // NOTE group 0 is the entire pattern
        matcher.group(index + 1)

    override val start: Int
        get() = matcher.start()
    override fun start(index: Int): Int =
        matcher.start(index + 1)

    override val end: Int
        get() = matcher.end()
    override fun end(index: Int): Int =
        matcher.end(index + 1)
}

