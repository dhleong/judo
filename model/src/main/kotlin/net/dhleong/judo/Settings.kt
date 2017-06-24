package net.dhleong.judo

/**
 * @author dhleong
 */

// NOTE: we declare this up here because declareSetting is inline
val ALL_SETTINGS = mutableMapOf<String, Setting<*>>()


/*
 * Settings declarations
 */

val WORD_WRAP = declareSetting("wordwrap", true)



/**
 * @param userName User-facing setting name; used as `set([userName], value)`
 */
class Setting<E : Any>(
    name: String,
    val userName: String,
    val type: Class<E>,
    val default: E
) : StateKind<E>(name) {

    /**
     * Read the value of this setting from the StateMap,
     * returning our default value if not explicitly set
     */
    fun read(state: StateMap): E =
        state[this] ?: default

}

private inline fun <reified E : Any> declareSetting(userName: String, default: E): Setting<E> {
    val setting = Setting("net.dhleong.judo.settings.$userName", userName, E::class.java, default)
    ALL_SETTINGS[userName] = setting
    return setting
}
