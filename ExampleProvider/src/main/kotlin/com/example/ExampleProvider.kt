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
        val items = doc.select("li.slider, li.listitem").map { el ->
            val title = el.select("figcaption").text()
            val href = el.select("a").attr("href")
            val poster = el.select("img").attr("src")
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("li.slider, li.listitem").map { el ->
            val title = el.select("figcaption").text()
            val href = el.select("a").attr("href")
            val poster = el.select("img").attr("src")
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1.entry-title, h1").first()?.text() ?: ""
        val poster = doc.select("div.poster img, figure img").attr("src")
        val desc = doc.select("div.entry-content p, .sinopsis").text()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = desc
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // Cari semua iframe player
        doc.select("iframe#main-player, iframe[src*=playeriframe], iframe[src*=iframe]").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
        return true
    }
}