package com.edu.unal.directorio.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.normalizeKey

/** Room database of the app, schema version 1 (design D3). */
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
         * loads [SAMPLE_COMPANIES] (design D8).
         */
        fun create(context: Context, name: String = FILE_NAME): DirectorioDatabase =
            Room.databaseBuilder(context.applicationContext, DirectorioDatabase::class.java, name)
                .addCallback(SampleDataCallback)
                .setDriver(AndroidSQLiteDriver())
                .build()

        /** Builds an empty in-memory database, without any creation callback. */
        fun createInMemory(context: Context): DirectorioDatabase =
            Room.inMemoryDatabaseBuilder(context, DirectorioDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}

/**
 * Loads [SAMPLE_COMPANIES] with parameterized SQL, only when the database file is created.
 *
 * Transaction: checked on device (Room 3.0.3, AndroidSQLiteDriver). A probe in `onCreate` that ran
 * `BEGIN; ROLLBACK` also rolled back the tables created just before, so `onCreate` runs inside
 * the creation transaction, as design D8 assumes (a failure here aborts the creation). The load
 * is still wrapped in a SAVEPOINT, which nests inside that transaction at no cost and keeps the
 * load all-or-nothing should the driver ever call `onCreate` outside it.
 */
private object SampleDataCallback : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.execSQL("SAVEPOINT load_samples")
        try {
            SAMPLE_COMPANIES.forEach { connection.insertCompany(it) }
            connection.execSQL("RELEASE load_samples")
        } catch (e: Throwable) {
            connection.execSQL("ROLLBACK TO load_samples")
            connection.execSQL("RELEASE load_samples")
            throw e
        }
    }
}

private fun SQLiteConnection.insertCompany(draft: CompanyDraft) {
    val id = prepare(
        "INSERT INTO company (name, name_key, email, email_key, url, phone, products) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
    ).use { st ->
        st.bindText(1, draft.name)
        st.bindText(2, normalizeKey(draft.name))
        st.bindText(3, draft.email)
        st.bindText(4, normalizeKey(draft.email))
        st.bindText(5, draft.url)
        st.bindText(6, draft.phone)
        st.bindText(7, draft.products)
        st.step()
        prepare("SELECT last_insert_rowid()").use { q ->
            q.step()
            q.getLong(0)
        }
    }
    draft.classifications.forEach { classification ->
        prepare("INSERT INTO company_classification (company_id, classification) VALUES (?, ?)").use { st ->
            st.bindLong(1, id)
            st.bindText(2, classification.code)
            st.step()
        }
    }
}
