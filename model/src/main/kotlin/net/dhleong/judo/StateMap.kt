package net.dhleong.judo

@Suppress("unused")
open class StateKind<E>(val name: String) {
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean =
        other is StateKind<*> && other.name == name
}

interface IStateMap {
    interface Listener<E : Any> {
        fun onChanged(key: StateKind<E>, newValue: E)
    }

    fun <E : Any> addListener(key: StateKind<E>, listener: Listener<E>)
    fun <E : Any> removeListener(key: StateKind<E>, listener: Listener<E>)

    operator fun <E : Any> set(key: StateKind<E>, value: E)

    operator fun <E : Any> get(key: StateKind<E>): E?
    operator fun <E : Any> get(key: Setting<E>): E

    operator fun <E : Any> contains(key: StateKind<E>): Boolean

    fun <E : Any> remove(key: StateKind<E>): E?
}

object EmptyStateMap : IStateMap {
    override fun <E : Any> addListener(key: StateKind<E>, listener: IStateMap.Listener<E>) {
        // nop
    }

    override fun <E : Any> removeListener(key: StateKind<E>, listener: IStateMap.Listener<E>) {
        // nop
    }

    override fun <E : Any> set(key: StateKind<E>, value: E) {}

    override fun <E : Any> get(key: StateKind<E>): E? = null
    override fun <E : Any> get(key: Setting<E>): E = TODO("not implemented")

    override fun <E : Any> contains(key: StateKind<E>): Boolean = false
    override fun <E : Any> remove(key: StateKind<E>): E? = null
}

/**
 * StateMap provides typesafe storage for whatever intermediate
 * state a motion or mode, etc. might need for communicating
 * with other motions or modes. The ;/, motions, for example,
 * need to know how to repeat the last f/F/t/T, and store that
 * in the StateMap.
 *
 * The StateMap is also a great place to store global settings
 */
@Suppress("UNCHECKED_CAST")
class StateMap() : IStateMap {
    private val map = mutableMapOf<StateKind<*>, Any>()
    private val listeners = mutableMapOf<StateKind<*>, MutableSet<IStateMap.Listener<*>>>()

    constructor(vararg pairs: Pair<StateKind<*>, Any>): this() {
        map.putAll(pairs)
    }

    override fun <E : Any> addListener(key: StateKind<E>, listener: IStateMap.Listener<E>) {
        val set = listeners[key] ?: mutableSetOf<IStateMap.Listener<*>>().also {
            listeners[key] = it
        }

        set += listener
    }

    override fun <E : Any> removeListener(key: StateKind<E>, listener: IStateMap.Listener<E>) {
        val set = listeners[key] ?: return
        set -= listener
    }

    override operator fun <E : Any> set(key: StateKind<E>, value: E) {
        map[key] = value

        val set = listeners[key] ?: return
        for (l in set) {
            (l as IStateMap.Listener<E>).onChanged(key, value)
        }
    }

    override operator fun <E : Any> get(key: StateKind<E>): E? =
        map[key] as E?

    override operator fun <E : Any> get(key: Setting<E>): E =
        map[key] as? E ?: key.default

    override operator fun <E : Any> contains(key: StateKind<E>): Boolean =
        key in map

    override fun <E : Any> remove(key: StateKind<E>): E? =
        map.remove(key) as E?
}

/**
 * [DelegateStateMap] writes to a separate local [IStateMap]
 * that overrides the [base], and reads from either (but does
 * not write to [base]).
 */
class DelegateStateMap(
    private val base: IStateMap,
    private val localState: IStateMap = StateMap()
) : IStateMap by localState {

    override operator fun <E : Any> get(key: StateKind<E>): E? =
        localState[key] ?: base[key]

    // NOTE: settings all come from the base
    override fun <E : Any> get(key: Setting<E>): E = base[key]

    override operator fun <E : Any> contains(key: StateKind<E>): Boolean =
        key in localState || key in base

}