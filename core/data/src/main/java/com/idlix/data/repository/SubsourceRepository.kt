package com.idlix.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.idlix.data.api.ApiClient
import com.idlix.data.model.SubsourceMovie
import com.idlix.data.model.SubsourceMoviesResponse
import com.idlix.data.model.SubsourceQuery
import com.idlix.data.model.SubsourceSearchResult
import com.idlix.data.model.SubsourceSubtitle
import com.idlix.data.model.SubsourceSubtitlesResponse
import com.idlix.data.model.SubtitleTrack
import com.idlix.data.util.SubdlTitleCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.zip.ZipInputStream

/**
 * SubSource (subsource.net) subtitle provider, Indonesian subtitles only.
 *
 * Mirrors [SubdlRepository]: the API key is user-supplied and stored in the
 * "idlix_config" SharedPreferences. Unlike SubDL there is no bundled default
 * key, so search reports [SubsourceSearchResult.Error.Reason.NO_API_KEY]
 * until the user pastes a key in Settings.
 *
 * API v1 facts (official docs + production clients):
 * - Base URL https://api.subsource.net/api/v1, auth header "X-API-Key".
 * - Movie resolution: GET /movies/search?searchType=text|imdb (no TMDB search).
 * - Subtitle list: GET /subtitles?movieId=&language=indonesian (full lowercase
 *   language name, not an ISO code), seasonNumber/episodeNumber for episodes.
 * - Download: GET /subtitles/{subtitleId}/download returns ZIP bytes directly.
 */
object SubsourceRepository {
    private const val TAG = "SubsourceRepository"
    private const val PREF_KEY_SUBSOURCE_API_KEY = "subsource_api_key"
    private const val BASE_URL = "https://api.subsource.net/api/v1"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 Idlix"

    @Volatile
    private var currentApiKey: String? = null

    fun getApiKey(): String {
        currentApiKey?.let { return it }
        val saved = try {
            val prefs = ApiClient.applicationContext?.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs?.getString(PREF_KEY_SUBSOURCE_API_KEY, null)
        } catch (_: Exception) {
            null
        }
        val key = saved?.trim().orEmpty()
        currentApiKey = key
        return key
    }

    fun setApiKey(newKey: String) {
        val trimmed = newKey.trim()
        currentApiKey = trimmed
        try {
            val prefs = ApiClient.applicationContext?.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs?.edit()?.putString(PREF_KEY_SUBSOURCE_API_KEY, trimmed)?.apply()
        } catch (_: Exception) {}
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    suspend fun testApiKey(targetKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = targetKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("SubSource API key cannot be empty"))
        }
        val testEndpoint = "$BASE_URL/movies/search?searchType=text&q=Avatar"
        try {
            val req = Request.Builder()
                .url(testEndpoint)
                .header("User-Agent", USER_AGENT)
                .header("X-API-Key", trimmed)
                .get()
                .build()
            ApiClient.okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val parsed = try {
                        ApiClient.json.decodeFromString<SubsourceMoviesResponse>(body)
                    } catch (_: Exception) { null }

                    if (parsed?.success == true) {
                        Result.success("SubSource API key is valid (${parsed.data.size} movies found)")
                    } else {
                        Result.failure(Exception("Invalid response or quota exceeded"))
                    }
                } else {
                    Result.failure(Exception("HTTP error: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildMovieSearchUrl(query: SubsourceQuery): String {
        val params = StringBuilder()
        if (!query.imdbId.isNullOrBlank()) {
            params.append("searchType=imdb")
            params.append("&imdb=${URLEncoder.encode(query.imdbId.trim(), "UTF-8")}")
        } else {
            params.append("searchType=text")
            val name = query.filmName?.trim().orEmpty()
            params.append("&q=${URLEncoder.encode(name, "UTF-8")}")
        }
        if (query.year != null && query.year > 0) {
            params.append("&year=${query.year}")
        }
        val apiType = when {
            query.type == "tv" || query.type == "series" -> "series"
            query.type == "movie" -> "movie"
            else -> "all"
        }
        params.append("&type=$apiType")
        if (apiType == "series" && query.season != null) {
            params.append("&season=${query.season}")
        }
        return "$BASE_URL/movies/search?$params"
    }

    private fun buildSubtitleListUrl(query: SubsourceQuery, movieId: Long): String {
        val params = StringBuilder("movieId=$movieId")
        params.append("&language=indonesian")
        params.append("&limit=100")
        if (query.season != null) params.append("&seasonNumber=${query.season}")
        if (query.episode != null) params.append("&episodeNumber=${query.episode}")
        return "$BASE_URL/subtitles?$params"
    }

    private fun httpError(code: Int, message: String): SubsourceSearchResult.Error {
        val reason = when (code) {
            401, 403 -> SubsourceSearchResult.Error.Reason.INVALID_KEY
            429 -> SubsourceSearchResult.Error.Reason.QUOTA
            else -> SubsourceSearchResult.Error.Reason.HTTP
        }
        return SubsourceSearchResult.Error(reason, message)
    }

    suspend fun search(query: SubsourceQuery): SubsourceSearchResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext SubsourceSearchResult.Error(
                SubsourceSearchResult.Error.Reason.NO_API_KEY,
                "SubSource API key is not set"
            )
        }

        try {
            // Step 1: resolve the title to a SubSource movieId.
            var candidates: List<SubsourceMovie> = emptyList()
            val movieId: Long = if (query.movieId != null && query.movieId > 0) {
                query.movieId
            } else {
                val movieUrl = buildMovieSearchUrl(query)
                val request = Request.Builder()
                    .url(movieUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("X-API-Key", apiKey)
                    .build()
                val response = ApiClient.okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "SubSource movie search failed with code: ${response.code}")
                    return@withContext httpError(response.code, "HTTP error: ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val parsed = try {
                    ApiClient.json.decodeFromString<SubsourceMoviesResponse>(body)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse SubSource movie response: ${e.message}")
                    return@withContext SubsourceSearchResult.Error(
                        SubsourceSearchResult.Error.Reason.PARSE, e.localizedMessage
                    )
                }
                candidates = parsed.data.filter { it.movieId != null }
                if (candidates.isEmpty()) {
                    return@withContext SubsourceSearchResult.NotFound(candidates = emptyList())
                }
                val chosen = if (query.year != null) {
                    candidates.firstOrNull { it.releaseYear == query.year } ?: candidates.first()
                } else {
                    candidates.first()
                }
                chosen.movieId!!
            }

            // Step 2: list Indonesian subtitles for that movieId.
            val listUrl = buildSubtitleListUrl(query, movieId)
            val listRequest = Request.Builder()
                .url(listUrl)
                .header("User-Agent", USER_AGENT)
                .header("X-API-Key", apiKey)
                .build()
            val listResponse = ApiClient.okHttpClient.newCall(listRequest).execute()
            if (!listResponse.isSuccessful) {
                Log.w(TAG, "SubSource subtitle list failed with code: ${listResponse.code}")
                return@withContext httpError(listResponse.code, "HTTP error: ${listResponse.code}")
            }
            val listBody = listResponse.body?.string().orEmpty()
            val listParsed = try {
                ApiClient.json.decodeFromString<SubsourceSubtitlesResponse>(listBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SubSource subtitle response: ${e.message}")
                return@withContext SubsourceSearchResult.Error(
                    SubsourceSearchResult.Error.Reason.PARSE, e.localizedMessage
                )
            }

            // Safety filter: keep Indonesian entries only (language is a full
            // name in responses, e.g. "Indonesian").
            val subtitles = listParsed.data.filter {
                it.language.isNullOrBlank() || it.language.equals("indonesian", ignoreCase = true)
            }

            if (subtitles.isNotEmpty()) {
                SubsourceSearchResult.Found(subtitles = subtitles, candidates = candidates)
            } else {
                SubsourceSearchResult.NotFound(candidates = candidates)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search SubSource subtitles: ${e.message}")
            SubsourceSearchResult.Error(SubsourceSearchResult.Error.Reason.NETWORK, e.localizedMessage)
        }
    }

    suspend fun searchIndonesianSubtitles(
        title: String,
        type: String = "movie",
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): SubsourceSearchResult {
        val cleaned = SubdlTitleCleaner.clean(title)
        val query = SubsourceQuery(
            filmName = cleaned.title,
            year = cleaned.year,
            type = type,
            imdbId = imdbId,
            season = season,
            episode = episode
        )
        return search(query)
    }

    suspend fun downloadSubtitle(
        sub: SubsourceSubtitle,
        context: Context
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        try {
            val subtitleId = sub.subtitleId ?: return@withContext null
            val apiKey = getApiKey()
            if (apiKey.isBlank()) return@withContext null

            val downloadUrl = "$BASE_URL/subtitles/$subtitleId/download"
            val prefix = "subsource_id_${subtitleId}_${System.currentTimeMillis()}"
            val extractedFile = downloadAndExtractSrt(downloadUrl, apiKey, context, prefix)
                ?: return@withContext null

            SubtitleTrack(
                lang = "id",
                label = "Indonesian [SubSource]",
                path = Uri.fromFile(extractedFile).toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download SubSource subtitle: ${e.message}")
            null
        }
    }

    private fun downloadAndExtractSrt(downloadUrl: String, apiKey: String, context: Context, filePrefix: String): File? {
        return try {
            val req = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", USER_AGENT)
                .header("X-API-Key", apiKey)
                .build()

            val res = ApiClient.okHttpClient.newCall(req).execute()
            if (!res.isSuccessful) return null

            val bytes = res.body?.bytes() ?: return null
            val subDir = File(context.cacheDir, "subtitles").apply { mkdirs() }

            // SubSource downloads are ZIP archives; fall back to saving the
            // payload as a raw .srt if it is not one.
            val isZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
            if (!isZip) {
                val outFile = File(subDir, "$filePrefix.srt")
                FileOutputStream(outFile).use { it.write(bytes) }
                return outFile
            }

            val zipStream = ZipInputStream(bytes.inputStream())
            var entry = zipStream.nextEntry
            var targetFile: File? = null

            while (entry != null) {
                val name = entry.name.lowercase()
                if (name.endsWith(".srt") || name.endsWith(".vtt")) {
                    val ext = if (name.endsWith(".vtt")) ".vtt" else ".srt"
                    val outFile = File(subDir, "$filePrefix$ext")
                    FileOutputStream(outFile).use { fos ->
                        zipStream.copyTo(fos)
                    }
                    targetFile = outFile
                    break
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting SubSource download: ${e.message}")
            null
        }
    }
}
