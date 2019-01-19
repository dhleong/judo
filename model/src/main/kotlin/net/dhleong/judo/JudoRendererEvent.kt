package net.dhleong.judo

sealed class JudoRendererEvent {
    /**
     * Fired if the [JudoRenderer.windowWidth] or [JudoRenderer.windowHeight]
     *  values changed; run inside a transaction
     */
    object OnResized : JudoRendererEvent()

    object OnBlockingEcho : JudoRendererEvent()
}