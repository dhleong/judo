package net.dhleong.judo.mapping

import java.io.File

/**
 * @author dhleong
 */
interface IMapManager {
    val current: IJudoMap?

    /**
     * Load the current map from disk
     */
    fun load(file: File)

    /**
     * Write the current map to disk, overwriting the
     * one we previously loaded in
     */
    fun save()

    /**
     * Write the current map to disk
     */
    fun saveAs(file: File, format: String = "tintin")


    /**
     * Set the current room id
     */
    fun goto(roomId: Int): Boolean {
        val map = current
        if (map != null) {
            map.inRoom = roomId
            return true
        }
        return false
    }
}