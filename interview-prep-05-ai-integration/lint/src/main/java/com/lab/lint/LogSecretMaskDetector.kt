package com.lab.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import java.util.EnumSet

class LogSecretMaskDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "LogSecretMaskCheck",
            briefDescription = "Secrets passed to Log must be wrapped in SecretMask",
            explanation = "Any logging statement that references an API key, apiKey, authorization, token, or secret must wrap it in SecretMask to prevent accidental leakage in logcat.",
            category = Category.SECURITY,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                LogSecretMaskDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("d", "e", "i", "v", "w")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        if (!evaluator.isMemberInClass(method, "android.util.Log")) return

        for (arg in node.valueArguments) {
            if (containsUnmaskedKey(arg)) {
                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "Secrets passed to Log must be wrapped in SecretMask"
                )
            }
        }
    }

    private fun containsUnmaskedKey(expression: UExpression): Boolean {
        val text = expression.asSourceString()
        val lowerText = text.lowercase()

        // Check if the argument expression references a sensitive key or credential term
        val referencesKey = lowerText.contains("api_key") ||
                lowerText.contains("apikey") ||
                lowerText.contains("authorization") ||
                lowerText.contains("secret")

        if (!referencesKey) return false

        // Check if it is already wrapped in SecretMask
        val isWrapped = text.contains("SecretMask")
        return !isWrapped
    }
}