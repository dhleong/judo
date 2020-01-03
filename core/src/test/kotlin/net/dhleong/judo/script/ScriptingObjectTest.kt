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
import net.dhleong.judo.script.init.CoreScripting
import net.dhleong.judo.script.init.UtilScripting
import org.junit.Test

/**
 * @author dhleong
 */
class ScriptingObjectTest {

    @Test fun `Extract param names and types`() {
        val inputFn = DocumentedScriptFunction.extractFrom(CoreScripting::class.java)
            .first { it.name == "complete" }
        assertThat(inputFn.doc.invocations).isNotNull().all {
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
        val allFns = DocumentedScriptFunction.extractFrom(CoreScripting::class.java)
        val completeFn = allFns.first { it.name == "complete" }
        assertThat(completeFn.doc.invocations).isNotNull().all {
            hasSize(1)
            each {
                it.doesNotHaveReturn()
            }
        }

        val inputFn = allFns.first { it.name == "input" }
        assertThat(inputFn.doc.invocations).isNotNull().all {
            hasSize(2)
            each {
                it.hasReturnType("String")
            }
        }
    }

    @Test fun `Extract nullable types`() {
        val expandPathFn = DocumentedScriptFunction.extractFrom(UtilScripting::class.java)
            .first { it.name == "expandpath" }
        assertThat(expandPathFn.doc.invocations!!.first()).all {
            hasReturnType("String?")
        }
    }
}

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


