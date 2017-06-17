package net.dhleong.judo

import java.lang.reflect.Proxy

/**
 * @author dhleong
 */

val DUMMY_JUDO_CORE = Proxy.newProxyInstance(
        ClassLoader.getSystemClassLoader(),
        arrayOf(IJudoCore::class.java)
    ) { _, method, _ ->

    throw UnsupportedOperationException("$method not implemented")

} as IJudoCore

