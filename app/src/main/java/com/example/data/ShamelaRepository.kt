package com.example.data

import kotlinx.coroutines.flow.Flow

class ShamelaRepository(private val shamelaDao: ShamelaDao) {

    // Reading Progress
    val allReadingProgress: Flow<List<ReadingProgress>> = shamelaDao.getAllReadingProgress()

    suspend fun saveReadingProgress(progress: ReadingProgress) {
        shamelaDao.saveReadingProgress(progress)
    }

    suspend fun getReadingProgressForBook(bookId: Int): ReadingProgress? {
        return shamelaDao.getReadingProgressForBook(bookId)
    }

    // Bookmarks
    val allBookmarks: Flow<List<Bookmark>> = shamelaDao.getAllBookmarks()

    fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>> {
        return shamelaDao.getBookmarksForBook(bookId)
    }

    suspend fun insertBookmark(bookmark: Bookmark) {
        shamelaDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(id: Long) {
        shamelaDao.deleteBookmark(id)
    }

    // Highlights
    val allHighlights: Flow<List<Highlight>> = shamelaDao.getAllHighlights()

    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>> {
        return shamelaDao.getHighlightsForBook(bookId)
    }

    suspend fun insertHighlight(highlight: Highlight) {
        shamelaDao.insertHighlight(highlight)
    }

    suspend fun deleteHighlight(id: Long) {
        shamelaDao.deleteHighlight(id)
    }

    // Favorites
    val allFavorites: Flow<List<Favorite>> = shamelaDao.getAllFavorites()

    fun isFavorite(bookId: Int): Flow<Boolean> {
        return shamelaDao.isFavorite(bookId)
    }

    suspend fun insertFavorite(favorite: Favorite) {
        shamelaDao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(bookId: Int) {
        shamelaDao.deleteFavorite(bookId)
    }

    // Pasal.id Local Repository Delegations
    val allPasalFavorites: Flow<List<PasalFavorite>> = shamelaDao.getAllPasalFavorites()

    fun isPasalFavorite(frbrUri: String): Flow<Boolean> {
        return shamelaDao.isPasalFavorite(frbrUri)
    }

    suspend fun insertPasalFavorite(favorite: PasalFavorite) {
        shamelaDao.insertPasalFavorite(favorite)
    }

    suspend fun deletePasalFavorite(frbrUri: String) {
        shamelaDao.deletePasalFavorite(frbrUri)
    }

    // --- Network API Calls ---

    suspend fun getDashboard(): ApiResponse<DashboardData> {
        return ShamelaApiClient.service.getDashboard()
    }

    suspend fun getCategories(): ApiResponse<List<Category>> {
        return ShamelaApiClient.service.getCategories()
    }

    suspend fun getCategoryBooks(categoryId: Int, page: Int = 1): ApiListResponse<Book> {
        return ShamelaApiClient.service.getCategoryBooks(categoryId, page)
    }

    suspend fun getBooks(page: Int = 1, search: String? = null): ApiListResponse<Book> {
        return ShamelaApiClient.service.getBooks(page, search)
    }

    suspend fun getBookDetail(bookId: Int): ApiResponse<Book> {
        return ShamelaApiClient.service.getBookDetail(bookId)
    }

    suspend fun getToc(bookId: Int): ApiResponse<List<TocItem>> {
        val original = ShamelaApiClient.service.getToc(bookId, flat = false)
        
        fun mapTocItem(item: TocItem, level: Int): TocItem {
            val mappedChildren = item.children?.map { mapTocItem(it, level + 1) } ?: emptyList()
            return item.copy(
                id = bookId,
                title = item.title_text ?: "",
                global_order = item.target_global_order ?: 0,
                level = level,
                children = mappedChildren
            )
        }
        
        val mappedData = original.data.map { mapTocItem(it, 0) }
        return ApiResponse(success = original.success, data = mappedData)
    }

    suspend fun getPage(bookId: Int, order: Int): ApiResponse<PageContent> {
        return ShamelaApiClient.service.getPage(bookId, order)
    }

    suspend fun resolvePage(bookId: Int, part: String? = null, pageNum: Int? = null, globalOrder: Int? = null): ApiResponse<ResolveResult> {
        return ShamelaApiClient.service.resolvePage(bookId, part, pageNum, globalOrder)
    }

    suspend fun search(query: String, scope: String = "pages", bookId: Int? = null): ApiListResponse<SearchResultItem> {
        return ShamelaApiClient.service.search(query, scope, bookId)
    }
}
