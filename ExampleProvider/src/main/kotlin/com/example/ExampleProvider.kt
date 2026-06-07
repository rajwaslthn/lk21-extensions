package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val doc = app.get(request.data).document
        val items = doc.select("li.slider, li.listitem").mapNotNull { el ->
            val title = el.select("h3.poster-title").text().ifEmpty { el.select("figcaption").text() }
            val href = el.select("a").attr("href")
            val poster = el.select("img[itemprop=image]").attr("src")
                .ifEmpty { el.select("img").attr("src") }
            if (title.isEmpty() || href.isEmpty()) return@mapNotNull null
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("li.slider, li.listitem").mapNotNull { el ->
            val title = el.select("h3.poster-title").text().ifEmpty { el.select("figcaption").text() }
            val href = el.select("a").attr("href")
            val poster = el.select("img[itemprop=image]").attr("src")
                .ifEmpty { el.select("img").attr("src") }
            if (title.isEmpty() || href.isEmpty()) return@mapNotNull null
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1.Nonton, div.movie-info h1").text()
        val poster = doc.select("img[itemprop=image]").attr("src")
        val desc = doc.select("div.synopsis p").text()
        val isSeries = doc.select("ul.episode-list li").isNotEmpty()

        return if (isSeries) {
            val episodes = doc.select("ul.episode-list li a").mapNotNull { ep ->
                val epHref = ep.attr("href")
                val epName = ep.text()
                if (epHref.isEmpty()) return@mapNotNull null
                val epNum = Regex("(\\d+)").find(epName)?.value?.toIntOrNull()
                newEpisode(epHref) {
                    name = epName
                    episode = epNum
                }
            }.reversed()
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
        doc.select("iframe#main-player, iframe[src*=playeriframe], iframe[src*=iframe]").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
        return true
    }
}