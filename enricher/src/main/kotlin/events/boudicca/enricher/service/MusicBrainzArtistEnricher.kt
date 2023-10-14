package events.boudicca.enricher.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import events.boudicca.SemanticKeys
import events.boudicca.enricher.model.Event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class MusicBrainzArtistEnricher @Autowired constructor(
    @Value("\${boudicca.enricher.musicbrainz.path:}") musicBrainzDataPath: String?,
) : Enricher {

    private val LOG = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val data = loadData(musicBrainzDataPath)
    private val enabled: Boolean = data != null

    private fun loadData(musicBrainzDataPath: String?): List<Artist>? {
        if (musicBrainzDataPath.isNullOrBlank()) {
            LOG.debug("no musicBrainzDataPath given, disabling enricher")
            return null
        }
        val file = File(musicBrainzDataPath)
        if (!file.exists() || !file.isFile || !file.canRead()) {
            throw IllegalArgumentException("musicbrainz data path $musicBrainzDataPath is not a readable file!")
        }
        return objectMapper
            .readValue(file, object : TypeReference<List<Artist>?>() {})
    }

    override fun enrich(e: Event): Event {
        if (!enabled) {
            return e
        }
        return doEnrich(e, data!!)
    }

    private fun doEnrich(e: Event, artists: List<Artist>): Event {
        val foundArtists = mutableListOf<Artist>()
        for (artist in artists) {
            if (matches(e, artist)) {
                foundArtists.add(artist)
            }
        }
        if (foundArtists.isNotEmpty()) {
            val nonSubstringArtists = foundArtists.filter { artist ->
                foundArtists.none { it.name.length != artist.name.length && it.name.contains(artist.name, true) }
            }
            return insertArtistData(e, nonSubstringArtists)
        }
        return e
    }

    private fun matches(e: Event, artist: Artist): Boolean {
        val eventName = e.name
        val artistName = artist.name
        val i = eventName.indexOf(artistName, ignoreCase = true)
        return i != -1 &&
                (i == 0 || !eventName[i - 1].isLetterOrDigit()) &&
                (i + artistName.length >= eventName.length || !eventName[i + artistName.length].isLetterOrDigit())
    }

    private fun insertArtistData(e: Event, artists: List<Artist>): Event {
        val enrichedData = e.data?.toMutableMap() ?: mutableMapOf()
        enrichedData[SemanticKeys.CONCERT_BANDLIST] = artists.joinToString(", ") { it.name }
        val genre = artists.firstNotNullOfOrNull { it.genre }
        if (genre != null && !enrichedData.containsKey(SemanticKeys.CONCERT_GENRE)) {
            enrichedData[SemanticKeys.CONCERT_GENRE] = genre
        }
        return Event(e.name, e.startDate, enrichedData)
    }

    data class Artist(
        val name: String,
        val genre: String?,
        var aliases: List<String>
    )
}
