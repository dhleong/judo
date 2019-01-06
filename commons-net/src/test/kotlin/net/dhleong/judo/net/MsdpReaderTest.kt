package net.dhleong.judo.net

import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.Test

/**
 * @author dhleong
 */
class MsdpReaderTest {
    @Test fun readStringValue() {
        val reader = reader(MSDP_VAL, "SERENITY")
        assert(reader.readObject())
            .isEqualTo("SERENITY")
    }

    @Test fun readEmptyArrayValue() {
        val reader = reader(MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_ARRAY_CLOSE
        )
        assert(reader.readObject())
            .isEqualTo(listOf<String>())
    }

    @Test fun readStringArrayValue() {
        val reader = reader(MSDP_VAL,
            MSDP_ARRAY_OPEN,
            MSDP_VAL, "Kaylee",
            MSDP_VAL, "Frye",
            MSDP_ARRAY_CLOSE
        )
        assert(reader.readObject())
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
        assert(reader.readObject())
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

        assert(reader.readObject())
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

        assert(reader.readObject())
            .isEqualTo(mapOf(
                "Kaywinnet" to mapOf("Lee" to "Frye")
            ))
    }


    private fun reader(vararg parts: Any): MsdpReader {
        val intList = ArrayList<Int>()
        for (part in parts) {
            if (part is Int) intList.add(part)
            else if (part is String) {
                part.forEach { intList.add(it.toInt()) }
            }
        }

        val intArray = intList.toIntArray()
        return MsdpReader(intArray, 0, intArray.size)
    }
}