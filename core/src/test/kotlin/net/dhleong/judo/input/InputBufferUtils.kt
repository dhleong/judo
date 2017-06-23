package net.dhleong.judo.input

import net.dhleong.judo.util.key

fun InputBuffer.type(string: String) {
    string
        .toCharArray()
        .map { key(it.toString()) }
        .forEach { this.type(it) }
}