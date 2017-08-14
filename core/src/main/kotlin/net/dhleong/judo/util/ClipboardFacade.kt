package net.dhleong.judo.util

import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/**
 * [ClipboardFacade] represents cross-platform
 *  Clipboard access
 *
 * @author dhleong
 */
interface ClipboardFacade {
    companion object {
        // NOTE: we could support injecting a custom
        // implementation for testing environments here

        fun newInstance(): ClipboardFacade {
            val os = System.getProperty("os.name", "generic").toLowerCase()
            if ("mac" in os || "darwin" in os) {
                // macos
                return MacosClipboard()
            }

            if (!GraphicsEnvironment.isHeadless()) {
                return ToolkitClipboard()
            }

            // headless environments don't support the Toolkit
            return FakeClipboard()
        }
    }

    fun read(): String
    fun write(value: String)
}

/**
 * There's a bug with Mac JDK (and older JRE versions)
 *  where use of AWT classes forces the system to use the
 *  high-perf graphics card. We don't want that, so on
 *  mac we use the `pbcopy` and `pbpaste` cli programs
 */
internal class MacosClipboard : ClipboardFacade {

    private val runtime = Runtime.getRuntime()

    override fun read(): String {
        val proc = runtime.exec("pbpaste")
        proc.waitFor()
        return proc.inputStream.reader().use {
            it.readText()
        }
    }

    override fun write(value: String) {
        val proc = runtime.exec("pbcopy")
        proc.outputStream.bufferedWriter().use {
            it.write(value)
        }
        proc.waitFor()
    }
}

/**
 * ToolkitClipboard is a generic clipboard when we
 *  have access to java.awt.Toolkit
 */
internal class ToolkitClipboard : ClipboardFacade {

    private val clipboard: Clipboard by lazy { Toolkit.getDefaultToolkit().systemClipboard }

    override fun read(): String = try {
        clipboard.getData(DataFlavor.stringFlavor).toString()
    } catch (e: Exception) {
        when (e) {
            // these three might be thrown from getData;
            // they all mean we couldn't get appropriate
            // clipboard data (IE: text); so just return ""
            is UnsupportedFlavorException,
            is IllegalStateException,
            is IOException -> ""

            else -> throw e
        }
    }

    override fun write(value: String) {
        val selection = StringSelection(value)
        clipboard.setContents(selection, selection)
    }
}

/**
 * The FakeClipboard is used for environments that don't
 *  have access to a real clipboard; it is a black hole.
 */
internal class FakeClipboard : ClipboardFacade {
    override fun read(): String {
        return ""
    }

    override fun write(value: String) {
        // should we store the value in memory?
    }
}

