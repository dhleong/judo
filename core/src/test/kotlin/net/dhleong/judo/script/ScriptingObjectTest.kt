package net.dhleong.judo.script

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.script.init.ConnectionScripting
import net.dhleong.judo.script.init.CoreScripting
import net.dhleong.judo.script.init.UtilScripting
import org.junit.Test
import kotlin.reflect.KClass

/**
 * @author dhleong
 */
class ScriptingObjectTest {

    @Test fun `Extract param names and types`() {
        val completeFn = CoreScripting::class["complete"]
        assertThat(completeFn.doc.invocations).isNotNull().all {
            hasSize(1)
            each { invocation ->
                invocation.transform("param") { it.args[0] }.all {
                    hasName("text")
                    hasType("String")
                }
            }
        }
    }

    @Test fun `Extract return types`() {
        val completeFn = CoreScripting::class["complete"]
        assertThat(completeFn.doc.invocations).isNotNull().all {
            hasSize(1)
            each {
                it.doesNotHaveReturn()
            }
        }

        val isConnectedFn = ConnectionScripting::class["isConnected"]
        assertThat(isConnectedFn.doc.invocations).isNotNull().all {
            hasSize(1)
            each {
                it.hasReturnType("Boolean")
            }
        }
    }

    @Test fun `Extract nullable types`() {
        val expandPathFn = UtilScripting::class["expandpath"]
        assertThat(expandPathFn.doc.invocations!!.first()).all {
            hasReturnType("String?")
        }
    }
}

private operator fun KClass<out ScriptingObject>.get(name: String) =
    DocumentedScriptFunction.extractFrom(this.javaObjectType)
            .first { it.name == name }

private fun Assert<JudoScriptInvocation>.hasReturnType(returnType: String) = given { actual ->
    if (actual.returnType == returnType) return
    expected("returnType == ${show(returnType)} but was ${show(actual.returnType)}")
}

private fun Assert<JudoScriptInvocation>.doesNotHaveReturn() = given { actual ->
    if (actual.returnType == null) return
    expected("no returnType but was ${show(actual.returnType)}")
}

private fun Assert<JudoScriptArgument>.hasName(name: String) =
    transform("name") { it.name }.isEqualTo(name)

private fun Assert<JudoScriptArgument>.hasType(type: String) =
    transform("type") { it.type }.isEqualTo(type)


