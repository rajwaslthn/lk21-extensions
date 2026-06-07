package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty

class LK21Provider : MainAPI() {
    override var mainUrl = "https://tv3.lk21online.mom"
    override var name = "LK21"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "id"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Film Terbaru",
        "$mainUrl/series/" to "Series Terbaru",
        "$mainUrl/genre/action/" to "Action",
        "$mainUrl/genre/drama/" to "Drama",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(request.data).document
        val items = doc.select("li.slider, li.listitem").mapNotNull { el ->
            val title = el.select("h3.poster-title").text().ifEmpty { el.select("figcaption").text() }
            val href = el.select("a").attr("href")
            val poster = el.select("img").attr("data-src").ifEmpty { el.select("img").attr("src") }
            if (title.isEmpty() || href.isEmpty()) return@mapNotNull null
            newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search/?s=$query").document
        return doc.select("li.slider, li.listitem").mapNotNull { el ->
            val title = el.select("h3.poster-title").text().ifEmpty { el.select("figcaption").text() }
            val href = el.select("a").attr("href")
            val poster = el.select("img").attr("data-src").ifEmpty { el.select("img").attr("src") }
            if (title.isEmpty() || href.isEmpty()) return@mapNotNull null
            newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    data class EpisodeData(
        @JsonProperty("s") val season: Int,
        @JsonProperty("episode_no") val episodeNo: Int,
        @JsonProperty("title") val title: String,
        @JsonProperty("slug") val slug: String
    )

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1").first()?.text() ?: ""
        val poster = doc.select("img[itemprop=image]").attr("data-src")
            .ifEmpty { doc.select("img[itemprop=image]").attr("src") }
        val desc = doc.select("div.synopsis").text()

        // Cek season-data JSON
        val seasonDataRaw = doc.select("script#season-data").html()

        return if (seasonDataRaw.isNotEmpty()) {
            // Ini series — parse episode dari JSON
            val episodes = mutableListOf<Episode>()
            try {
                val seasonMap = AppUtils.parseJson<Map<String, List<EpisodeData>>>(seasonDataRaw)
                seasonMap.forEach { (_, epList) ->
                    epList.forEach { ep ->
                        episodes.add(newEpisode("$mainUrl/${ep.slug}") {
                            name = ep.title
                            season = ep.season
                            episode = ep.episodeNo
                        })
                    }
                }
            } catch (e: Exception) { }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = desc
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = desc
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // Ambil semua player dari player-list
        doc.select("ul#player-list li a").forEach { player ->
            val playerUrl = player.attr("data-url").ifEmpty { player.attr("href") }
            if (playerUrl.isNotEmpty()) {
                // Fetch halaman player untuk dapat M3U8
                try {
                    val playerDoc = app.get(
                        playerUrl,
                        referer = mainUrl
                    ).document

                    // Cari M3U8 di source player
                    val m3u8 = Regex("(https?://[^\"']+\\.m3u8[^\"']*)").find(playerDoc.html())
                        ?.groupValues?.get(1)

                    if (m3u8 != null) {
                        callback(newExtractorLink(
                            name, name, m3u8,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = playerUrl
                            this.quality = 0
                        })
                    } else {
                        loadExtractor(playerUrl, mainUrl, subtitleCallback, callback)
                    }
                } catch (e: Exception) {
                    loadExtractor(playerUrl, mainUrl, subtitleCallback, callback)
                }
            }
        }
        return true
    }
}