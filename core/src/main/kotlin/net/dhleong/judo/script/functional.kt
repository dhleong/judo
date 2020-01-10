package net.dhleong.judo.script

/**
 * @author dhleong
 */
@Suppress("UNCHECKED_CAST")
fun JudoScriptingEntity.Function<*>.toFunctionalInterface(
    engine: ScriptingEngine
): JudoCallable = try {
    when (fn) {
        is Function0<*> -> Fn0(name, engine, fn as Function0<Any>)
        is Function1<*, *> -> {
            if (hasMultipleArities) {
                FnN(name, engine, fn as Function1<Array<out Any?>, Any>)
            } else {
                Fn1(name, engine, fn as Function1<Any, Any>)
            }
        }
        is Function2<*, *, *> -> Fn2(name, engine, fn as Function2<Any, Any, Any>)
        is Function3<*, *, *, *> -> Fn3(name, engine, fn as Function3<Any, Any, Any, Any>)
        else -> FnN(name, engine, fn as Function1<Array<out Any?>, Any>)
    }
} catch (e: ClassCastException) {
    throw IllegalArgumentException(
        "Error converting $name to FunctionalInterface", e
    )
}

interface JudoCallable {
    fun call(vararg args: Any): Any?
}

@FunctionalInterface
internal interface FIN : JudoCallable {
    fun invoke(vararg args: Any): Any?
    override fun call(vararg args: Any): Any? = invoke(*args)
}
@Suppress("UNCHECKED_CAST")
private class FnN(name: String, engine: ScriptingEngine, val fn: Function1<Array<out Any?>, Any?>) : FnBase(name, engine) {
    override fun invoke(vararg args: Any) = safely {
        fn.invoke(engine.toJava(args as Array<Any?>))
    }
}

/**
 * Base class that throws a useful error when an unexpected number
 *  of args is passed
 */
private abstract class FnBase(
    private val name: String,
    protected val engine: ScriptingEngine,
    private val expectedArgs: Int = -1
) : FIN {
    override fun invoke(vararg args: Any): Any? {
        val expectedStatement = when {
            expectedArgs != -1 -> "expected $expectedArgs, "
            else -> ""
        }
        throw IllegalArgumentException(
            "Incorrect arguments to $name(); $expectedStatement" +
                "received ${args.size}"
        )
    }

    protected inline fun <R> safely(block: () -> R): R {
        val result = try {
            block()
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                "Incorrect arguments to $name()", e
            )
        }

        if (result == null) {
            return result
        }

        @Suppress("UNCHECKED_CAST")
        return engine.toScript(result as Any) as R
    }
}

@FunctionalInterface
internal interface FI0 : Function0<Any>, FIN
private class Fn0(name: String, engine: ScriptingEngine, val fn: Function0<Any>) : FnBase(name, engine), FI0 {
    override fun invoke(vararg args: Any): Any? {
        if (args.isEmpty()) {
            return invoke()
        }
        return super.invoke(*args)
    }
    override fun invoke(): Any = safely { fn.invoke() }
}

@FunctionalInterface
internal interface FI1 : Function1<Any, Any>, FIN
private class Fn1(name: String, engine: ScriptingEngine, val fn: Function1<Any, Any>) : FnBase(name, engine), FI1 {
    override fun invoke(vararg args: Any): Any? {
        if (args.size == 1) {
            return invoke(args[0])
        }
        return super.invoke(*args)
    }
    override fun invoke(p1: Any): Any = safely { fn.invoke(engine.toJava(p1)) }
}

@FunctionalInterface
internal interface FI2 : Function2<Any, Any, Any>
private class Fn2(name: String, engine: ScriptingEngine, val fn: Function2<Any, Any, Any>) : FnBase(name, engine), FI2 {
    override fun invoke(vararg args: Any): Any? {
        if (args.size == 2) {
            return invoke(args[0], args[1])
        }
        return super.invoke(*args)
    }
    override fun invoke(p1: Any, p2: Any) = safely {
        fn.invoke(engine.toJava(p1), engine.toJava(p2))
    }
}

@FunctionalInterface
internal interface FI3 : Function3<Any, Any, Any, Any>
private class Fn3(name: String, engine: ScriptingEngine, val fn: Function3<Any, Any, Any, Any>) : FnBase(name, engine), FI3 {
    override fun invoke(vararg args: Any): Any? {
        if (args.size == 3) {
            return invoke(args[0], args[1], args[2])
        }
        return super.invoke(*args)
    }

    override fun invoke(p1: Any, p2: Any, p3: Any) = safely {
        fn.invoke(
            engine.toJava(p1),
            engine.toJava(p2),
            engine.toJava(p3)
        )
    }
}
