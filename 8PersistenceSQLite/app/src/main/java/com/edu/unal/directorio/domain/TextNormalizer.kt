package com.edu.unal.directorio.domain

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{Mn}+")

/**
 * Comparison key: trimmed, accents removed (NFD, then dropping `\p{Mn}`) and lowercased with
 * [Locale.ROOT]. "Ñandú Ágil", "ñandú ágil" and "NANDU AGIL" produce the same key.
 */
fun normalizeKey(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)

/** Escapes `\`, `%` and `_` with a leading `\` so the text is literal in a `LIKE ... ESCAPE '\'`. */
fun escapeLike(text: String): String = buildString(text.length) {
    for (c in text) {
        if (c == '\\' || c == '%' || c == '_') append('\\')
        append(c)
    }
}
