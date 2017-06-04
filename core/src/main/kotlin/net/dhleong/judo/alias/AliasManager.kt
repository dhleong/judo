package net.dhleong.judo.alias

class AliasManager : IAliasManager {

    val aliases = mutableListOf<Alias>()

    override fun clear() =
        aliases.clear()

    override fun define(inputSpec: String, outputSpec: String) {
        define(inputSpec, VariableOutputProcessor(outputSpec)::process)
    }

    override fun define(inputSpec: String, parser: AliasProcesser) {
        aliases.add(Alias.compile(inputSpec, parser))
    }

    private val MAX_ITERATIONS = 50

    override fun process(input: CharSequence): CharSequence {
        val builder = StringBuilder(input)

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

    fun hasAliasFor(inputSpec: String): Boolean =
        aliases.any { it.original == inputSpec }
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

