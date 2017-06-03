package net.dhleong.judo.alias

class AliasManager : IAliasManager {

    val aliases = mutableListOf<Alias>()

    override fun clear() =
        aliases.clear()

    override fun define(inputSpec: String, outputSpec: String) {
        define(inputSpec, { args ->
            // TODO replace variables
            outputSpec
        })
    }

    override fun define(inputSpec: String, parser: AliasProcesser) {
        aliases.add(Alias.compile(inputSpec, parser))
    }

    private val MAX_ITERATIONS = 50

    override fun process(input: String): String {
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

        return builder.toString()
    }

    fun hasAliasFor(inputSpec: String): Boolean =
        aliases.any { it.original == inputSpec }
}

class AliasProcessingException(reason: String, original: String)
    : IllegalStateException("$reason; input=`$original`")

