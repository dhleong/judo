package net.dhleong.judo.render

/**
 * @author dhleong
 */
enum class EchoType {
    /**
     * The Renderer will handle dismissing the Echo
     */
    TRANSIENT,

    /**
     * The echo is "blocking" and needs to be dismissed
     * by typing something. Typing `<cr>` should not be
     * remapped in this mode
     */
    BLOCKING
}