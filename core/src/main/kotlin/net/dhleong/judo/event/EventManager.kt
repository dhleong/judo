package net.dhleong.judo.event

import com.google.common.collect.HashMultimap
import net.dhleong.judo.util.JudoMainDispatcher
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext


/**
 * @author dhleong
 */
class EventManager : IEventManager {

    // NOTE: we now support a multiple handlers per event
    private val events = HashMultimap.create<String, EventHandler>()
    private val eventThreadId = Thread.currentThread().id

    private val raiseEventsWorkspace = ArrayList<EventHandler>()

    override fun clear() {
        events.clear()
    }

    override fun clear(entry: Pair<String, EventHandler>) =
        unregister(entry.first, entry.second)

    override fun has(eventName: String) = events.containsKey(eventName)

    override fun unregister(eventName: String, handler: EventHandler) {
        events.remove(eventName, handler)
    }

    override suspend fun raise(eventName: String, data: Any?) {
        if (
            coroutineContext[ContinuationInterceptor] !is JudoMainDispatcher
            && Thread.currentThread().id != eventThreadId
        ) {
            throw IllegalStateException(
                "Attempting to raise $eventName on non-event thread ${Thread.currentThread()}")
        }

        events[eventName]?.let { handlers ->
            // NOTE since we enforce use from a single thread, we can safely
            // reuse the same collection for all raise() calls. We copy to this
            // collection so handlers can register/deregister event handlers without
            // triggering a concurrent modification exception
            raiseEventsWorkspace.let { workspace ->
                workspace.addAll(handlers)
                workspace.forEach { it(data) }
                workspace.clear()
            }
        }
    }

    override fun register(eventName: String, handler: EventHandler) {
        events.put(eventName, handler)
    }

}