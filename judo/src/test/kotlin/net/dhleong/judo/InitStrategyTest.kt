package net.dhleong.judo

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFailure
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
        assertThat(strategy).isInstanceOf(InitStrategy.Nop::class)
    }

    @Test fun `Handle excessive arguments`() {
        assertThat {
            InitStrategy.pick(listOf("ssl", "host", "23"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("arguments")
            }
        }
    }

    @Test fun `Parse simple host-colon-port as Uri`() {
        val strategy = InitStrategy.pick(listOf("host:23"))
        assertThat(strategy).all {
            hasSchema("telnet")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Parse simple host and port pair as Uri`() {
        val strategy = InitStrategy.pick(listOf("host", "23"))
        assertThat(strategy).all {
            hasSchema("telnet")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Support full URIs`() {
        val strategy = InitStrategy.pick(listOf("ssl://host:23"))
        assertThat(strategy).all {
            hasSchema("ssl")
            hasHost("host")
            hasPort(23)
        }
    }

    @Test fun `Handle python world script`() {
        val strategy = InitStrategy.pick(listOf("world.py"))
        assertThat(strategy).all {
            hasScriptFile(File("world.py"))
        }
    }

    @Test fun `Complain about unknown script type`() {
        assertThat {
            InitStrategy.pick(listOf("world.c"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("scripting")
            }
        }
    }

    @Test fun `Complain about invalid port`() {
        assertThat {
            InitStrategy.pick(listOf("host", "port"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("port")
            }
        }
    }

    @Test fun `Complain about invalid port in scheme`() {
        assertThat {
            InitStrategy.pick(listOf("host:port"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("port")
            }
        }
    }

}

private fun Assert<InitStrategy>.hasHost(host: String) {
    asUriStrategy().given { actual ->
        val uri = actual.uri
        if (uri.host == host) return
        expected("host to be ${show(host)} but was ${show(uri.host)}")
    }
}

private fun Assert<InitStrategy>.hasPort(port: Int) {
    asUriStrategy().given { actual ->
        val uri = actual.uri
        if (uri.port == port) return
        expected("port to be ${show(port)} but was ${show(uri.port)}")
    }
}

private fun Assert<InitStrategy>.hasSchema(scheme: String) {
    asUriStrategy().given { actual ->
        val uri = actual.uri
        if (uri.scheme == scheme) return
        expected("scheme to be ${show(scheme)} but was ${show(uri.scheme)}")
    }
}

private fun Assert<InitStrategy>.asUriStrategy() = transform { actual ->
    assertThat(actual).isInstanceOf(InitStrategy.Uri::class)
    actual as InitStrategy.Uri
}

private fun Assert<InitStrategy>.hasScriptFile(file: File) {
    asFileStrategy().given { actual ->
        val actualFile = actual.worldScriptFile
        if (actualFile == file.absoluteFile) return
        expected("script file to be ${show(file)} but was ${show(actualFile)}")
    }
}

private fun Assert<InitStrategy>.asFileStrategy() = transform { actual ->
    assertThat(actual).isInstanceOf(InitStrategy.WorldScript::class)
    actual as InitStrategy.WorldScript
}

