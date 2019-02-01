package net.dhleong.judo

import assertk.Assert
import assertk.all
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class InitStrategyTest {
    @Test fun `Handle no arguments`() {
        val strategy = InitStrategy.pick(emptyList())
        assert(strategy).isInstanceOf(InitStrategy.Nop::class)
    }

    @Test fun `Handle excessive arguments`() {
        assert {
            InitStrategy.pick(listOf("ssl", "host", "23"))
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }
    }

    @Test fun `Parse simple host-colon-port as Uri`() {
        val strategy = InitStrategy.pick(listOf("host:23"))
        assert(strategy).all {
            hasSchema("telnet")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Parse simple host and port pair as Uri`() {
        val strategy = InitStrategy.pick(listOf("host", "23"))
        assert(strategy).all {
            hasSchema("telnet")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Support full URIs`() {
        val strategy = InitStrategy.pick(listOf("ssl://host:23"))
        assert(strategy).all {
            hasSchema("ssl")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Handle python world script`() {
        val strategy = InitStrategy.pick(listOf("world.py"))
        assert(strategy).all {
            hasScriptFile(File("world.py"))
        }
    }

    @Test fun `Complain about unknown script type`() {
        assert {
            InitStrategy.pick(listOf("world.c"))
        }.thrownError {
            message().isNotNull {
                it.contains("scripting")
            }
        }
    }

    @Test fun `Complain about invalid port`() {
        assert {
            InitStrategy.pick(listOf("host", "port"))
        }.thrownError {
            message().isNotNull {
                it.contains("port")
            }
        }
    }

    @Test fun `Complain about invalid port in scheme`() {
        assert {
            InitStrategy.pick(listOf("host:port"))
        }.thrownError {
            message().isNotNull {
                it.contains("port")
            }
        }
    }

}

private fun Assert<InitStrategy>.hasHost(host: String) {
    val uri = asUriStrategy().uri
    if (uri.host == host) return
    expected("host to be ${show(host)} but was ${show(uri.host)}")
}

private fun Assert<InitStrategy>.hasPort(port: Int) {
    val uri = asUriStrategy().uri
    if (uri.port == port) return
    expected("port to be ${show(port)} but was ${show(uri.port)}")
}

private fun Assert<InitStrategy>.hasSchema(scheme: String) {
    val uri = asUriStrategy().uri
    if (uri.scheme == scheme) return
    expected("scheme to be ${show(scheme)} but was ${show(uri.scheme)}")
}

private fun Assert<InitStrategy>.asUriStrategy(): InitStrategy.Uri {
    assert(actual).isInstanceOf(InitStrategy.Uri::class)
    return actual as InitStrategy.Uri
}

private fun Assert<InitStrategy>.hasScriptFile(file: File) {
    val actualFile = asFileStrategy().worldScriptFile
    if (actualFile == file.absoluteFile) return
    expected("script file to be ${show(file)} but was ${show(actualFile)}")
}

private fun Assert<InitStrategy>.asFileStrategy(): InitStrategy.WorldScript {
    assert(actual).isInstanceOf(InitStrategy.WorldScript::class)
    return actual as InitStrategy.WorldScript
}

