package net.dhleong.judo.event

typealias EventHandler = (Any?) -> Unit

/**
 * @author dhleong
 */
interface IEventManager {
    fun clear()
    fun has(eventName: String): Boolean
    fun raise(eventName: String, data: Any? = null)
    fun register(eventName: String, handler: EventHandler)
    fun unregister(eventName: String, handler: EventHandler)
}