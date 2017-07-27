package net.dhleong.judo

/**
 * @author dhleong
 */

// NOTE: we declare this up here because declareSetting is inline
val ALL_SETTINGS = mutableMapOf<String, Setting<*>>()


/*
 * Settings declarations
 */

val CLIPBOARD = declareSetting("clipboard", "",
    "If set to 'unnamed', cuts and pastes will default to the * register")

val WORD_WRAP = declareSetting("wordwrap", true)

val MODE_STACK = declareSetting("modestack", true,
    "If True, exitMode() returns to the previous mode; if False, always returns to Normal mode.")

val MAX_INPUT_LINES = declareSetting("inputlines", 1,
    "Maximum number of lines the input field can expand to before the text gets scrolled.")

/**
 * @param userName User-facing setting name; used as `set([userName], value)`
 */
class Setting<E : Any>(
    name: String,
    val userName: String,
    val type: Class<E>,
    val default: E,
    val description: String
) : StateKind<E>(name) {

    /**
     * Read the value of this setting from the StateMap,
     * returning our default value if not explicitly set
     */
    @Deprecated("Just use state[this] instead", ReplaceWith("state[this]"))
    fun read(state: StateMap): E = state[this]

}

private inline fun <reified E : Any> declareSetting(
    userName: String, default: E, description: String = ""
): Setting<E> {
    val setting = Setting(
        "net.dhleong.judo.settings.$userName", userName, E::class.java, default,
        description
    )
    ALL_SETTINGS[userName] = setting
    return setting
}
