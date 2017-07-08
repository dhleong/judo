package net.dhleong.judo.event

/**
 * @author dhleong
 */
class EventManager : IEventManager {

    // NOTE: we currently support a single handler per event
    private val events = HashMap<String, EventHandler>()
    private val eventThreadId = Thread.currentThread().id

    override fun clear() {
        events.clear()
    }

    override fun clear(entry: Pair<String, EventHandler>) =
        unregister(entry.first, entry.second)

    override fun has(eventName: String) = eventName in events

    override fun unregister(eventName: String, handler: EventHandler) {
        if (events[eventName] == handler) {
            events.remove(eventName)
        }
    }

    override fun raise(eventName: String, data: Any?) {
        if (Thread.currentThread().id != eventThreadId) {
            throw IllegalStateException(
                "Attempting to raise $eventName on non-event thread ${Thread.currentThread()}")
        }

        events[eventName]?.let { handler ->
            handler(data)
        }
    }

    override fun register(eventName: String, handler: EventHandler) {
        events[eventName] = handler
    }

}