package net.dhleong.judo.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Source
import okio.buffer

/**
 * @author dhleong
 */
object Json {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(LinkedHashMap::class.java, object : JsonAdapter<LinkedHashMap<*, *>>() {
                val delegate = Moshi.Builder().build().adapter(Map::class.java)

                override fun fromJson(reader: JsonReader): LinkedHashMap<*, *>? =
                    LinkedHashMap(delegate.fromJson(reader))

                override fun toJson(writer: JsonWriter, value: LinkedHashMap<*, *>?) {
                    delegate.toJson(writer, value)
                }
            })
            .build()
    }

    inline fun <reified T> adapter() = moshi.adapter<T>(T::class.java)!!

    inline fun <reified T> read(json: Source): T =
        adapter<T>().fromJson(json.buffer())!!

    fun write(value: Any) =
        moshi.adapter(value.javaClass)
            .toJson(value)!!
}

