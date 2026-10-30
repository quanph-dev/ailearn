package com.example.basekmp.data

data class PlanQuestion(
    val question: PathLessonContent.Question,
    val itemId: String,
    val model: String = "",
    val alternatives: List<String> = emptyList(),
    val content: String = ""
) {
    constructor(
        text: String,
        options: List<String>,
        correct: String,
        meaning: String = "",
        itemId: String = text.hashCode().toString()
    ) : this(
        question = PathLessonContent.Question(
            kind = PathLessonContent.Kind.VOCABULARY,
            text = text,
            options = options,
            answer = options.indexOf(correct).let { if (it >= 0) it else 0 },
            start = meaning
        ),
        itemId = itemId,
        content = text
    )

    val text: String get() = content.ifBlank { question.text }
    val options: List<String> get() = question.options
    val answer: Int get() = question.answer
    val correct: String get() = if (question.answer in question.options.indices) question.options[question.answer] else question.options.firstOrNull().orEmpty()
    val meaning: String get() = question.start
}
