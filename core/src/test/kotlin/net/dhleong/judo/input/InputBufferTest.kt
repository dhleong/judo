package net.dhleong.judo.input

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.dhleong.judo.StateMap
import net.dhleong.judo.register.RegisterManager
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class InputBufferTest {

    private val registers = RegisterManager(StateMap())
    val buffer = InputBuffer(registers)

    @Before fun setUp() {
        buffer.clear()
    }

    @Test fun simpleType() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type(key("'"))
        buffer.type(key("\""))
        buffer.type(key("t"))

        assertThat(buffer.toString()).isEqualTo("'\"t")
        assertThat(buffer.cursor).isEqualTo(3)
    }

    @Test fun backspace() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds")

        assertThat(buffer.toString()).isEqualTo("malcolm reynolds")
        assertThat(buffer.cursor).isEqualTo(16)

        buffer.type(Key.BACKSPACE)

        assertThat(buffer.toString()).isEqualTo("malcolm reynold")
        assertThat(buffer.cursor).isEqualTo(15)
    }

    @Test fun backspace_middle() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds")
        buffer.cursor = 8
        buffer.type(Key.BACKSPACE)

        assertThat(buffer.toString()).isEqualTo("malcolmreynolds")
        assertThat(buffer.cursor).isEqualTo(7)
    }

    @Test fun backspace_empty() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type(Key.BACKSPACE)

        assertThat(buffer.toString()).isEmpty()
        assertThat(buffer.cursor).isEqualTo(0)
    }

    @Test fun deleteWithCursorForward() {
        buffer.set("0123 5678 101112")
        buffer.cursor = 5

        buffer.deleteWithCursor(5..10)
        assertThat(buffer.toString())
            .isEqualTo("0123 101112")
        assertThat(buffer.cursor).isEqualTo(5)
    }

    @Test fun deleteIntoRegister() {
        buffer.set("0123 5678 101112")
        buffer.cursor = 5

        buffer.deleteWithCursor(5..10)
        assertThat(registers.unnamed.value.toString())
            .isEqualTo("5678 ")
    }

    @Test fun undoDeletes() {
        buffer.set("0123 5678 101112")
        buffer.cursor = 0

        buffer.deleteWithCursor(0..5)
        buffer.deleteWithCursor(5..7)
        assertThat(buffer.undoMan.size).isEqualTo(2)
        assertThat(buffer.toString()).isEqualTo("5678 1112")
        assertThat(buffer.cursor).isEqualTo(5)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("5678 101112")
        assertThat(buffer.cursor).isEqualTo(5)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("0123 5678 101112")
        assertThat(buffer.cursor).isEqualTo(0)

        // just ensure further undo doesn't throw
        assertThat(buffer.undoMan.size).isEqualTo(0)
        buffer.undoMan.undo(buffer)
    }

    @Test fun undoReplacements() {
        buffer.set("Abcd eFgH JkLMno")
        buffer.cursor = 0

        buffer.replaceWithCursor(0..5) { it.toString().toUpperCase() }
        buffer.switchCaseWithCursor(5..9)
        assertThat(buffer.undoMan.size).isEqualTo(2)
        assertThat(buffer.toString()).isEqualTo("ABCD EfGh JkLMno")
        assertThat(buffer.cursor).isEqualTo(5)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("ABCD eFgH JkLMno")
        assertThat(buffer.cursor).isEqualTo(5)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("Abcd eFgH JkLMno")
        assertThat(buffer.cursor).isEqualTo(0)

        // just ensure further undo doesn't throw
        assertThat(buffer.undoMan.size).isEqualTo(0)
        buffer.undoMan.undo(buffer)
    }

    @Test fun undoInsertion() {
        buffer.set("0123 5678 101112")

        // NOTE: moving the cursor should create a new change set,
        // but otherwise everything in insert mode is within a single
        // change set
        buffer.inChangeSet {
            buffer.cursor = 0
            buffer.type(key("a"))
            buffer.type(key("b"))
        }

        buffer.inChangeSet {
            buffer.cursor = 7
            buffer.type(key("c"))
            buffer.type(key("d"))
            buffer.type(key("e"))
        }

        buffer.inChangeSet {
            buffer.cursor = 15
            buffer.type(key("f"))
        }

        assertThat(buffer.toString()).isEqualTo("ab0123 cde5678 f101112")
        assertThat(buffer.undoMan.size).isEqualTo(3)
        assertThat(buffer.cursor).isEqualTo(16)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("ab0123 cde5678 101112")
        assertThat(buffer.cursor).isEqualTo(15)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("ab0123 5678 101112")
        assertThat(buffer.cursor).isEqualTo(7)

        buffer.undoMan.undo(buffer)
        assertThat(buffer.toString()).isEqualTo("0123 5678 101112")
        assertThat(buffer.cursor).isEqualTo(0)
    }
}

