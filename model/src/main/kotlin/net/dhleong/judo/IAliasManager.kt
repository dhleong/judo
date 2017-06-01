package net.dhleong.judo

typealias AliasParser = (args: Array<String>) -> String

interface IAliasManager {
    fun define(inputSpec: String, outputSpec: String)

    fun define(inputSpec: String, parser: AliasParser)

    fun process(input: String): String
}