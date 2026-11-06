package com.semavia.ailearn.data

import kotlinx.serialization.Serializable

@Serializable
data class VocabItem(
    val id: String,
    val meaning: Map<String, String> = emptyMap(),
    val options: List<String> = emptyList(),
    val answer: Int = 0,
    val question: Map<String, String> = emptyMap()
)

@Serializable
data class TranslationItem(
    val id: String,
    val source: Map<String, String> = emptyMap(),
    val options: List<String> = emptyList(),
    val answer: Int = 0
)

@Serializable
data class SpeakingItem(
    val id: String,
    val task: Map<String, String> = emptyMap(),
    val say: String = "",
    val start: String = "",
    val alt: List<String> = emptyList()
)

@Serializable
data class LevelBank(
    val vocab: List<VocabItem> = emptyList(),
    val translation: List<TranslationItem> = emptyList(),
    val speaking: List<SpeakingItem> = emptyList()
)

@Serializable
data class PlanBank(
    val stage: String,
    val learn: String,
    val reviewed: Boolean = false,
    val levels: Map<String, LevelBank> = emptyMap()
)
