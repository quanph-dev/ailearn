package com.example.basekmp.data

/**
 * Standard English CEFR levels and display naming helper.
 * Synchronizes Onboarding, Settings, Practice screens (Speaking, Vocab, Grammar),
 * and 30-Day Plan models with the canonical original app representation.
 */
object EnglishLevel {
    const val A1 = "A1 Beginner"
    const val A2 = "A2 Elementary"
    const val B1 = "B1 Intermediate"
    const val B2 = "B2 Upper Intermediate"
    const val C1 = "C1 Advanced"
    const val NOT_SURE = "Not Sure"

    const val DEFAULT = B1

    /** All 6 settings options matching decompiled SettingsScreenKt and OnboardingScreenKt */
    val ALL_SETTINGS: List<String> = listOf(A1, A2, B1, B2, C1, NOT_SURE)

    /** 3-tier difficulty levels matching decompiled Practice screens */
    val THREE_TIERS: List<String> = listOf("Beginner", "Intermediate", "Advanced")

    /**
     * Converts any raw string (e.g. "a1", "A1", "Beginner", "A1 Beginner")
     * to its canonical full name (e.g. "A1 Beginner").
     */
    fun toFullName(raw: String?): String {
        val s = raw?.trim() ?: return DEFAULT
        return when {
            s.startsWith("A1", ignoreCase = true) || s.equals("Beginner", ignoreCase = true) || s.equals("a1", ignoreCase = true) -> A1
            s.startsWith("A2", ignoreCase = true) || s.equals("Elementary", ignoreCase = true) || s.equals("a2", ignoreCase = true) -> A2
            s.startsWith("B2", ignoreCase = true) || s.equals("Upper Intermediate", ignoreCase = true) || s.equals("b2", ignoreCase = true) -> B2
            s.startsWith("B1", ignoreCase = true) || s.equals("b1", ignoreCase = true) -> B1
            s.startsWith("C", ignoreCase = true) || s.equals("Advanced", ignoreCase = true) || s.equals("c1", ignoreCase = true) -> C1
            s.equals("unsure", ignoreCase = true) || s.equals("Not Sure", ignoreCase = true) -> NOT_SURE
            s.equals("Intermediate", ignoreCase = true) -> B1
            else -> DEFAULT
        }
    }

    /**
     * Converts any level format to the 3-tier difficulty ("Beginner", "Intermediate", "Advanced")
     * used by Speaking, Vocab, and Grammar practice setup dropdowns.
     */
    fun to3TierDifficulty(raw: String?): String {
        val s = raw?.trim() ?: return "Intermediate"
        return when {
            s.startsWith("A", ignoreCase = true) || s.contains("Begin", ignoreCase = true) || s.contains("Elem", ignoreCase = true) -> "Beginner"
            s.startsWith("C", ignoreCase = true) || s.contains("Advan", ignoreCase = true) -> "Advanced"
            else -> "Intermediate" // B1, B2, Not Sure, Intermediate
        }
    }

    /**
     * Extracts the short CEFR code (A1, A2, B1, B2, C1).
     */
    fun toCefrCode(raw: String?): String {
        val s = raw?.trim() ?: return "B1"
        return when {
            s.startsWith("A1", ignoreCase = true) || s.equals("a1", ignoreCase = true) -> "A1"
            s.startsWith("A2", ignoreCase = true) || s.equals("a2", ignoreCase = true) -> "A2"
            s.startsWith("B1", ignoreCase = true) || s.equals("b1", ignoreCase = true) -> "B1"
            s.startsWith("B2", ignoreCase = true) || s.equals("b2", ignoreCase = true) -> "B2"
            s.startsWith("C", ignoreCase = true) || s.equals("c1", ignoreCase = true) -> "C1"
            s.equals("unsure", ignoreCase = true) || s.equals("Not Sure", ignoreCase = true) -> "?"
            else -> "B1"
        }
    }
}
