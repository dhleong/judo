package net.dhleong.judo.alias

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

typealias AliasProcesser = (args: Array<String>) -> String

interface IAliasManager : Clearable<String> {

    fun define(inputSpec: String, outputSpec: String)
    fun define(inputSpec: String, parser: AliasProcesser)

    fun define(inputSpec: PatternSpec, outputSpec: String)
    fun define(inputSpec: PatternSpec, parser: AliasProcesser)

    fun process(input: FlavorableCharSequence): FlavorableCharSequence

    fun undefine(inputSpec: String)
}