package com.example.basekmp.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportIssueValidationTest {
    @Test
    fun onlyOtherRequiresNonBlankDescription() {
        assertTrue(canSubmitReport("Technical Problem", ""))
        assertFalse(canSubmitReport("Other", ""))
        assertFalse(canSubmitReport("Other", " \n "))
        assertTrue(canSubmitReport("Other", "Audio stopped"))
    }
}
