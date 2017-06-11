package net.dhleong.judo.alias

typealias AliasProcesser = (args: Array<String>) -> String

interface IAliasManager {
    fun clear()

    fun define(inputSpec: String, outputSpec: String)

    fun define(inputSpec: String, parser: AliasProcesser)

    fun process(input: CharSequence): CharSequence

    fun undefine(inputSpec: String)
}