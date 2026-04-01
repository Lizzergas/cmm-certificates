package com.cmm.certificates.feature.emailsending.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class EmailBodyBuilderTest {

    @Test
    fun convertsUrlsInBodyToHyperlinks() {
        val result = buildEmailHtmlBody(
            body = "Apmoketi galima cia: https://example.com/pay.",
            signatureHtml = "<div>Pagarbiai</div>",
        )

        assertEquals(
            "Apmoketi galima cia: <a href=\"https://example.com/pay\">https://example.com/pay</a>.<br><br><div>Pagarbiai</div>",
            result,
        )
    }

    @Test
    fun escapesHtmlOutsideUrls() {
        val result = buildEmailHtmlBody(
            body = "<b>Tekstas</b> https://example.com?a=1&b=2",
            signatureHtml = "<div>Parašas</div>",
        )

        assertEquals(
            "&lt;b&gt;Tekstas&lt;/b&gt; <a href=\"https://example.com?a=1&amp;b=2\">https://example.com?a=1&amp;b=2</a><br><br><div>Parašas</div>",
            result,
        )
    }

    @Test
    fun returnsHyperlinkedHtmlEvenWithoutSignature() {
        val result = buildEmailHtmlBody(
            body = "Nuoroda: https://example.com/pay",
            signatureHtml = "   ",
        )

        assertEquals(
            "Nuoroda: <a href=\"https://example.com/pay\">https://example.com/pay</a>",
            result,
        )
    }

    @Test
    fun rendersFeedbackUrlPlaceholderUsingRuntimeVariables() {
        val result = renderEmailTemplate(
            text = "Atsiliepimas: {{feedback_url}}",
            variables = EmailTemplateVariables(feedbackUrl = "https://example.com/form"),
        )

        assertEquals("Atsiliepimas: https://example.com/form", result)
    }

    @Test
    fun rendersBlankForMissingFeedbackUrl() {
        val result = renderEmailTemplate(
            text = "Atsiliepimas: {{feedback_url}}",
            variables = EmailTemplateVariables(),
        )

        assertEquals("Atsiliepimas: ", result)
    }
}
