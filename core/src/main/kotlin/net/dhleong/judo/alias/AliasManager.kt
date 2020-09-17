package net.dhleong.judo.alias

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.util.PatternSpec
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val MAX_ITERATIONS = 50
private const val MAX_RECURSION = 10

class AliasManager : IAliasManager {

    internal val aliases = mutableListOf<Alias>()

    private val recursionDepth = AtomicInteger(0)

    override fun clear() =
        aliases.clear()

    override fun define(inputSpec: String, outputSpec: String) =
        define(inputSpec, outputSpec, VariableOutputProcessor(outputSpec)::process)

    override fun define(inputSpec: String, parser: AliasProcesser) =
        define(inputSpec, null, parser)

    private fun define(inputSpec: String, outputSpec: String?, parser: AliasProcesser) =
        define(inputSpec, Alias.compile(inputSpec, outputSpec, parser))


    override fun define(inputSpec: PatternSpec, outputSpec: String) =
        define(inputSpec, outputSpec, VariableOutputProcessor(outputSpec)::process)

    override fun define(inputSpec: PatternSpec, parser: AliasProcesser) =
        define(inputSpec, null, parser)

    private fun define(inputSpec: PatternSpec, outputSpec: String?, parser: AliasProcesser) =
        define(inputSpec.original, Alias(inputSpec.original, outputSpec, inputSpec, parser))

    /** Shared implementation */
    private fun define(inputSpec: String, alias: Alias): IAlias {
        // de-dup
        aliases.removeIf { it.original == inputSpec }

        aliases.add(alias)
        return alias
    }

    override fun process(input: FlavorableCharSequence): FlavorableCharSequence {
        val builder = FlavorableStringBuilder(input)

        if (recursionDepth.getAndIncrement() > MAX_RECURSION) {
            recursionDepth.set(0)
            throw AliasProcessingException("Excessive recursion detected", input)
        }

        // keep looping as long as *some* alias was applied,
        //  in case there was a recursive alias
        var iteration = 0
        do {
            val appliedAny = aliases.any { it.apply(builder) }
            ++iteration
        } while (appliedAny && iteration < MAX_ITERATIONS)

        if (iteration >= MAX_ITERATIONS) {
            throw AliasProcessingException("Infinite recursion detected", input)
        }

        recursionDepth.decrementAndGet()

        return builder
    }

    /**
     * NOTE: in most cases, aliases will be processing user input, which shouldn't
     * ever need to be flavored. So, we provide this convenience to handle that common case
     */
    fun process(string: String) = process(FlavorableStringBuilder.fromString(string))

    override fun undefine(inputSpec: String) {
        aliases.removeIf { it.original == inputSpec }
    }

    override fun clear(entry: String) = undefine(entry)

    /**
     * Non-recursive processing that supports a postProcess step
     */
    fun process(
        input: FlavorableCharSequence,
        postProcess: (IAlias, String?) -> String?
    ): FlavorableCharSequence {
        val builder = FlavorableStringBuilder(input)
        aliases.forEach { alias ->
            alias.parse(builder) { postProcess(alias, it) }
        }
        return builder
    }

    fun hasAliasFor(inputSpec: String): Boolean =
        aliases.any { it.original == inputSpec }

    fun describeContentsTo(out: Appendable) =
        aliases.forEach {
            it.describeTo(out)
            out.appendln()
        }

    override fun toString(): String =
        StringBuilder(1024).apply {
            appendln("Aliases:")
            appendln("========")
            describeContentsTo(this)
        }.toString()
}

class VariableOutputProcessor(outputSpec: String) {

    private val processors = mutableListOf<AliasProcesser>()

    init {
        var lastEnd = 0
        VAR_REGEX.findAll(outputSpec)
            .forEach { match ->
                val before = outputSpec.substring(lastEnd until match.range.first)
                lastEnd = match.range.last + 1

                processors.add { before }

                // 1-indexed
                val variable = match.groupValues[1].toInt() - 1
                if (variable < 0) {
                    throw IllegalArgumentException("Alias variables are 1-indexed")
                }
                processors.add { args -> args[variable] }
            }

        if (lastEnd < outputSpec.length) {
            val end = outputSpec.substring(lastEnd)
            processors.add { end }
        }
    }

    fun process(vars: Array<String>): String {
        val builder = StringBuilder()
        processors.forEach { builder.append(it(vars)) }
        return builder.toString()
    }
}

class AliasProcessingException(reason: String, original: CharSequence)
    : IllegalStateException("$reason; input=`$original`")

