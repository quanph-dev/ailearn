package com.semavia.ailearn

import com.semavia.ailearn.config.BrandConfig

/** External links owned by the app. */
object AppLinks {
    const val ANDROID_PACKAGE_NAME = BrandConfig.ANDROID_APPLICATION_ID
    const val PRIVACY_POLICY_URL = "https://greenpinellc.store/privacy-green.html"
    const val TERMS_OF_SERVICE_URL = "https://greenpinellc.store/privacy-green.html"
    const val SUPPORT_EMAIL = "wyattphillip5507913@gmail.com"

    fun playStoreUrl(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun shareMessage(packageName: String): String = buildList {
        add("Try ${BrandConfig.STORE_TITLE}")
        add(playStoreUrl(packageName))
        if (PRIVACY_POLICY_URL.isNotBlank()) add("Privacy: $PRIVACY_POLICY_URL")
        if (TERMS_OF_SERVICE_URL.isNotBlank()) add("Terms: $TERMS_OF_SERVICE_URL")
    }.joinToString("\n\n")

    fun documentUrl(kind: String): String = when (kind.lowercase()) {
        "privacy" -> PRIVACY_POLICY_URL
        "terms", "term" -> TERMS_OF_SERVICE_URL
        else -> ""
    }

    fun supportEmailUrl(): String? = SUPPORT_EMAIL
        .takeIf(String::isNotBlank)
        ?.let { "mailto:$it" }
}
