package net.dhleong.judo.render

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.flavor.Flavor

/**
 * @author dhleong
 */
fun Assert<FlavorableCharSequence>.hasFlavor(
    flavor: Flavor,
    atIndex: Int = 0
) = given { actual -> assertThat(actual).hasFlavor(flavor, atIndex, actual.length) }
fun Assert<FlavorableCharSequence>.hasFlavor(
    flavor: Flavor,
    atIndex: Int = 0,
    untilIndex: Int
) = given { actual ->
    for (i in atIndex until untilIndex) {
        val thisFlavor = actual.getFlavor(i)
        if (thisFlavor != flavor) {
            expected(
                "from ${show(atIndex)} until ${show(untilIndex)} ${show(flavor)};" +
                    "\n encountered at ${show(i)}: ${show(thisFlavor)}"
            )
        }
    }
}

fun Assert<FlavorableCharSequence>.doesNotHaveTrailingFlavor() = given { actual ->
    if (actual.trailingFlavor == null) return
    expected("to NOT have trailing flavor, but had ${actual.trailingFlavor}")
}

fun Assert<FlavorableCharSequence>.hasTrailingFlavor(
    flavor: Flavor
) = given { actual ->
    if (actual.trailingFlavor == flavor) return
    expected("trailing flavor to be ${show(flavor)} but was ${show(actual.trailingFlavor)}")
}

fun Assert<FlavorableCharSequence>.splitsAtNewlinesToStrings(
    vararg strings: String,
    continueLines: MutableList<String>? = null
) = given { actual ->
    val split = continueLines?.map {
        FlavorableStringBuilder.withDefaultFlavor(it) as FlavorableCharSequence
    }?.toMutableList() ?: mutableListOf()
    actual.splitAtNewlines(split, continueIncompleteLines = continueLines != null)
    assertThat(split.map { it.toString() }).isEqualTo(strings.toList())
}

