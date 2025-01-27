package events.boudicca.enricher.service

import events.boudicca.enricher.model.EnrichRequestDTO
import events.boudicca.enricher.model.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class EnricherService @Autowired constructor(
    private val enrichers: List<Enricher>,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun enrich(enrichRequestDTO: EnrichRequestDTO): List<Event> {
        return (enrichRequestDTO.events ?: emptyList()).map {
            var enrichedEvent = it
            for (enricher in enrichers) {
                enrichedEvent = enricher.enrich(enrichedEvent)
            }
            enrichedEvent
        }
    }

    fun forceUpdate() {
        eventPublisher.publishEvent(ForceUpdateEvent())
    }

}

class ForceUpdateEvent
