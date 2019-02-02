package net.dhleong.judo.alias

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

typealias AliasProcesser = (args: Array<String>) -> String?

interface IAlias {
    /**
     * The original, raw spec
     */
    val original: String
}

interface IAliasManager : Clearable<String> {

    fun define(inputSpec: String, outputSpec: String): IAlias
    fun define(inputSpec: String, parser: AliasProcesser): IAlias

    fun define(inputSpec: PatternSpec, outputSpec: String): IAlias
    fun define(inputSpec: PatternSpec, parser: AliasProcesser): IAlias

    fun process(input: FlavorableCharSequence): FlavorableCharSequence

    fun undefine(inputSpec: String)
}