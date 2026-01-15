# DOCX → PDF with Background Image (OSS, In-Process)

## Problem
- DOCX templates often use an **image behind text** (certificate background).
- `docx4j` OSS converts DOCX → PDF via **XSL-FO + Apache FOP**.
- FOP **cannot correctly render Word floating/behind-text images**, causing:
    - image moved to next page
    - text hidden
    - extra pages
    - white margins

This is a known limitation and not reliably fixable inside docx4j OSS.

---

## Solution (Architecture)

Instead of forcing Word semantics into PDF:

1. **Fill DOCX placeholders** (Apache POI)
2. **Extract background image from DOCX ZIP** (`word/media/image1.*`)
3. **Convert DOCX → PDF** (docx4j)
4. **Stamp background image into PDF** *behind content* (PDFBox, `AppendMode.PREPEND`)
5. **Remove extra pages**, keep only page 1

This separates concerns correctly:
- Word → text layout
- PDF → rendering & layering

---

## Why This Works
- PDF drawing order defines z-order → background is always behind text
- No Word anchors, wrapping, or z-index involved
- Uses the template’s own embedded image (single source of truth)
- Fully OSS, in-process, no LibreOffice, no server
- Deterministic output (exactly one page)

---

## Result
- Correct background coverage
- No white margins (via bleed pixels)
- No extra pages
- Stable across platforms (Compose Multiplatform JVM)

```kotlin
actual fun fillTemplateToPdf(
    templateBytes: ByteArray,
    outputPath: String,
    replacements: Map<String, String>,
) {
// 1) Fill DOCX placeholders
    val docxBytes = buildDocxBytes(templateBytes, replacements)

    // 2) Extract first embedded image (word/media/image1.* preferred)
    val bg = extractFirstImageFromDocx(docxBytes)

    // 3) DOCX -> PDF (docx4j)
    val wordPackage = WordprocessingMLPackage.load(ByteArrayInputStream(docxBytes))
    val pdfBytes = ByteArrayOutputStream().use { baos ->
        Docx4J.toPDF(wordPackage, baos)
        baos.toByteArray()
    }

    // 4) Stamp background behind content (PDFBox)
    val stampedPdfBytes = if (bg != null) {
        addBackgroundToPdfBytes(
            pdfBytes = pdfBytes,
            backgroundImageBytes = bg.bytes,
            imageExtLower = bg.extLower,
            mode = BackgroundMode.COVER,
            bleedPx = 2f
        )
    } else pdfBytes

    // 5) Keep only the first page
    val finalPdfBytes = keepOnlyFirstPage(stampedPdfBytes)

    // 6) Write output
    FileOutputStream(outputPath).use { it.write(finalPdfBytes) }
}
```
