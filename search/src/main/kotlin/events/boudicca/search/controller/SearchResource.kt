package events.boudicca.search.controller

import events.boudicca.search.model.Filters
import events.boudicca.search.model.QueryDTO
import events.boudicca.search.model.SearchDTO
import events.boudicca.search.model.SearchResultDTO
import events.boudicca.search.service.QueryService
import events.boudicca.search.service.SearchService
import events.boudicca.search.service.SynchronizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping("/")
@RestController
class SearchResource @Autowired constructor(
    private val searchService: SearchService,
    private val queryService: QueryService,
    private val synchronizationService: SynchronizationService,
    @Value("\${boudicca.local.mode}") private val localMode: Boolean,
) {

    @Deprecated("it is recommended to use the query endpoint", ReplaceWith("/query"), DeprecationLevel.WARNING)
    @PostMapping(
        "search",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun search(@RequestBody searchDTO: SearchDTO): SearchResultDTO {
        if (localMode) {
            synchronizationService.update()
        }
        return searchService.search(searchDTO)
    }

    @GetMapping("filters")
    fun filters(): Filters {
        if (localMode) {
            synchronizationService.update()
        }
        return searchService.filters()
    }

    @PostMapping(
        "query",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun query(@RequestBody queryDTO: QueryDTO): SearchResultDTO {
        if (localMode) {
            synchronizationService.update()
        }
        return queryService.query(queryDTO)
    }
}
