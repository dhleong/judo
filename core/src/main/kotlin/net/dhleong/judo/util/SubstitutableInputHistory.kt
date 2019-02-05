package net.dhleong.judo.util

import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import java.io.File

/**
 * @author dhleong
 */
class SubstitutableInputHistory(
    private val base: IInputHistory
) : IInputHistory {

    private var current: IInputHistory = base

    override var buffer: InputBuffer
        get() = current.buffer
        set(_) {
            throw UnsupportedOperationException(
                "Do not change SubstituitableInputHistory's buffer!"
            )
        }

    override val size: Int
        get() = current.size

    override fun clear() = current.clear()

    override fun push(line: String) = current.push(line)

    override fun resetHistoryOffset() = current.resetHistoryOffset()

    override fun scroll(dir: Int, clampCursor: Boolean) = current.scroll(dir, clampCursor)

    override fun search(match: String, forceNext: Boolean): Boolean =
        current.search(match, forceNext)

    override fun writeTo(path: File) = current.writeTo(path)

    fun substitute(temporary: IInputHistory): RestorableState {
        val state = RestorableState(
            current,
            temporary.buffer,
            temporary
        )
        temporary.buffer = base.buffer
        current = temporary
        return state
    }

    fun restore(state: RestorableState) {
        current = state.previousHistory
        state.previousBufferOwner.buffer = state.previousBuffer
    }

    class RestorableState(
        internal val previousHistory: IInputHistory,
        internal val previousBuffer: InputBuffer,
        internal val previousBufferOwner: IInputHistory
    )

}