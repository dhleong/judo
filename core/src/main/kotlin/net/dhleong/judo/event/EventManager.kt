package net.dhleong.judo.event

import com.google.common.collect.HashMultimap


/**
 * @author dhleong
 */
class EventManager : IEventManager {

    // NOTE: we now support a multiple handlers per event
    private val events = HashMultimap.create<String, EventHandler>()
    private val eventThreadId = Thread.currentThread().id

    override fun clear() {
        events.clear()
    }

    override fun clear(entry: Pair<String, EventHandler>) =
        unregister(entry.first, entry.second)

    override fun has(eventName: String) = events.containsKey(eventName)

    override fun unregister(eventName: String, handler: EventHandler) {
        events.remove(eventName, handler)
    }

    override fun raise(eventName: String, data: Any?) {
        if (Thread.currentThread().id != eventThreadId) {
            throw IllegalStateException(
                "Attempting to raise $eventName on non-event thread ${Thread.currentThread()}")
        }

        events[eventName]?.let { handlers ->
            handlers.forEach { it(data) }
        }
    }

    override fun register(eventName: String, handler: EventHandler) {
        events.put(eventName, handler)
    }

}