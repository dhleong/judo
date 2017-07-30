package net.dhleong.judo

import net.dhleong.judo.logging.AnsiStylist
import net.dhleong.judo.logging.AnsiStylistAssert
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IJudoWindowAssert
import org.assertj.core.api.Assertions.assertThat

/**
 * @author dhleong
 */

internal fun assertThat(actual: AnsiStylist) = AnsiStylistAssert(actual)

fun assertThat(actual: IJudoWindow?) = IJudoWindowAssert(actual)

// imports for convenience:
fun <T> assertThat(actual: List<T>) = assertThat(actual)!!
fun <T> assertThat(actual: Array<T>) = assertThat(actual)!!
fun assertThat(actual: Boolean?) = assertThat(actual)!!
fun assertThat(actual: Byte?) = assertThat(actual)!!
fun assertThat(actual: CharSequence?) = assertThat(actual)!!
fun assertThat(actual: Int?) = assertThat(actual)!!
fun assertThat(actual: String?) = assertThat(actual)!!
