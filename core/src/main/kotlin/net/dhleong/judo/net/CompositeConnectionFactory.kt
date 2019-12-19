package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import java.net.URI

/**
 * @author dhleong
 */
class CompositeConnectionFactory(
    private val candidates: List<JudoConnection.Factory>
) : JudoConnection.Factory {
    override suspend fun create(judo: IJudoCore, uri: URI): JudoConnection? {
        for (candidate in candidates) {
            candidate.create(judo, uri)?.let {
                // success!
                return it
            }
        }
        return null
    }
}