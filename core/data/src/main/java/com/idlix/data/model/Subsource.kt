package com.idlix.data.model

import kotlinx.serialization.Serializable

/**
 * SubSource API v1 models (https://api.subsource.net/api/v1).
 *
 * Response envelope: { success, message, data } where data is a list for
 * search/list endpoints. Field names verified against the official API docs
 * and production clients of this API (Bazarr subsource provider).
 */
@Serializable
data class SubsourceMoviesResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<SubsourceMovie> = emptyList()
)

@Serializable
data class SubsourceMovie(
    val movieId: Long? = null,
    val title: String? = null,
    val alternateTitle: String? = null,
    val type: String? = null,
    val releaseYear: Int? = null,
    val imdbId: String? = null,
    val tmdbId: Long? = null,
    val season: Int? = null,
    val subtitleCount: Int? = null
)

@Serializable
data class SubsourceSubtitlesResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<SubsourceSubtitle> = emptyList(),
    val pagination: SubsourcePagination? = null
)

@Serializable
data class SubsourcePagination(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val pages: Int? = null
)

@Serializable
data class SubsourceSubtitle(
    val subtitleId: Long? = null,
    val movieId: Long? = null,
    val language: String? = null,
    val releaseInfo: List<String> = emptyList(),
    val commentary: String? = null,
    val hearingImpaired: Boolean = false,
    val foreignParts: Boolean = false,
    val downloads: Int? = null,
    val size: Long? = null
) {
    /** First release name, used as the row title in the subtitle picker. */
    val displayRelease: String
        get() = releaseInfo.firstOrNull { it.isNotBlank() } ?: "Subtitle"
}
