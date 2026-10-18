package com.edu.unal.directorio.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.normalizeKey

/** Room database of the app, schema version 1 (design D3): `company` and the `classification` catalog. */
@Database(
    entities = [CompanyEntity::class, ClassificationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DirectorioDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao

    companion object {
        /** Name of the database file used by the app. */
        const val FILE_NAME = "directorio.db"

        /**
         * Builds the file-backed database named [name]. When the file is created, the callback
         * loads the classification catalog and then [SAMPLE_COMPANIES] (design D8).
         */
        fun create(context: Context, name: String = FILE_NAME): DirectorioDatabase =
            Room.databaseBuilder(context.applicationContext, DirectorioDatabase::class.java, name)
                .addCallback(CreationCallback(loadSamples = true))
                .setDriver(AndroidSQLiteDriver())
                .build()

        /**
         * Builds an empty in-memory database. Its creation callback loads only the classification
         * catalog (the foreign key of `company` needs it), never the sample companies.
         */
        fun createInMemory(context: Context): DirectorioDatabase =
            Room.inMemoryDatabaseBuilder(context, DirectorioDatabase::class.java)
                .addCallback(CreationCallback(loadSamples = false))
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}

/**
 * Creation callback: loads the classification catalog from the [Classification] enum in every
 * new database and, when [loadSamples] is set, the [SAMPLE_COMPANIES] (design D8).
 *
 * Transaction: checked on device (Room 3.0.3, AndroidSQLiteDriver). A probe in `onCreate` that ran
 * `BEGIN; ROLLBACK` also rolled back the tables created just before, so `onCreate` runs inside
 * the creation transaction, as design D8 assumes (a failure here aborts the creation). The load
 * is still wrapped in a SAVEPOINT, which nests inside that transaction at no cost and keeps the
 * load all-or-nothing should the driver ever call `onCreate` outside it.
 */
private class CreationCallback(private val loadSamples: Boolean) : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.execSQL("SAVEPOINT load_initial_data")
        try {
            Classification.entries.forEach { connection.insertClassification(it) }
            if (loadSamples) SAMPLE_COMPANIES.forEach { connection.insertCompany(it) }
            connection.execSQL("RELEASE load_initial_data")
        } catch (e: Throwable) {
            connection.execSQL("ROLLBACK TO load_initial_data")
            connection.execSQL("RELEASE load_initial_data")
            throw e
        }
    }
}

private fun SQLiteConnection.insertClassification(classification: Classification) {
    prepare("INSERT INTO classification (id, code) VALUES (?, ?)").use { st ->
        st.bindLong(1, classification.id.toLong())
        st.bindText(2, classification.code)
        st.step()
    }
}

private fun SQLiteConnection.insertCompany(draft: CompanyDraft) {
    val classification = requireNotNull(draft.classification) { "Sample ${draft.name} has no classification" }
    prepare(
        "INSERT INTO company (name, name_key, email, email_key, url, phone, products, classification_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
    ).use { st ->
        st.bindText(1, draft.name)
        st.bindText(2, normalizeKey(draft.name))
        st.bindText(3, draft.email)
        st.bindText(4, normalizeKey(draft.email))
        st.bindText(5, draft.url)
        st.bindText(6, draft.phone)
        st.bindText(7, draft.products)
        st.bindLong(8, classification.id.toLong())
        st.step()
    }
}
