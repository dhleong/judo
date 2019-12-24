package net.dhleong.judo.net.options

import assertk.assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.net.MSDP_ARRAY_CLOSE
import net.dhleong.judo.net.MSDP_ARRAY_OPEN
import net.dhleong.judo.net.MSDP_TABLE_CLOSE
import net.dhleong.judo.net.MSDP_TABLE_OPEN
import net.dhleong.judo.net.MSDP_VAL
import net.dhleong.judo.net.MSDP_VAR
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.write
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * @author dhleong
 */
class MsdpReaderTest {
    @Test fun readStringValue() {
        val reader = reader(MSDP_VAL, "SERENITY")
        assertThat(reader.readObject())
            .isEqualTo("SERENITY")
    }

    @Test fun readEmptyArrayValue() {
        val reader = reader(MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_ARRAY_CLOSE
        )
        assertThat(reader.readObject())
            .isEqualTo(listOf<String>())
    }

    @Test fun readStringArrayValue() {
        val reader = reader(MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_VAL, "Kaylee",
            MSDP_VAL, "Frye",
            MSDP_ARRAY_CLOSE
        )
        assertThat(reader.readObject())
            .isEqualTo(listOf("Kaylee", "Frye"))
    }

    @Test fun readNestedArrayValue() {
        val reader = reader(MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_VAL, "Kaywinnet",
            MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_VAL, "Lee",
            MSDP_VAL, "Frye",
            MSDP_ARRAY_CLOSE,
            MSDP_ARRAY_CLOSE
        )
        assertThat(reader.readObject())
            .isEqualTo(listOf("Kaywinnet", listOf("Lee", "Frye")))
    }

    @Test fun readTableValue() {
        val reader = reader(MSDP_VAL,
            MSDP_TABLE_OPEN,
            MSDP_VAR, "Kaylee",
            MSDP_VAL, "Frye",
            MSDP_VAR, "Mal",
            MSDP_VAL, "Reynolds",
            MSDP_TABLE_CLOSE
        )

        assertThat(reader.readObject())
            .isEqualTo(mapOf(
                "Kaylee" to "Frye",
                "Mal" to "Reynolds"
            ))
    }

    @Test fun readNestedTableValue() {
        val reader = reader(MSDP_VAL,
            MSDP_TABLE_OPEN,
            MSDP_VAR, "Kaywinnet",
            MSDP_VAL,
            MSDP_TABLE_OPEN,
            MSDP_VAR, "Lee",
            MSDP_VAL, "Frye",
            MSDP_TABLE_CLOSE,
            MSDP_TABLE_CLOSE
        )

        assertThat(reader.readObject())
            .isEqualTo(mapOf(
                "Kaywinnet" to mapOf("Lee" to "Frye")
            ))
    }


    private fun reader(vararg parts: Any): MsdpReader {
        val bytes = ByteArrayOutputStream().apply {
            for (part in parts) {
                when (part) {
                    is Byte -> write(part)
                    is String -> write(part)
                    else -> throw IllegalArgumentException("Unexpected `$part`")
                }
            }
        }.toByteArray()

        return MsdpReader(TelnetEvent(bytes), 0)
    }
}
