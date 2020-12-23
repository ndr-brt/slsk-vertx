package ndr.brt.slsk.model

import ndr.brt.slsk.Event
import ndr.brt.slsk.SearchResponded

data class SearchResult(val token: String = "", val results: List<SearchResponded> = emptyList()): Event {
  fun files() = results.flatMap { it.files }
}