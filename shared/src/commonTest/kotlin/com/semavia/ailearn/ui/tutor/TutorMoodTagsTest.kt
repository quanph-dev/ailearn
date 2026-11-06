package com.semavia.ailearn.ui.tutor

import com.semavia.ailearn.components.RobotMood
import kotlin.test.Test
import kotlin.test.assertEquals

class TutorMoodTagsTest {
    @Test
    fun originalTagsAndObservedPrefixesSelectTheSameMood() {
        val moods = mapOf(
            "great" to RobotMood.HAPPY,
            "ok" to RobotMood.NEUTRAL,
            "oops" to RobotMood.SAD,
            "hi" to RobotMood.HAPPY
        )
        for ((tag, mood) in moods) {
            for (prefix in listOf("", "B", "TB", "TD")) {
                assertEquals(mood to "Hello!", TutorAi.splitMood("[$prefix$tag] Hello!"))
            }
        }
    }

    @Test
    fun handlesCaseWhitespaceAndRepeatedControlTags() {
        assertEquals(
            RobotMood.HAPPY to "Bạn nói rất tốt!",
            TutorAi.splitMood("  [ TB GREAT ] [Bhi] Bạn nói rất tốt! [oops]")
        )
    }

    @Test
    fun removesPrefixedControlTagsAppearingLaterInAReply() {
        assertEquals(
            RobotMood.HAPPY to "Try [ok] now.",
            TutorAi.splitMood("Try [ok] [TBgreat]now.")
        )
    }

    @Test
    fun leavesUnknownTagsAndOrdinaryTextAlone() {
        val text = "Use [subject] and [verb] in the sentence."
        assertEquals(null to text, TutorAi.splitMood(text))
        assertEquals(null to "[TBunknown] Hello", TutorAi.splitMood("[TBunknown] Hello"))
    }
}
