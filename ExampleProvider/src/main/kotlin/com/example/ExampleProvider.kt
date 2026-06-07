package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

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
        val items = doc.select("article.item").map { el ->
            val title = el.select("h2, h3, .Title").text()
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
        return doc.select("article.item").map { el ->
            val title = el.select("h2, h3, .Title").text()
            val href = el.select("a").attr("href")
            val poster = el.select("img").attr("src")
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1, .Title").first()?.text() ?: ""
        val poster = doc.select("img.Poster, .Image img").attr("src")
        val desc = doc.select(".Description, .sinopsis").text()

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
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                loadExtractor(src, subtitleCallback, callback)
            }
        }
        return true
    }
}