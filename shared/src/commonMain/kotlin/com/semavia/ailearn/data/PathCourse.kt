package com.semavia.ailearn.data

/**
 * Duolingo-style Structured Learning Path Course definition.
 * 100% migrated from original app's PathCourse.java.
 * Zero dummy data: 36 units and 60+ lessons across 3 proficiency levels.
 */
object PathCourse {
    enum class Status {
        DONE,
        CURRENT,
        LOCKED
    }

    data class Lesson(
        val id: String,
        val title: String,
        val level: String,
        val unitNumber: Int
    )

    data class PathUnit(
        val id: String,
        val level: String,
        val number: Int,
        val title: String,
        val lessons: List<Lesson>
    )

    data class LessonState(
        val lesson: Lesson,
        val status: Status,
        val score: Int?
    )

    data class UnitProgress(
        val done: Int,
        val total: Int
    ) {
        val percent: Int
            get() = if (total == 0) 0 else (done * 100) / total
    }

    val LEVELS = listOf("Beginner", "Intermediate", "Advanced")

    private val beginner: List<PathUnit> = listOf(
        unit("Beginner", 1, "Greetings & Introductions", "Hello", "Goodbye", "Name", "Age", "Country"),
        unit("Beginner", 2, "About Me", "Personal information", "Likes and dislikes"),
        unit("Beginner", 3, "Family & Friends", "Family members", "Relationships", "Describing people"),
        unit("Beginner", 4, "Numbers & Time", "Numbers", "Dates", "Days", "Telling time"),
        unit("Beginner", 5, "Daily Routine", "Wake up", "Work", "Study", "Sleep", "Daily activities"),
        unit("Beginner", 6, "Food & Drinks", "Common foods", "Preferences", "Ordering basics"),
        unit("Beginner", 7, "Shopping Basics", "Prices", "Sizes", "Colors", "Buying something"),
        unit("Beginner", 8, "Home & Neighborhood", "Rooms", "Objects", "Places near home"),
        unit("Beginner", 9, "Getting Around", "Basic directions", "Transportation"),
        unit("Beginner", 10, "Everyday Questions", "Who", "What", "Where", "When", "Why", "How"),
        unit("Beginner", 11, "Weather & Seasons", "Weather", "Seasons"),
        unit("Beginner", 12, "Beginner Conversations", "Combining everything in short real-life dialogues")
    )

    private val intermediate: List<PathUnit> = listOf(
        unit("Intermediate", 1, "Making Plans", "Invitations", "Accepting/declining", "Arranging times"),
        unit("Intermediate", 2, "Restaurants & Cafés", "Ordering", "Special requests", "Paying"),
        unit("Intermediate", 3, "Shopping & Returns", "Comparing products", "Refunds", "Exchanges"),
        unit("Intermediate", 4, "Travel & Airports", "Check-in", "Immigration", "Baggage", "Boarding"),
        unit("Intermediate", 5, "Hotels & Accommodation", "Reservations", "Requests", "Common problems"),
        unit("Intermediate", 6, "Work & Office", "Tasks", "Colleagues", "Schedules", "Workplace vocabulary"),
        unit("Intermediate", 7, "Phone Conversations", "Calling", "Taking messages", "Clarifying information"),
        unit("Intermediate", 8, "Health & Appointments", "Describing basic symptoms", "Making appointments"),
        unit("Intermediate", 9, "Problems & Solutions", "Explaining a problem and asking for help"),
        unit("Intermediate", 10, "Experiences & Stories", "Talking about past events and experiences"),
        unit("Intermediate", 11, "Opinions & Preferences", "Agreeing", "Disagreeing", "Explaining reasons"),
        unit("Intermediate", 12, "Social Conversations", "Longer, more natural everyday conversations")
    )

    private val advanced: List<PathUnit> = listOf(
        unit("Advanced", 1, "Natural Conversations", "Fluency", "Fillers", "Natural responses"),
        unit("Advanced", 2, "Idioms & Expressions", "Common expressions used by native speakers"),
        unit("Advanced", 3, "Advanced Storytelling", "Detailed and engaging stories"),
        unit("Advanced", 4, "Discussions & Opinions", "Developing and supporting an argument"),
        unit("Advanced", 5, "Meetings", "Leading", "Interrupting politely", "Contributing ideas"),
        unit("Advanced", 6, "Presentations", "Explaining ideas clearly and confidently"),
        unit("Advanced", 7, "Job Interviews", "Detailed professional answers and follow-ups"),
        unit("Advanced", 8, "Negotiation", "Proposals", "Compromises", "Persuasion"),
        unit("Advanced", 9, "Professional Communication", "Formal vs casual workplace language"),
        unit("Advanced", 10, "Difficult Conversations", "Complaints", "Disagreements", "Misunderstandings"),
        unit("Advanced", 11, "Humor & Informal English", "Casual expressions", "Jokes", "Conversational tone"),
        unit("Advanced", 12, "Real-World Fluency", "Unscripted, longer conversations across situations")
    )

    private val byLevel: Map<String, List<PathUnit>> = mapOf(
        "Beginner" to beginner,
        "Intermediate" to intermediate,
        "Advanced" to advanced
    )

    private fun unit(level: String, number: Int, title: String, vararg lessons: String): PathUnit {
        val prefix = "${level.lowercase()}-u$number"
        val list = lessons.mapIndexed { index, name ->
            Lesson(
                id = "$prefix-l${index + 1}",
                title = name,
                level = level,
                unitNumber = number
            )
        }
        return PathUnit(
            id = prefix,
            level = level,
            number = number,
            title = title,
            lessons = list
        )
    }

    fun levelEmoji(level: String): String = when (level) {
        "Beginner" -> "🌱"
        "Intermediate" -> "🌿"
        else -> "🏆"
    }

    fun units(level: String): List<PathUnit> = byLevel[level] ?: beginner

    fun lessons(level: String): List<Lesson> = units(level).flatMap { it.lessons }

    fun lesson(id: String): Lesson? = byLevel.values.asSequence().flatten().flatMap { it.lessons }.firstOrNull { it.id == id }

    fun unitOf(lesson: Lesson): PathUnit = units(lesson.level).first { it.number == lesson.unitNumber }

    fun levelForEnglishLevel(englishLevel: String): String = when {
        englishLevel.startsWith("C", ignoreCase = true) -> "Advanced"
        englishLevel.startsWith("B", ignoreCase = true) -> "Intermediate"
        else -> "Beginner"
    }

    fun placementLevel(prefs: UserPrefs): String = levelForEnglishLevel(prefs.englishLevel)

    fun startLevel(prefs: UserPrefs): String {
        val pathLevel = prefs.pathLevel
        if (pathLevel != null && LEVELS.contains(pathLevel)) {
            return pathLevel
        }
        return if (prefs.englishLevelChosen) placementLevel(prefs) else "Beginner"
    }

    fun isLevelOpen(level: String, prefs: UserPrefs): Boolean {
        val index = LEVELS.indexOf(level)
        val placementIdx = LEVELS.indexOf(placementLevel(prefs))
        if (index > 0 && index > placementIdx) {
            val prevLevel = LEVELS[index - 1]
            val doneMap = prefs.pathLessonsState.value
            val prevLessons = lessons(prevLevel)
            return prevLessons.all { doneMap.containsKey(it.id) }
        }
        return true
    }

    fun states(level: String, prefs: UserPrefs): List<LessonState> {
        val doneMap = prefs.pathLessonsState.value
        var foundCurrent = false
        val levelOpen = isLevelOpen(level, prefs)

        return lessons(level).map { lesson ->
            val isDone = doneMap.containsKey(lesson.id)
            val status = when {
                isDone -> Status.DONE
                levelOpen && !foundCurrent -> {
                    foundCurrent = true
                    Status.CURRENT
                }
                else -> Status.LOCKED
            }
            LessonState(
                lesson = lesson,
                status = status,
                score = doneMap[lesson.id]
            )
        }
    }

    fun unitProgress(unit: PathUnit, states: List<LessonState>): UnitProgress {
        val unitLessonIds = unit.lessons.map { it.id }.toSet()
        val unitStates = states.filter { unitLessonIds.contains(it.lesson.id) }
        val doneCount = unitStates.count { it.status == Status.DONE }
        return UnitProgress(done = doneCount, total = unit.lessons.size)
    }
}
