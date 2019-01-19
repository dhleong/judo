package net.dhleong.judo.script

import net.dhleong.judo.mapping.IMapManager

/**
 * Subset of [net.dhleong.judo.IJudoCore] that is exposed to scripting
 *
 * @author dhleong
 */
interface IScriptJudo {

    val mapper: IMapManager
    val current: ICurrentJudoObjects

}

interface ICurrentJudoObjects {

    /** *Should* implement [net.dhleong.judo.script.IScriptTabpage] */
    var tabpage: Any

    /** *Should* implement [net.dhleong.judo.script.IScriptWindow] */
    var window: Any

    /** *Should* implement [net.dhleong.judo.script.IScriptBuffer] */
    var buffer: Any

}