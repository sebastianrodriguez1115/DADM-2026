package com.edu.unal.directorio.ui

import androidx.annotation.StringRes
import com.edu.unal.directorio.R
import com.edu.unal.directorio.domain.Classification

/** The single place that maps a [Classification] to its Spanish label resource. */
@StringRes
fun Classification.labelRes(): Int = when (this) {
    Classification.Consulting -> R.string.classification_consulting
    Classification.CustomDevelopment -> R.string.classification_custom_development
    Classification.SoftwareFactory -> R.string.classification_software_factory
}
