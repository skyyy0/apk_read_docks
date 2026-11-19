package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.PdfAnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY isPinned DESC, lastOpenedAt DESC")
    fun observeAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC")
    suspend fun getAllDocumentsOnce(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentEntity): Long

    @Update
    suspend fun update(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE documents SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE documents SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE documents SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touchOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET title = :title, updatedAt = :timestamp WHERE id = :id")
    suspend fun rename(id: Long, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}

@Dao
interface PdfAnnotationDao {
    @Query("SELECT * FROM pdf_annotations WHERE documentId = :docId ORDER BY pageIndex ASC, createdAt ASC")
    fun observeForDocument(docId: Long): Flow<List<PdfAnnotationEntity>>

    @Query("SELECT * FROM pdf_annotations WHERE documentId = :docId ORDER BY pageIndex ASC, createdAt ASC")
    suspend fun getForDocumentOnce(docId: Long): List<PdfAnnotationEntity>

    @Query("SELECT * FROM pdf_annotations")
    suspend fun getAllOnce(): List<PdfAnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: PdfAnnotationEntity): Long

    @Query("DELETE FROM pdf_annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pdf_annotations WHERE documentId = :docId")
    suspend fun deleteForDocument(docId: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE documentId = :docId ORDER BY targetIndex ASC")
    fun observeForDocument(docId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentId = :docId")
    suspend fun deleteForDocument(docId: Long)
}

@Database(
    entities = [DocumentEntity::class, PdfAnnotationEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun annotationDao(): PdfAnnotationDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: LuminaDatabase? = null

        fun getInstance(context: Context): LuminaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LuminaDatabase::class.java,
                    "lumina_docs.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
