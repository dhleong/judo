package net.dhleong.judo.event

import net.dhleong.judo.util.Clearable

typealias EventHandler = (Any?) -> Unit

/**
 * @author dhleong
 */
interface IEventManager : Clearable<Pair<String, EventHandler>> {
    fun has(eventName: String): Boolean
    fun raise(eventName: String, data: Any? = null)
    fun register(eventName: String, handler: EventHandler)
    fun unregister(eventName: String, handler: EventHandler)
}