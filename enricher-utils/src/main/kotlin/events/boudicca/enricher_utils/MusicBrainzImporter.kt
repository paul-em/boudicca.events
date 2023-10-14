package events.boudicca.enricher_utils

import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import kotlin.streams.asSequence

fun main() {

    val englishReader = BufferedReader(FileReader(File("C:\\projects\\boudicca\\musicbrainz_data\\words-english.txt")))
    val germanReader = BufferedReader(FileReader(File("C:\\projects\\boudicca\\musicbrainz_data\\words-german.txt")))

    val allWords = (englishReader.readLines() + germanReader.readLines()).map { it.lowercase() }.toSet()

    englishReader.close()
    germanReader.close()

    val reader = BufferedReader(FileReader(File("C:\\projects\\boudicca\\musicbrainz_data\\artist")))
    val out = BufferedWriter(FileWriter("C:\\projects\\boudicca\\musicbrainz_data\\artist_parsed.json", false))

    val artists = reader.lines().asSequence()
//        .take(100)
        .map { mapArtist(it) }
        .toList()

    val filteredArtists = getFilteredArtists(artists, allWords)

    serialize(filteredArtists).write(out)
    out.close()
    reader.close()
}

private fun getFilteredArtists(artists: List<Artist>, allWords: Set<String>): List<Artist> {
    var filtered = artists.filter { it.name.length >= 3 /*&& it.aliases.all { it.length >= 3 }*/ }

    filtered = filtered.filter { !allWords.contains(it.name.lowercase()) }

    filtered = filtered.filter { !it.ended }

//    filtered = filtered.filter { it.genre != null }

    val names = filtered.map { it.name.lowercase() }.groupBy { it }.mapValues { it.value.size }
    filtered = filtered.filter { names[it.name.lowercase()]!! == 1 }

    return filtered
}

fun serialize(artists: List<Artist>): JSONArray {
    val array = JSONArray()
    for (artist in artists) {
        val artistObject = JSONObject()
        artistObject.put("name", artist.name)
        artistObject.put("genre", artist.genre)
        val aliasesArray = JSONArray()
        for (alias in artist.aliases) {
            aliasesArray.put(alias)
        }
        artistObject.put("aliases", aliasesArray)
        array.put(artistObject)
    }
    return array
}

fun mapArtist(line: String): Artist {
    val jsonObject = JSONObject(line)
    val name = jsonObject.getString("name")
    val genre = mapGenre(jsonObject.getJSONArray("genres"))
    val aliases = jsonObject.getJSONArray("aliases").map { (it as JSONObject).getString("name") }
    val ended = jsonObject.has("ended") && jsonObject.getBoolean("ended")
    return Artist(name, genre, aliases, ended)
}

fun mapGenre(jsonArray: JSONArray): String? {
    val list = mutableListOf<Pair<String, Int>>()
    for (entry in jsonArray) {
        val obj = entry as JSONObject
        list.add(Pair(obj.getString("name"), obj.getInt("count")))
    }
    return list.sortedWith(
        Comparator.comparing<Pair<String, Int>?, Int?> { it.second }.reversed()
            .then(Comparator.comparing { it.first })
    ).firstOrNull()?.first
}

data class Artist(
    val name: String,
    val genre: String?,
    val aliases: List<String>,
    val ended: Boolean,
)