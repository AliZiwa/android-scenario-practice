package com.lab.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class JobNotesIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(LogSecretMaskDetector.ISSUE)

    override val api: Int
        get() = CURRENT_API

    override val vendor: Vendor
        get() = Vendor(
            vendorName = "Lab",
            identifier = "com.lab",
            feedbackUrl = "https://github.com/aliziwa/android-scenario-practice/issues",
        )
}