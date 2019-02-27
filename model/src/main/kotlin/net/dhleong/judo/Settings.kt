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

val WORD_WRAP = declareSetting("wordwrap", true,
    "If false, output will go up to the edge of the window and words will be split.")

val MODE_STACK = declareSetting("modestack", true,
    "If True, exitMode() returns to the previous mode; if False, always returns to Normal mode.")

val MAX_INPUT_LINES = declareSetting("inputlines", 1,
    "Maximum number of lines the input field can expand to before the text gets scrolled.")

val SCROLL = declareSetting("scroll", 0, """
    Number of lines to scroll with CTRL-U and CTRL-D commands. When less
    than or equal to 0, will use half the window's height.
""".trimIndent())

// map settings
val MAP_AUTORENDER = declareSetting("map:autorender", false,
    "Automatically render maps on move")
val MAP_AUTOROOM = declareSetting("map:autoroom", false,
    "Auto create rooms in a map when moving in a direction.")
val MAP_AUTOMAGIC = declareSetting("map:automagic", false,
    "If enabled, attempt to automagically create maps using MSDP. MUST be set BEFORE creating or loading a map.")

// debug flags
val DEBUG_AUTOMAGIC = declareSetting("debug:automagic", false)

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
