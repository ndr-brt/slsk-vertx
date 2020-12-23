package ndr.brt.slsk.model

import ndr.brt.slsk.Event
import ndr.brt.slsk.SearchResponded

data class SearchResult(val token: String = "", private val results: List<SearchResponded> = emptyList()): Event {
  fun files() = sortedResults().flatMap { it.files }
  fun filesWithSlot() = sortedResults().filter { it.slots }.flatMap { it.files }
  private fun sortedResults() = results.sortedByDescending { it.uploadSpeed }
}