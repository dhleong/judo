package net.dhleong.judo

sealed class JudoRendererEvent {
    /**
     * Fired if the [JudoRenderer] dimensions are unchanged, but
     * the window layout has changed (IE a new window was created,
     * a window was resized, etc.)
     */
    object OnLayout : JudoRendererEvent()

    /**
     * Fired if the [JudoRenderer.windowWidth] or [JudoRenderer.windowHeight]
     *  values changed; run inside a transaction
     */
    object OnResized : JudoRendererEvent()

    object OnBlockingEcho : JudoRendererEvent()
}