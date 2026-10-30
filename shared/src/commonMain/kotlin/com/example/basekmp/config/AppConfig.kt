package com.example.basekmp.config

/**
 * Global App Configuration for AILearn KMP.
 *
 * Designed to decouple the app from legacy hardcoded package names
 * and provide easy re-pointing for CDN, LLM endpoints, and testing bypass flags.
 */
object AppConfig {
    /**
     * Sample configurable Package Name / App ID placeholder.
     * Prevents any hardcoded dependency on legacy packages.
     */
    const val SAMPLE_APP_PACKAGE_NAME: String = "com.example.basekmp"

    /**
     * App Branding & Version
     */
    const val APP_NAME: String = "AILearn"
    const val APP_VERSION_NAME: String = "1.0.0"
    const val APP_VERSION_CODE: Int = 1

    /**
     * LLM backend endpoint. The legacy client key/header are retained for proxy compatibility.
     */
    const val LLM_API_URL: String = "https://us-central1-llmchat-22d7a.cloudfunctions.net/generate"
    const val LLM_API_KEY_HEADER: String = "QtcY6-KEY"
    const val LLM_API_KEY_VALUE: String = "bOBHq59mhSJ7aPK5Ut4k"

    /**
     * Dynamic CDN Endpoints (Firebase Storage Bucket)
     * Can be replaced anytime with new custom CDN / Cloudflare R2 / S3 URLs.
     */
    var cdnBucketUrl: String = "https://firebasestorage.googleapis.com/v0/b/apl567-tutor-ai-language.firebasestorage.app/o/"
    var shadowingCatalogUrl: String = "${cdnBucketUrl}shadowing%2Fcatalog.json?alt=media"
    var planCatalogUrl: String = "${cdnBucketUrl}plan%2Fplan.json?alt=media"
    var lessonContentManifestUrl: String = "${cdnBucketUrl}plan%2Fcontent%2Fmanifest.json?alt=media"

    /**
     * Release behavior. Premium and quotas are granted only by the billing/backend layers.
     */
    const val BYPASS_ADS: Boolean = false
    const val BYPASS_PAYWALL: Boolean = false
    const val IS_PRO_DEFAULT: Boolean = false
    const val BYPASS_GENERATION_QUOTA: Boolean = false
    const val UNLIMITED_AI_SLOTS: Int = 0
}
