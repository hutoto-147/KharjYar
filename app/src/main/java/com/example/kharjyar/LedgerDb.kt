package com.example.kharjyar

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LedgerDb(context: Context) : SQLiteOpenHelper(
    context,
    "kharjyar.db",
    null,
    1
) {
    companion object {
        private const val TAG_SEP = "\u001F"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                category TEXT NOT NULL,
                subcategory TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '',
                note TEXT NOT NULL DEFAULT '',
                occurred_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE custom_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                subcategory TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE custom_tags (
                name TEXT PRIMARY KEY
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_entries_occurred_at ON entries(occurred_at)")
        db.execSQL("CREATE INDEX idx_entries_type ON entries(type)")
        db.execSQL("CREATE INDEX idx_entries_category ON entries(category)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getEntries(): List<LedgerEntry> {
        val result = mutableListOf<LedgerEntry>()
        readableDatabase.query(
            "entries",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val amountIdx = cursor.getColumnIndexOrThrow("amount")
            val categoryIdx = cursor.getColumnIndexOrThrow("category")
            val subIdx = cursor.getColumnIndexOrThrow("subcategory")
            val tagsIdx = cursor.getColumnIndexOrThrow("tags")
            val noteIdx = cursor.getColumnIndexOrThrow("note")
            val occurredIdx = cursor.getColumnIndexOrThrow("occurred_at")
            val createdIdx = cursor.getColumnIndexOrThrow("created_at")

            while (cursor.moveToNext()) {
                result += LedgerEntry(
                    id = cursor.getLong(idIdx),
                    type = EntryType.valueOf(cursor.getString(typeIdx)),
                    amount = cursor.getLong(amountIdx),
                    category = cursor.getString(categoryIdx),
                    subcategory = cursor.getString(subIdx),
                    tags = cursor.getString(tagsIdx)
                        .split(TAG_SEP)
                        .filter { it.isNotBlank() },
                    note = cursor.getString(noteIdx),
                    occurredAt = cursor.getLong(occurredIdx),
                    createdAt = cursor.getLong(createdIdx)
                )
            }
        }
        return result
    }

    fun saveEntry(entry: LedgerEntry): Long {
        val values = ContentValues().apply {
            put("type", entry.type.name)
            put("amount", entry.amount)
            put("category", entry.category)
            put("subcategory", entry.subcategory)
            put("tags", entry.tags.joinToString(TAG_SEP))
            put("note", entry.note)
            put("occurred_at", entry.occurredAt)
            put("created_at", entry.createdAt)
        }

        return if (entry.id == 0L) {
            writableDatabase.insertOrThrow("entries", null, values)
        } else {
            writableDatabase.update(
                "entries",
                values,
                "id = ?",
                arrayOf(entry.id.toString())
            )
            entry.id
        }
    }

    fun deleteEntry(id: Long) {
        writableDatabase.delete("entries", "id = ?", arrayOf(id.toString()))
    }

    fun addCustomCategory(type: EntryType, name: String, subcategory: String) {
        if (name.isBlank()) return

        val exists = readableDatabase.rawQuery(
            """
            SELECT id FROM custom_categories
            WHERE type = ? AND name = ? AND subcategory = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(type.name, name.trim(), subcategory.trim())
        ).use { it.moveToFirst() }

        if (exists) return

        val values = ContentValues().apply {
            put("type", type.name)
            put("name", name.trim())
            put("subcategory", subcategory.trim())
        }
        writableDatabase.insertOrThrow("custom_categories", null, values)
    }

    fun getCustomCategories(type: EntryType? = null): List<CategoryRow> {
        val rows = mutableListOf<CategoryRow>()
        val selection = if (type == null) null else "type = ?"
        val args = if (type == null) null else arrayOf(type.name)

        readableDatabase.query(
            "custom_categories",
            null,
            selection,
            args,
            null,
            null,
            "type, name, subcategory"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val subIdx = cursor.getColumnIndexOrThrow("subcategory")
            while (cursor.moveToNext()) {
                rows += CategoryRow(
                    id = cursor.getLong(idIdx),
                    type = EntryType.valueOf(cursor.getString(typeIdx)),
                    name = cursor.getString(nameIdx),
                    subcategory = cursor.getString(subIdx)
                )
            }
        }
        return rows
    }

    fun addCustomTag(tag: String) {
        val cleaned = tag.trim().removePrefix("#")
        if (cleaned.isBlank()) return
        val values = ContentValues().apply {
            put("name", cleaned)
        }
        writableDatabase.insertWithOnConflict(
            "custom_tags",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun getCustomTags(): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.query(
            "custom_tags",
            arrayOf("name"),
            null,
            null,
            null,
            null,
            "name"
        ).use { cursor ->
            val idx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                result += cursor.getString(idx)
            }
        }
        return result
    }

    fun getBudget(): Long {
        return readableDatabase.rawQuery(
            "SELECT value FROM settings WHERE key = 'monthly_budget' LIMIT 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).toLongOrNull() ?: 0L else 0L
        }
    }

    fun setBudget(amount: Long) {
        val values = ContentValues().apply {
            put("key", "monthly_budget")
            put("value", amount.toString())
        }
        writableDatabase.insertWithOnConflict(
            "settings",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}

class LedgerRepository(context: Context) {
    private val db = LedgerDb(context.applicationContext)

    fun entries(): List<LedgerEntry> = db.getEntries()
    fun save(entry: LedgerEntry): Long = db.saveEntry(entry)
    fun delete(id: Long) = db.deleteEntry(id)

    fun customCategories(type: EntryType? = null): List<CategoryRow> =
        db.getCustomCategories(type)

    fun addCategory(type: EntryType, name: String, subcategory: String) =
        db.addCustomCategory(type, name, subcategory)

    fun customTags(): List<String> = db.getCustomTags()
    fun addTag(tag: String) = db.addCustomTag(tag)

    fun budget(): Long = db.getBudget()
    fun setBudget(amount: Long) = db.setBudget(amount)
}
