package net.dhleong.judo

import java.lang.reflect.Method

/**
 * @author dhleong
 */
@Suppress("FunctionName")
inline fun <reified T> Proxy(): T {
    return Proxy { method, _ ->
        if (method.name == "toString") {
            "Proxy<${T::class.java}>()"
        } else {
            throw UnsupportedOperationException(
                "${method.name} is not implemented"
            )
        }
    }
}

@Suppress("FunctionName")
inline fun <reified T> Proxy(crossinline methodHandler: (method: Method, args: Array<Any?>) -> Any?) =
    java.lang.reflect.Proxy.newProxyInstance(
        ClassLoader.getSystemClassLoader(),
        arrayOf(T::class.java)
    ) { _, method, args ->
        methodHandler(method, args ?: emptyArray())
    } as T

@Suppress("FunctionName")
inline fun <reified T> DumbProxy() = Proxy<T> { _, _ -> null }

