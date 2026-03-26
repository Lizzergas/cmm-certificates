package com.cmm.certificates.feature.settings.data

import certificates.composeapp.generated.resources.Res
import certificates.composeapp.generated.resources.conversion_default_lector_label
import certificates.composeapp.generated.resources.settings_default_accredited_type_options
import certificates.composeapp.generated.resources.settings_default_email_body
import certificates.composeapp.generated.resources.settings_default_email_subject
import certificates.composeapp.generated.resources.settings_default_signature_html
import com.cmm.certificates.core.i18n.localizedString
import com.cmm.certificates.feature.settings.domain.CertificateSettingsState
import com.cmm.certificates.feature.settings.domain.EmailTemplateSettingsState
import com.cmm.certificates.feature.settings.domain.SettingsState
import com.cmm.certificates.feature.settings.domain.SmtpSettingsState

const val DEFAULT_DAILY_LIMIT = 450
const val DEFAULT_PREVIEW_EMAIL = ""

fun defaultEmailSubject(): String = localizedString(Res.string.settings_default_email_subject)

fun defaultEmailBody(): String = localizedString(Res.string.settings_default_email_body)

fun defaultAccreditedTypeOptions(): String = localizedString(Res.string.settings_default_accredited_type_options)

fun defaultSignatureHtml(): String = localizedString(Res.string.settings_default_signature_html)

fun defaultLectorLabel(): String = localizedString(Res.string.conversion_default_lector_label)

fun defaultSettingsState(): SettingsState {
    return SettingsState(
        smtp = SmtpSettingsState(),
        email = EmailTemplateSettingsState(
            subject = defaultEmailSubject(),
            body = defaultEmailBody(),
            signatureHtml = defaultSignatureHtml(),
            previewEmail = DEFAULT_PREVIEW_EMAIL,
            dailyLimit = DEFAULT_DAILY_LIMIT,
        ),
        certificate = CertificateSettingsState(
            accreditedTypeOptions = defaultAccreditedTypeOptions(),
        ),
    )
}
