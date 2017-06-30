package net.dhleong.judo.event

/**
 * @author dhleong
 */
class EventManager : IEventManager {

    // NOTE: we currently support a single handler per event
    private val events = HashMap<String, (Any) -> Unit>()

    override fun clear() {
        events.clear()
    }

    override fun has(eventName: String) = eventName in events

    override fun unregister(eventName: String, handler: (Any) -> Unit) {
        if (events[eventName] == handler) {
            events.remove(eventName)
        }
    }

    override fun raise(eventName: String, data: Any) {
        events[eventName]?.let { handler ->
            handler(data)
        }
    }

    override fun register(eventName: String, handler: (Any) -> Unit) {
        events[eventName] = handler
    }

}