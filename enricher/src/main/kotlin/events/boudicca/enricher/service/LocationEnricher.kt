package events.boudicca.enricher.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import events.boudicca.SemanticKeys
import events.boudicca.enricher.model.Event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


@Service
class LocationEnricher @Autowired constructor(
    @Value("\${boudicca.enricher.location.googleCredentialsPath:}") googleCredentialsPath: String?,
) : Enricher {

    private val LOG = LoggerFactory.getLogger(this.javaClass)
    private val updateLock = ReentrantLock()
    private val updater = createUpdater(googleCredentialsPath)
    private var data = emptyList<LocationData>()

    override fun enrich(e: Event): Event {
        for (locationData in data) {
            if (e.data != null && matches(e.data, locationData)) {
                val enrichedData = e.data.toMutableMap()
                for (locationDatum in locationData) {
                    enrichedData[locationDatum.key] = locationDatum.value.first()
                }
                return Event(e.name, e.startDate, enrichedData)
            }
        }
        return e
    }

    private fun matches(eventData: Map<String, String>, locationData: Map<String, List<String>>): Boolean {
        for (locationDatumKey in listOf(SemanticKeys.LOCATION_NAME, SemanticKeys.LOCATION_ADDRESS)) {
            val locationDatumValue = locationData[locationDatumKey]!!
            val eventDatum = eventData[locationDatumKey]
            if (eventDatum != null) {
                for (locationDatumLine in locationDatumValue) {
                    if (eventDatum == locationDatumLine) {
                        return true
                    }
                }
            }
        }
        return false
    }

    @EventListener
    fun onEventsUpdate(event: ForceUpdateEvent) {
        updateData()
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun update() {
        updateData()
    }

    private fun updateData() {
        updateLock.lock()
        try {
            if (updater != null) {
                data = updater.updateData()
            }
        } finally {
            updateLock.unlock()
        }
    }

    private fun createUpdater(googleCredentialsPath: String?): Updater? {
        return if (!googleCredentialsPath.isNullOrBlank()) {
            Updater(googleCredentialsPath)
        } else {
            null
        }
    }

    class Updater(private val googleCredentialsPath: String) {
        private val LOG = LoggerFactory.getLogger(this.javaClass)
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()
        private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        private val spreadsheetId = "1yYOE5gRR6gjNBim7hwEe3__fXoRAMtREkYbs-lsn7uM"
        private val range = "LocationData!A1:Z"
        private val credentials = createCredentials()
        private val service = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, HttpCredentialsAdapter(credentials))
            .setApplicationName("Boudicca Location Data Enricher")
            .build()

        private fun createCredentials(): GoogleCredentials {
            var credentials: GoogleCredentials =
                GoogleCredentials.fromStream(FileInputStream(googleCredentialsPath))
            credentials = credentials.createScoped(SheetsScopes.SPREADSHEETS_READONLY)
            return credentials
        }

        fun updateData(): List<LocationData> {
            credentials.refreshIfExpired()
            val response: ValueRange = service.spreadsheets().values()[spreadsheetId, range]
                .execute()
            val values: List<List<Any?>>? = response.getValues()
            if (values.isNullOrEmpty()) {
                LOG.error("no data found in spreadsheet!")
                return emptyList()
            }

            val headers = values[0].filterNotNull().map { it.toString() }.filter { it.isNotBlank() }

            val allLocationData = mutableListOf<LocationData>()
            for (row in values.subList(1, values.size)) {
                val locationData = mutableMapOf<String, List<String>>()

                for (i in headers.indices) {
                    val value = row[i] as String?
                    if (!value.isNullOrBlank()) {
                        locationData[headers[i]] = value.split("\n")
                    }
                }
                allLocationData.add(locationData)
            }

            return allLocationData
        }
    }

}

typealias LocationData = Map<String, List<String>>
