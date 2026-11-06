package com.semavia.ailearn.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyTranslationsTest {
    @Test
    fun matchesTranslationsByIdInsteadOfResponseOrder() {
        val response = """```json
            [{"id":1,"meaning":"thức dậy","translation":"Tôi thức dậy lúc sáu giờ."},
             {"id":0,"meaning":"nước","translation":"Tôi uống nước mỗi sáng."}]
            ```"""

        val result = VocabularyTranslations.parse(response, 2)
        assertEquals("nước", result[0]?.meaning)
        assertEquals("Tôi thức dậy lúc sáu giờ.", result[1]?.translation)
    }

    @Test
    fun rejectsMissingOutOfRangeAndAmbiguousTranslations() {
        val response = """[
            {"id":0,"meaning":"nước","translation":""},
            {"id":1,"meaning":"nhà","translation":"Tôi ở nhà."},
            {"id":1,"meaning":"ngôi nhà","translation":"Nhà của tôi."},
            {"id":8,"meaning":"khác","translation":"Khác."}
        ]"""
        assertTrue(VocabularyTranslations.parse(response, 2).isEmpty())
        assertTrue(VocabularyTranslations.parse("not JSON", 2).isEmpty())
    }
}
