package com.edu.unal.directorio.data.db

import androidx.room3.Embedded
import androidx.room3.Relation

/** A company row together with its classification rows, loaded in one transactional read. */
data class CompanyWithClassifications(
    @Embedded val company: CompanyEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["company_id"])
    val classifications: List<ClassificationEntity>,
)
