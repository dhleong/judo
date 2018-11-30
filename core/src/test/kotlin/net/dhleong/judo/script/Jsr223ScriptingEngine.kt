package net.dhleong.judo.script

import java.io.InputStream
import javax.script.ScriptEngineManager

/**
 * @author dhleong
 */
class Jsr223ScriptingEngine(
    extension: String = "js" // built in to JVM
) : ScriptingEngine {
    class Factory(
        private val forExtension: String = "js" // built in to JVM
    ) : ScriptingEngine.Factory {
        override fun supportsFileType(ext: String): Boolean = ext == forExtension

        override fun create(): ScriptingEngine = Jsr223ScriptingEngine(forExtension)
    }

    private val engine = ScriptEngineManager().getEngineByExtension(extension)

    override fun register(entity: JudoScriptingEntity) {
        when (entity) {
            is JudoScriptingEntity.Constant<*> -> {
                engine.put(entity.name, entity.value)
            }

            is JudoScriptingEntity.Function<*> -> {
                engine.put(entity.name, entity.toFunctionalInterface())
            }
        }
    }

    override fun execute(code: String) {
        engine.eval(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.eval(stream.bufferedReader())
    }
}

@Suppress("UNCHECKED_CAST")
private fun JudoScriptingEntity.Function<*>.toFunctionalInterface(): Any = when (fn) {
    is Function1<*, *> -> {
        if (hasMultipleUsages) {
            FnN(fn as Function1<Array<out Any?>, Any>)
        } else {
            Fn1(name, fn as Function1<Any, Any>)
        }
    }
    is Function2<*, *, *> -> Fn2(name, fn as Function2<Any, Any, Any>)
    is Function3<*, *, *, *> -> Fn3(name, fn as Function3<Any, Any, Any, Any>)
    is Function4<*, *, *, *, *> -> Fn4(name, fn as Function4<Any, Any, Any, Any, Any>)
    else -> FnN(fn as Function1<Array<out Any?>, Any>)
}

@FunctionalInterface
internal interface FIN {
    fun invoke(vararg args: Any): Any
}
private class FnN(val fn: Function1<Array<out Any?>, Any>) : FIN {
    override fun invoke(vararg args: Any) = fn.invoke(args)
}

/**
 * Base class that throws a useful error when an unexpected number
 *  of args is passed
 */
private abstract class FnBase(private val name: String) : FIN {
    override fun invoke(vararg args: Any): Any {
        throw IllegalArgumentException(
            "Incorrect arguments to $name()"
        )
    }
}

@FunctionalInterface
internal interface FI1 : Function1<Any, Any>, FIN
private class Fn1(name: String, val fn: Function1<Any, Any>) : FnBase(name), FI1 {
    override fun invoke(p1: Any): Any = fn.invoke(p1)
}

@FunctionalInterface
internal interface FI2 : Function2<Any, Any, Any>
private class Fn2(name: String, val fn: Function2<Any, Any, Any>) : FnBase(name), FI2 {
    override fun invoke(p1: Any, p2: Any) = fn.invoke(p1, p2)
}

@FunctionalInterface
internal interface FI3 : Function3<Any, Any, Any, Any>
private class Fn3(name: String, val fn: Function3<Any, Any, Any, Any>) : FnBase(name), FI3 {
    override fun invoke(p1: Any, p2: Any, p3: Any) = fn.invoke(p1, p2, p3)
}

@FunctionalInterface
internal interface FI4 : Function4<Any, Any, Any, Any, Any>
private class Fn4(name: String, val fn: Function4<Any, Any, Any, Any, Any>) : FnBase(name), FI4 {
    override fun invoke(p1: Any, p2: Any, p3: Any, p4: Any) = fn.invoke(p1, p2, p3, p4)
}
