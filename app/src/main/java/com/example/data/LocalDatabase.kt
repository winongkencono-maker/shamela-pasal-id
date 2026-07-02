package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val bookId: Int,
    val bookTitle: String,
    val bookAuthor: String,
    val globalOrder: Int,
    val pageNum: Int,
    val sectionTitle: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int,
    val bookTitle: String,
    val globalOrder: Int,
    val pageNum: Int,
    val sectionTitle: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int,
    val globalOrder: Int,
    val text: String,
    val color: String = "#FFEB3B", // Yellow by default
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val bookId: Int,
    val bookTitle: String,
    val bookAuthor: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pasal_favorites")
data class PasalFavorite(
    @PrimaryKey val frbrUri: String,
    val title: String,
    val number: String?,
    val year: Int?,
    val type: String?,
    val status: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ShamelaDao {
    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    fun getAllReadingProgress(): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getReadingProgressForBook(bookId: Int): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgress)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY globalOrder ASC")
    fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY globalOrder ASC")
    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<Favorite>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE bookId = :bookId)")
    fun isFavorite(bookId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun deleteFavorite(bookId: Int)

    // Pasal.id Local Operations
    @Query("SELECT * FROM pasal_favorites ORDER BY createdAt DESC")
    fun getAllPasalFavorites(): Flow<List<PasalFavorite>>

    @Query("SELECT EXISTS(SELECT 1 FROM pasal_favorites WHERE frbrUri = :frbrUri)")
    fun isPasalFavorite(frbrUri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasalFavorite(favorite: PasalFavorite)

    @Query("DELETE FROM pasal_favorites WHERE frbrUri = :frbrUri")
    suspend fun deletePasalFavorite(frbrUri: String)
}

@Database(
    entities = [ReadingProgress::class, Bookmark::class, Highlight::class, Favorite::class, PasalFavorite::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shamelaDao(): ShamelaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shamela_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
