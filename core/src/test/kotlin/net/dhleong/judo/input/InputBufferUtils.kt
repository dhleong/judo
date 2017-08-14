package net.dhleong.judo.input

fun InputBuffer.type(string: String) {
    string
        .toCharArray()
        .map { key(it.toString()) }
        .forEach { this.type(it) }
}