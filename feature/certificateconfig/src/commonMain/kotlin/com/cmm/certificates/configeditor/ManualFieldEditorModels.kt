package com.cmm.certificates.configeditor

import com.cmm.certificates.domain.config.CertificateFieldType
import com.cmm.certificates.domain.config.ManualTagField

data class ManualTagFieldDraft(
    val tag: String = "",
    val label: String = "",
    val type: CertificateFieldType = CertificateFieldType.TEXT,
    val defaultValue: String = "",
    val optionsText: String = "",
)

fun ManualTagField.toDraft(): ManualTagFieldDraft {
    return ManualTagFieldDraft(
        tag = tag,
        label = label.orEmpty(),
        type = type,
        defaultValue = defaultValue.orEmpty(),
        optionsText = options.joinToString("\n"),
    )
}

fun ManualTagFieldDraft.toField(): ManualTagField {
    return ManualTagField(
        tag = tag.trim(),
        label = label.trim().ifBlank { null },
        type = type,
        defaultValue = defaultValue.trim().ifBlank { null },
        options = optionsText.lineSequence().map(String::trim).filter(String::isNotBlank).toList(),
    )
}

fun <T> List<T>.updateItem(index: Int, update: (T) -> T): List<T> {
    return mapIndexed { currentIndex, item -> if (currentIndex == index) update(item) else item }
}

fun <T> List<T>.removeItem(index: Int): List<T> {
    return filterIndexed { currentIndex, _ -> currentIndex != index }
}
