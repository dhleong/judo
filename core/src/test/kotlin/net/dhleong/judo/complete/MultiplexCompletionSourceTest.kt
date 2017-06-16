package net.dhleong.judo.complete

import net.dhleong.judo.complete.multiplex.WeightedRandomSelector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class MultiplexCompletionSourceTest {
    @Test fun weightedSelector() {
        var nextRandom = 0.0
        val selector = WeightedRandomSelector(
            doubleArrayOf(.60, .40),
            { nextRandom }
        )
        val multiplex = MultiplexCompletionSource(
            listOf(
                DumbCompletionSource().apply {
                    process("She sells seashells")
                },

                DumbCompletionSource().apply {
                    process("By the sea shore")
                }
            ),
            { _, _ -> selector }
        )

        val suggestions = multiplex.suggest("s").iterator()
        nextRandom = .39 // below 40 should go to the 2nd source
        assertThat(suggestions.next())
            .isEqualTo("sea")


        nextRandom = .41 // above should go first
        assertThat(suggestions.next())
            .isEqualTo("seashells")

        nextRandom = .42 // continue
        assertThat(suggestions.next())
            .isEqualTo("sells")


        nextRandom = 0.20 // resume second
        assertThat(suggestions.next())
            .isEqualTo("shore")


        nextRandom = 0.02 // second is empty; go with first
        assertThat(suggestions.next())
            .isEqualTo("she")

        // done!
        assertThat(suggestions.hasNext()).isFalse()
    }

    @Test fun weightedSelectorOnEmpty() {

        var nextRandom = 0.0
        val selector = WeightedRandomSelector(
            doubleArrayOf(.60, .40),
            { nextRandom }
        )
        val multiplex = MultiplexCompletionSource(
            listOf(
                // first has a higher weight, but is empty!
                DumbCompletionSource(),

                DumbCompletionSource().apply {
                    process("By the sea shore")
                }
            ),
            { _, _ -> selector }
        )

        val suggestions = multiplex.suggest("s").iterator()

        nextRandom = 0.5
        assertThat(suggestions.next()).isEqualTo("sea")
        assertThat(suggestions.next()).isEqualTo("shore")
        assertThat(suggestions.hasNext()).isFalse()
    }
}