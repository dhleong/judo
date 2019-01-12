package net.dhleong.judo

import org.assertj.core.api.Assertions.assertThat

/**
 * @author dhleong
 */

// imports for convenience:
fun <T> assertThat(actual: List<T>) = assertThat(actual)!!
fun <T> assertThat(actual: Array<T>) = assertThat(actual)!!
fun assertThat(actual: Boolean?) = assertThat(actual)!!
fun assertThat(actual: Byte?) = assertThat(actual)!!
fun assertThat(actual: CharSequence?) = assertThat(actual)!!
fun assertThat(actual: Int?) = assertThat(actual)!!
fun assertThat(actual: String?) = assertThat(actual)!!
