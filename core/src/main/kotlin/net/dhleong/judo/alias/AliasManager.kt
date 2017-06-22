package net.dhleong.judo.alias

import net.dhleong.judo.util.IStringBuilder

class AliasManager : IAliasManager {

    private val MAX_ITERATIONS = 50

    internal val aliases = mutableListOf<Alias>()

    override fun clear() =
        aliases.clear()

    override fun define(inputSpec: String, outputSpec: String) {
        define(inputSpec, outputSpec, VariableOutputProcessor(outputSpec)::process)
    }

    override fun define(inputSpec: String, parser: AliasProcesser) {
        define(inputSpec, null, parser)
    }

    private fun define(inputSpec: String, outputSpec: String?, parser: AliasProcesser) {
        aliases.add(Alias.compile(inputSpec, outputSpec, parser))
    }

    override fun process(input: CharSequence): CharSequence {
        val builder = IStringBuilder.from(input)

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

        return builder
    }

    override fun undefine(inputSpec: String) {
        aliases.removeIf { it.original == inputSpec }
    }

    /**
     * Non-recursive processing that supports a postProcess step
     */
    fun process(input: CharSequence, postProcess: (Int, String) -> String): CharSequence {
        val builder = IStringBuilder.from(input)
        aliases.forEachIndexed { index, alias ->
            alias.parse(builder, { postProcess(index, it) })
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

    val processors = mutableListOf<AliasProcesser>()

    init {
        var lastEnd = 0
        VAR_REGEX.findAll(outputSpec)
            .forEach { match ->
                val before = outputSpec.substring(lastEnd until match.range.start)
                lastEnd = match.range.endInclusive + 1

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

