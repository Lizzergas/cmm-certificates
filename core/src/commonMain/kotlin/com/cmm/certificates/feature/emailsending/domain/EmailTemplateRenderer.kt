package com.cmm.certificates.feature.emailsending.domain

private const val FeedbackUrlPlaceholder = "{{feedback_url}}"

data class EmailTemplateVariables(
    val feedbackUrl: String = "",
)

fun renderEmailTemplate(
    text: String,
    variables: EmailTemplateVariables,
): String {
    return text.replace(FeedbackUrlPlaceholder, variables.feedbackUrl.trim())
}
