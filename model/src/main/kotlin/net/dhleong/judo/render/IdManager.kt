package net.dhleong.judo.render

class IdManager {
    private var nextBufferId = 0
    private var nextTabpageId = 0
    private var nextWindowId = 0

    fun newBuffer() = ++nextBufferId
    fun newTabpage() = ++nextTabpageId
    fun newWindow() = ++nextWindowId
}