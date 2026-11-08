package com.idlix.data.model

/**
 * Query + result types for SubSource subtitle search, mirroring the
 * SubDL equivalents (SubdlQuery / SubdlSearchResult).
 *
 * SubSource has no TMDB search: titles are resolved by text search or
 * IMDb id via GET /movies/search, then subtitles are listed per movieId.
 */
data class SubsourceQuery(
    val filmName: String? = null,
    val movieId: Long? = null,
    val imdbId: String? = null,
    val year: Int? = null,
    /** "movie" or "tv" (mapped to SubSource's "movie"/"series"). */
    val type: String = "movie",
    val season: Int? = null,
    val episode: Int? = null
)

sealed interface SubsourceSearchResult {
    data class Found(
        val subtitles: List<SubsourceSubtitle>,
        val candidates: List<SubsourceMovie> = emptyList()
    ) : SubsourceSearchResult

    data class NotFound(
        val candidates: List<SubsourceMovie> = emptyList()
    ) : SubsourceSearchResult

    data class Error(
        val reason: Reason,
        val message: String? = null
    ) : SubsourceSearchResult {
        enum class Reason {
            NETWORK,
            NO_API_KEY,
            INVALID_KEY,
            QUOTA,
            HTTP,
            PARSE
        }
    }
}
