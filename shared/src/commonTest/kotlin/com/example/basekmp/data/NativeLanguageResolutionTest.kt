package com.example.basekmp.data

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeLanguageResolutionTest {
    @Test
    fun canonicalNativeTakesPriorityOverOldSnapshotAndSystem() {
        assertEquals("fr", Languages.resolveNativeLanguage("fr", "vi", "en-US"))
        assertEquals("vi", Languages.resolveNativeLanguage("", "vi", "en-US"))
    }

    @Test
    fun freshInstallUsesSupportedDeviceLanguageOrEnglish() {
        assertEquals("pt", Languages.resolveNativeLanguage("", null, "pt-BR"))
        assertEquals("en", Languages.resolveNativeLanguage("", null, "sv-SE"))
        assertEquals("es", Languages.resolveNativeLanguage("invalid", "invalid", "es-ES"))
    }
}
