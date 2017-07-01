package net.dhleong.judo.alias

typealias AliasProcesser = (args: Array<String>) -> String

interface AliasSpec {
    val groups: Int
    fun matcher(input: CharSequence): AliasMatcher

    val original: String
}

interface AliasMatcher {
    fun find(): Boolean
    fun group(index: Int): String

    val start: Int
    val end: Int
}

interface IAliasManager {
    fun clear()

    fun define(inputSpec: String, outputSpec: String)
    fun define(inputSpec: String, parser: AliasProcesser)

    fun define(inputSpec: AliasSpec, outputSpec: String)
    fun define(inputSpec: AliasSpec, parser: AliasProcesser)

    fun process(input: CharSequence): CharSequence

    fun undefine(inputSpec: String)
}