package net.dhleong.judo.search

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.bufferOf
import net.dhleong.judo.render.IJudoBuffer
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class BufferSearcherTest {

    lateinit var searcher: BufferSearcher
    lateinit var buffer: IJudoBuffer

    @Before fun setUp() {
        searcher = BufferSearcher()
    }

    @Test fun `Forward and backward`() {
        buffer = bufferOf("""
            Take my love
            Take my land
            Take me where
        """.trimIndent())

        incSearch("m", 1).shouldSucceed()
        assertThat(searcher).hasResultAt(2, 5)

        incSearch("m", 1).shouldSucceed()
        assertThat(searcher).hasResultAt(1, 5)

        incSearch("m", 1).shouldSucceed()
        assertThat(searcher).hasResultAt(0, 5)

        // no more
        incSearch("m", 1).shouldFail()
        assertThat(searcher).hasResultAt(0, 5) // don't drop the result

        // now, go back...
        incSearch("m", -1).shouldSucceed()
        assertThat(searcher).hasResultAt(1, 5)

        incSearch("m", -1).shouldSucceed()
        assertThat(searcher).hasResultAt(2, 5)

        // no more
        incSearch("m", -1).shouldFail()
        assertThat(searcher).hasResultAt(2, 5)
    }

    @Suppress("SameParameterValue")
    private fun incSearch(keyword: CharSequence, direction: Int): Boolean {
        val scrollback = when (searcher.hasResult) {
            true -> buffer.lastIndex - searcher.resultLine
            else -> 0
        }
        return searcher.searchForKeyword(
            buffer,
            scrollback,
            keyword,
            direction,
            ignoreCase = false
        )
    }
}

private fun Boolean.shouldSucceed() = shouldBe(true)
private fun Boolean.shouldFail() = shouldBe(false)
private fun Boolean.shouldBe(expected: Boolean) {
    assertThat(this, "search return value").isEqualTo(expected)
}

private fun Assert<BufferSearcher>.hasNoResult() = given { actual ->
    if (!actual.hasResult) return
    expected("to NOT have a result, but had one at ${show(actual.resultLine to actual.resultOffset)}")
}

private fun Assert<BufferSearcher>.hasResultAt(line: Int, offset: Int) = given { actual ->
    if (actual.hasResult && actual.resultLine == line && actual.resultOffset == offset) return
    val result = when {
        !actual.hasResult -> "had no result"
        else -> "was ${show(actual.resultLine to actual.resultOffset)}"
    }
    expected("to have a result at ${show(line to offset)} but $result")
}
