package com.example.basekmp.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentTitleLanguageTest {
    @Test
    fun catalogOnlyUsesBundledTitlesForMatchingVideoAndSourceTitle() {
        val local = CatalogEntry(id = "return", title = "Returning an Item", level = "B1", category = "daily",
            titles = mapOf("vi" to "Trả hàng", "ja" to "返品"))
        val remote = local.copy(titles = mapOf("vi" to "", "ja" to "商品の返品"))
        val changed = remote.copy(title = "Different scene")

        val result = ShadowingVideos.supplementCatalogTitles(listOf(remote, changed), listOf(local))
        assertEquals("Trả hàng", result[0].titles["vi"])
        assertEquals("商品の返品", result[0].titles["ja"])
        assertEquals("", result[1].titles["vi"])
    }

    @Test
    fun videoAndSavedWordTitlesFollowInterfaceInsteadOfNativeOrLearnLanguage() {
        val oldUi = Strings.getLanguage()
        val oldNative = UserPrefs.nativeLanguage
        val oldLearn = UserPrefs.targetLanguage
        try {
            UserPrefs.nativeLanguage = "ja"
            UserPrefs.targetLanguage = "ja"
            val titles = mapOf("vi" to "Trả hàng", "ja" to "商品の返品", "ko" to "상품 반품")
            val video = ShadowingVideos.Video(
                id = "returning_an_item", title = "Returning an Item", level = "B1", category = "daily",
                scene = "", colors = 0L to 0L, asset = "", thumbMs = 0,
                speakers = emptyList(), lines = emptyList(), words = emptyList(),
                remote = CatalogEntry(id = "returning_an_item", title = "Returning an Item", titles = titles,
                    level = "B1", category = "daily")
            )
            Strings.select("vi")
            assertEquals("Trả hàng", ShadowingVideos.title(video))
            assertEquals("Trả hàng", ShadowingVideos.localizedTitle(video.title, titles))
            UserPrefs.nativeLanguage = "ko"
            assertEquals("vi", Strings.getLanguage())
            assertEquals("Trả hàng", ShadowingVideos.title(video))
            UserPrefs.nativeLanguage = "ja"
            val line = ShadowingVideos.Line(
                speaker = 0, text = "Hello", translations = mapOf("ja" to "こんにちは", "vi" to "Xin chào"),
                startMs = 0, endMs = 1000
            )
            assertEquals("こんにちは", line.textIn(UserPrefs.targetLanguage))
            assertEquals("Xin chào", line.translationFor("vi", UserPrefs.targetLanguage))
            Strings.select("ko")
            assertEquals("상품 반품", ShadowingVideos.title(video))
            assertEquals("상품 반품", ShadowingVideos.localizedTitle(video.title, titles))
            assertEquals("Returning an Item", ShadowingVideos.localizedTitle(video.title, mapOf("ja" to "商品の返品")))
            assertEquals("Unknown scene", ShadowingVideos.localizedTitle("Unknown scene", mapOf("ja" to "未知")))
        } finally {
            Strings.select(oldUi)
            UserPrefs.nativeLanguage = oldNative
            UserPrefs.targetLanguage = oldLearn
        }
    }

    @Test
    fun planTitlePrefersInterfaceTranslationBeforeEnglishFallback() {
        val oldUi = Strings.getLanguage()
        try {
            Strings.select("vi")
            val stage = PlanStage(
                id = "greetings", title = "Greetings", titles = mapOf("vi" to "Chào hỏi"),
                learnTitles = mapOf("ja" to mapOf("en" to "Japanese greetings"))
            )
            assertEquals("Chào hỏi", stage.displayTitle("ja"))
            assertEquals("vi", stage.displayTitleLanguage("ja"))
            Strings.select("ko")
            assertEquals("Japanese greetings", stage.displayTitle("ja"))
            assertEquals("en", stage.displayTitleLanguage("ja"))
        } finally {
            Strings.select(oldUi)
        }
    }
}
