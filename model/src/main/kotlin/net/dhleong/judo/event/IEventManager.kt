package net.dhleong.judo.event

/**
 * @author dhleong
 */
interface IEventManager {
    fun clear()
    fun has(eventName: String): Boolean
    fun raise(eventName: String, data: Any)
    fun register(eventName: String, handler: (Any) -> Unit)
    fun unregister(eventName: String, handler: (Any) -> Unit)
}