package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(val categories: List<Category>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

sealed interface BooksUiState {
    object Loading : BooksUiState
    data class Success(val books: List<Book>, val categoryName: String? = null) : BooksUiState
    data class Error(val message: String) : BooksUiState
}

sealed interface TocUiState {
    object Loading : TocUiState
    data class Success(val toc: List<TocItem>) : TocUiState
    data class Error(val message: String) : TocUiState
}

sealed interface ReaderUiState {
    object Idle : ReaderUiState
    object Loading : ReaderUiState
    data class Success(val page: PageContent) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<SearchResultItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface PasalSearchUiState {
    object Idle : PasalSearchUiState
    object Loading : PasalSearchUiState
    data class Success(val data: PasalSearchResponse) : PasalSearchUiState
    data class Error(val message: String) : PasalSearchUiState
}

sealed interface PasalListUiState {
    object Loading : PasalListUiState
    data class Success(val data: PasalListResponse) : PasalListUiState
    data class Error(val message: String) : PasalListUiState
}

sealed interface PasalDetailUiState {
    object Idle : PasalDetailUiState
    object Loading : PasalDetailUiState
    data class Success(val data: PasalDetailResponse) : PasalDetailUiState
    data class Error(val message: String) : PasalDetailUiState
}

class ShamelaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShamelaRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ShamelaRepository(database.shamelaDao())
    }

    // Database-backed states
    val readingProgressList: StateFlow<List<ReadingProgress>> = repository.allReadingProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<Favorite>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksList: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightsList: StateFlow<List<Highlight>> = repository.allHighlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pasalFavoritesList: StateFlow<List<PasalFavorite>> = repository.allPasalFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _dashboardState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _categoriesState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val categoriesState: StateFlow<CategoriesUiState> = _categoriesState.asStateFlow()

    private val _booksState = MutableStateFlow<BooksUiState>(BooksUiState.Loading)
    val booksState: StateFlow<BooksUiState> = _booksState.asStateFlow()

    private val _tocState = MutableStateFlow<TocUiState>(TocUiState.Loading)
    val tocState: StateFlow<TocUiState> = _tocState.asStateFlow()

    private val _readerState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val readerState: StateFlow<ReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _pasalSearchState = MutableStateFlow<PasalSearchUiState>(PasalSearchUiState.Idle)
    val pasalSearchState: StateFlow<PasalSearchUiState> = _pasalSearchState.asStateFlow()

    private val _pasalListState = MutableStateFlow<PasalListUiState>(PasalListUiState.Loading)
    val pasalListState: StateFlow<PasalListUiState> = _pasalListState.asStateFlow()

    private val _pasalDetailState = MutableStateFlow<PasalDetailUiState>(PasalDetailUiState.Idle)
    val pasalDetailState: StateFlow<PasalDetailUiState> = _pasalDetailState.asStateFlow()

    // Navigation and Active Selection States
    var currentTab = MutableStateFlow("beranda")
    var activeBook = MutableStateFlow<Book?>(null)
    var activePage = MutableStateFlow<PageContent?>(null)

    // Pasal.id selection states
    var activePasalLaw = MutableStateFlow<PasalWork?>(null)
    var activePasalDetail = MutableStateFlow<PasalDetailResponse?>(null)
    var pasalSearchQuery = MutableStateFlow("")
    var pasalSelectedTypeFilter = MutableStateFlow<String?>(null)
    var pasalSelectedYearFilter = MutableStateFlow<Int?>(null)
    var pasalSelectedStatusFilter = MutableStateFlow<String?>(null)
    
    // Reader configuration (saved in memory, reset on app restart or custom preference can be added)
    var readerFontSize = MutableStateFlow(22f)
    var readerLineSpacing = MutableStateFlow(1.6f)
    var readerFontFamily = MutableStateFlow("Amiri")
    var readerTheme = MutableStateFlow("Dark") // Sepia, Light, Dark

    // AI States
    var aiExplanation = MutableStateFlow<String?>(null)
    var isExplainingText = MutableStateFlow(false)

    var aiChatHistory = MutableStateFlow<List<Pair<String, String>>>(listOf(
        "asisten" to "Assalamu'alaikum! Saya adalah AI Assistant Shamela. Tanyakan apa saja tentang kandungan kitab-kitab klasik, tafsir, hukum fikih, hadits, atau tata bahasa Arab."
    ))
    var isAiTyping = MutableStateFlow(false)

    // OpenAI-Compatible AI States & Persistence via SharedPreferences
    private val prefs = application.getSharedPreferences("shamela_ai_prefs", android.content.Context.MODE_PRIVATE)

    var pasalToken = MutableStateFlow(prefs.getString("pasal_api_token", "") ?: "")

    fun updatePasalToken(value: String) {
        pasalToken.value = value
        prefs.edit().putString("pasal_api_token", value).apply()
    }

    var useOpenAi = MutableStateFlow(prefs.getBoolean("use_openai", false))
    var openAiApiKey = MutableStateFlow(prefs.getString("openai_api_key", "sk-8Epc1-5QOLoQ1KGvOwuZhg") ?: "sk-8Epc1-5QOLoQ1KGvOwuZhg")
    var openAiBaseUrl = MutableStateFlow(prefs.getString("openai_base_url", "https://ai.sumopod.com/v1") ?: "https://ai.sumopod.com/v1")
    var openAiModel = MutableStateFlow(prefs.getString("openai_model", "gemini/gemini-2.5-flash-lite") ?: "gemini/gemini-2.5-flash-lite")
    
    private val defaultModelsString = "gemini/gemini-2.5-flash-lite,gpt-4o-mini,gpt-4o,claude-3-5-sonnet"
    var openAiAvailableModels = MutableStateFlow<List<String>>(
        (prefs.getString("openai_available_models", defaultModelsString) ?: defaultModelsString)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    )

    var isFetchingModels = MutableStateFlow(false)
    var fetchModelsError = MutableStateFlow<String?>(null)

    fun updateUseOpenAi(value: Boolean) {
        useOpenAi.value = value
        prefs.edit().putBoolean("use_openai", value).apply()
    }

    fun updateOpenAiApiKey(value: String) {
        openAiApiKey.value = value
        prefs.edit().putString("openai_api_key", value).apply()
    }

    fun updateOpenAiBaseUrl(value: String) {
        openAiBaseUrl.value = value
        prefs.edit().putString("openai_base_url", value).apply()
    }

    fun updateOpenAiModel(value: String) {
        openAiModel.value = value
        prefs.edit().putString("openai_model", value).apply()
    }

    fun fetchModels() {
        viewModelScope.launch {
            isFetchingModels.value = true
            fetchModelsError.value = null
            try {
                val models = OpenAiApiClient.fetchModels(openAiBaseUrl.value, openAiApiKey.value)
                if (models.isNotEmpty()) {
                    openAiAvailableModels.value = models
                    prefs.edit().putString("openai_available_models", models.joinToString(",")).apply()
                    if (!models.contains(openAiModel.value)) {
                        updateOpenAiModel(models.first())
                    }
                    fetchModelsError.value = "Berhasil memuat ${models.size} model!"
                } else {
                    fetchModelsError.value = "Tidak ada model yang ditemukan."
                }
            } catch (e: Exception) {
                fetchModelsError.value = "Gagal memuat model: ${e.localizedMessage}"
            } finally {
                isFetchingModels.value = false
            }
        }
    }

    // Model Context Protocol (MCP) States
    var useMcp = MutableStateFlow(prefs.getBoolean("use_mcp", true))
    var mcpBaseUrl = MutableStateFlow(prefs.getString("mcp_base_url", "https://winongkencono-shamelah.hf.space") ?: "https://winongkencono-shamelah.hf.space")
    var mcpStatusText = MutableStateFlow("Menghubungkan...")

    fun updateUseMcp(value: Boolean) {
        useMcp.value = value
        prefs.edit().putBoolean("use_mcp", value).apply()
        if (value) {
            viewModelScope.launch {
                com.example.data.McpClient.startConnection(mcpBaseUrl.value)
                checkMcpStatusPeriodically()
            }
        } else {
            com.example.data.McpClient.close()
            mcpStatusText.value = "Dinonaktifkan"
        }
    }

    fun updateMcpBaseUrl(value: String) {
        mcpBaseUrl.value = value
        prefs.edit().putString("mcp_base_url", value).apply()
        com.example.data.McpClient.close()
        viewModelScope.launch {
            com.example.data.McpClient.startConnection(value)
            checkMcpStatusPeriodically()
        }
    }

    private fun checkMcpStatusPeriodically() {
        viewModelScope.launch {
            for (i in 1..8) {
                val status = com.example.data.McpClient.getStatus()
                mcpStatusText.value = status
                if (status.contains("Terhubung")) break
                delay(1200)
            }
            mcpStatusText.value = com.example.data.McpClient.getStatus()
        }
    }

    // AI Search Translation States
    var useAiSearchTranslation = MutableStateFlow(prefs.getBoolean("use_ai_search_translation", true))
    var isTranslatingQuery = MutableStateFlow(false)
    var translatedQuery = MutableStateFlow("")

    fun updateUseAiSearchTranslation(value: Boolean) {
        useAiSearchTranslation.value = value
        prefs.edit().putBoolean("use_ai_search_translation", value).apply()
    }

    fun isQueryArabic(text: String): Boolean {
        val arabicRegex = Regex("[\\u0600-\\u06FF]+")
        return arabicRegex.containsMatchIn(text)
    }

    init {
        loadDashboard()
        loadCategories()
        loadAllBooks()
        if (useMcp.value) {
            com.example.data.McpClient.startConnection(mcpBaseUrl.value)
            checkMcpStatusPeriodically()
        } else {
            mcpStatusText.value = "Dinonaktifkan"
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = DashboardUiState.Loading
            try {
                val response = repository.getDashboard()
                if (response.success) {
                    _dashboardState.value = DashboardUiState.Success(response.data)
                } else {
                    _dashboardState.value = DashboardUiState.Error("Gagal mengambil data beranda.")
                }
            } catch (e: Exception) {
                _dashboardState.value = DashboardUiState.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan.")
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesUiState.Loading
            try {
                val response = repository.getCategories()
                if (response.success) {
                    _categoriesState.value = CategoriesUiState.Success(response.data)
                } else {
                    _categoriesState.value = CategoriesUiState.Error("Gagal mengambil kategori.")
                }
            } catch (e: Exception) {
                _categoriesState.value = CategoriesUiState.Error(e.localizedMessage ?: "Terjadi kesalahan.")
            }
        }
    }

    fun loadBooksByCategory(category: Category) {
        viewModelScope.launch {
            _booksState.value = BooksUiState.Loading
            try {
                val response = repository.getCategoryBooks(category.id)
                if (response.success) {
                    _booksState.value = BooksUiState.Success(response.data, category.name)
                } else {
                    _booksState.value = BooksUiState.Error("Gagal mengambil kitab dalam kategori.")
                }
            } catch (e: Exception) {
                _booksState.value = BooksUiState.Error(e.localizedMessage ?: "Terjadi kesalahan.")
            }
        }
    }

    fun loadAllBooks() {
        viewModelScope.launch {
            _booksState.value = BooksUiState.Loading
            try {
                val response = repository.getBooks(page = 1)
                if (response.success) {
                    _booksState.value = BooksUiState.Success(response.data, "Semua Kitab")
                } else {
                    _booksState.value = BooksUiState.Error("Gagal mengambil data kitab.")
                }
            } catch (e: Exception) {
                _booksState.value = BooksUiState.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan.")
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) {
            loadAllBooks()
            return
        }
        viewModelScope.launch {
            _booksState.value = BooksUiState.Loading
            try {
                val response = repository.getBooks(search = query)
                if (response.success) {
                    _booksState.value = BooksUiState.Success(response.data, "Hasil Pencarian")
                } else {
                    _booksState.value = BooksUiState.Error("Tidak ada hasil.")
                }
            } catch (e: Exception) {
                _booksState.value = BooksUiState.Error(e.localizedMessage ?: "Gagal menelusuri kitab.")
            }
        }
    }

    fun loadBookDetailAndToc(book: Book) {
        activeBook.value = book
        _tocState.value = TocUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getToc(book.id)
                if (response.success) {
                    _tocState.value = TocUiState.Success(response.data)
                } else {
                    _tocState.value = TocUiState.Error("Daftar isi tidak tersedia.")
                }
            } catch (e: Exception) {
                _tocState.value = TocUiState.Error(e.localizedMessage ?: "Gagal mengambil daftar isi.")
            }
        }
    }

    fun loadPageFromToc(bookId: Int, item: TocItem) {
        val order = item.global_order
        if (order > 0) {
            loadPage(bookId, order)
        } else {
            _readerState.value = ReaderUiState.Loading
            viewModelScope.launch {
                try {
                    val query = item.title.trim()
                    if (query.isNotEmpty()) {
                        val response = repository.search(query, scope = "pages", bookId = bookId)
                        if (response.success && response.data.isNotEmpty()) {
                            val bestMatch = response.data.firstOrNull { it.book_id == bookId }
                            if (bestMatch != null) {
                                loadPage(bookId, bestMatch.global_order)
                                return@launch
                            }
                        }
                    }
                    // Fallback
                    loadPage(bookId, 1)
                } catch (e: Exception) {
                    loadPage(bookId, 1)
                }
            }
        }
    }

    fun loadPage(bookId: Int, globalOrder: Int) {
        _readerState.value = ReaderUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getPage(bookId, globalOrder)
                if (response.success) {
                    val pageContent = response.data
                    activePage.value = pageContent
                    _readerState.value = ReaderUiState.Success(pageContent)
                    
                    // Save reading progress to local database
                    val book = activeBook.value
                    if (book != null) {
                        repository.saveReadingProgress(
                            ReadingProgress(
                                bookId = bookId,
                                bookTitle = book.title,
                                bookAuthor = book.author_name ?: "Unknown",
                                globalOrder = globalOrder,
                                pageNum = pageContent.page_num,
                                sectionTitle = pageContent.section_title ?: "Halaman $globalOrder"
                            )
                        )
                    }
                } else {
                    _readerState.value = ReaderUiState.Error("Gagal memuat halaman.")
                }
            } catch (e: Exception) {
                _readerState.value = ReaderUiState.Error(e.localizedMessage ?: "Koneksi terputus.")
            }
        }
    }

    fun jumpToPage(bookId: Int, part: String?, pageNum: Int) {
        _readerState.value = ReaderUiState.Loading
        viewModelScope.launch {
            try {
                val resolveRes = repository.resolvePage(bookId, part, pageNum)
                if (resolveRes.success) {
                    loadPage(bookId, resolveRes.data.global_order)
                } else {
                    _readerState.value = ReaderUiState.Error("Gagal melacak halaman cetak.")
                }
            } catch (e: Exception) {
                _readerState.value = ReaderUiState.Error(e.localizedMessage ?: "Gagal melacak.")
            }
        }
    }

    fun searchTextInPages(query: String, bookId: Int? = null) {
        if (query.isBlank()) {
            _searchState.value = SearchUiState.Idle
            translatedQuery.value = ""
            return
        }
        _searchState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                var finalQuery = query
                if (!isQueryArabic(query) && useAiSearchTranslation.value) {
                    isTranslatingQuery.value = true
                    try {
                        val translated = if (useOpenAi.value) {
                            com.example.data.OpenAiApiClient.translateQueryToArabic(
                                baseUrl = openAiBaseUrl.value,
                                apiKey = openAiApiKey.value,
                                model = openAiModel.value,
                                query = query
                            )
                        } else {
                            com.example.data.GeminiApiClient.translateQueryToArabic(query)
                        }
                        if (translated.isNotBlank() && translated != query) {
                            finalQuery = translated
                            translatedQuery.value = translated
                        } else {
                            translatedQuery.value = ""
                        }
                    } catch (e: Exception) {
                        translatedQuery.value = ""
                    } finally {
                        isTranslatingQuery.value = false
                    }
                } else {
                    translatedQuery.value = ""
                }

                val response = repository.search(finalQuery, scope = "pages", bookId = bookId)
                if (response.success && response.data.isNotEmpty()) {
                    _searchState.value = SearchUiState.Success(response.data)
                } else {
                    _searchState.value = SearchUiState.Error("Pencarian tidak menemukan hasil.")
                }
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error(e.localizedMessage ?: "Gagal melakukan pencarian teks.")
            }
        }
    }

    fun clearSearchState() {
        _searchState.value = SearchUiState.Idle
        translatedQuery.value = ""
    }

    // Favorites operations
    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            val isFav = favoritesList.value.any { it.bookId == book.id }
            if (isFav) {
                repository.deleteFavorite(book.id)
            } else {
                repository.insertFavorite(
                    Favorite(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author_name ?: "Unknown"
                    )
                )
            }
        }
    }

    // Bookmarks operations
    fun addBookmark(note: String? = null) {
        val page = activePage.value ?: return
        val book = activeBook.value ?: return
        viewModelScope.launch {
            repository.insertBookmark(
                Bookmark(
                    bookId = page.book_id,
                    bookTitle = book.title,
                    globalOrder = page.global_order,
                    pageNum = page.page_num,
                    sectionTitle = page.section_title ?: "Halaman ${page.global_order}",
                    note = note
                )
            )
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    // Highlights operations
    fun addHighlight(text: String, color: String = "#FFEB3B") {
        val page = activePage.value ?: return
        viewModelScope.launch {
            repository.insertHighlight(
                Highlight(
                    bookId = page.book_id,
                    globalOrder = page.global_order,
                    text = text,
                    color = color
                )
            )
        }
    }

    fun removeHighlight(id: Long) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
        }
    }

    private fun extractArabicOrKeywords(text: String): String {
        // Find if there is Arabic text in the query and search with it
        val arabicRegex = Regex("[\\u0600-\\u06FF]+")
        val arabicMatches = arabicRegex.findAll(text).map { it.value }.toList()
        if (arabicMatches.isNotEmpty()) {
            return arabicMatches.joinToString(" ")
        }
        
        // Otherwise, extract key Islamic terms in Indonesian/Arabic (e.g. wudhu, shalat, najis, dll)
        val keywords = listOf(
            "wudhu", "shalat", "salat", "sholat", "najis", "puasa", "zakat", "haji",
            "nikah", "talak", "waris", "riba", "jual beli", "hadis", "tafsir", "fardhu",
            "sunnah", "makruh", "haram", "syarat", "rukun", "batal", "istinja", "tayammum",
            "mandi wajib", "haid", "nifas", "zakat fitrah", "murtad", "ijtihad", "taqlid"
        )
        val found = keywords.filter { text.contains(it, ignoreCase = true) }
        if (found.isNotEmpty()) {
            return found.joinToString(" ")
        }
        
        return text
    }

    private suspend fun getMcpContext(query: String): String {
        if (!useMcp.value) return ""
        
        try {
            val searchTerm = extractArabicOrKeywords(query)
            if (searchTerm.isBlank()) return ""

            val responseJson = com.example.data.McpClient.executeTool(
                "search_arabic_text",
                mapOf("q" to searchTerm)
            )
            
            val searchResultText = com.example.data.McpClient.parseToolResultText(responseJson)
            if (searchResultText.isNotEmpty() && !searchResultText.startsWith("Error")) {
                return "\n\n=== RUJUKAN ILMIAH & SITASI KITAB (Ditemukan via MCP Server) ===\n" +
                        "Berikut adalah teks asli bahasa Arab dan kutipan langsung dari kitab klasik al-Maktaba al-Shamela terkait pertanyaan Anda. " +
                        "Wajib sertakan rujukan nama kitab, bab, juz/halaman, penerbit, dan nomor bab dalam merumuskan sitasi ilmiah bagi jawaban hukum atau penjelasan Anda.\n\n" +
                        "$searchResultText\n" +
                        "===========================================================\n"
            }
        } catch (e: Exception) {
            // ignore
        }
        return ""
    }

    // AI explanations from Reader text selection
    fun explainText(selectedText: String, analysisType: String = "makna") {
        val bookTitle = activeBook.value?.title ?: "Kitab"
        aiExplanation.value = "" // Start with empty instead of null to indicate start
        isExplainingText.value = true
        viewModelScope.launch {
            var mcpContext = ""
            if (useMcp.value) {
                try {
                    val mcpResult = com.example.data.McpClient.executeTool(
                        "search_arabic_text",
                        mapOf("q" to selectedText)
                    )
                    val parsed = com.example.data.McpClient.parseToolResultText(mcpResult)
                    if (parsed.isNotEmpty() && !parsed.startsWith("Error")) {
                        mcpContext = "\n\nBerikut adalah teks serupa dan referensi silang ilmiah dari al-Maktaba al-Shamela untuk membantu penjelasan ilmiah Anda:\n$parsed\n"
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            val finalSelectedText = selectedText + mcpContext

            val streamFlow = if (useOpenAi.value) {
                OpenAiApiClient.getExplanationStream(
                    baseUrl = openAiBaseUrl.value,
                    apiKey = openAiApiKey.value,
                    model = openAiModel.value,
                    textToExplain = finalSelectedText,
                    contextBook = bookTitle,
                    analysisType = analysisType
                )
            } else {
                GeminiApiClient.getExplanationStream(finalSelectedText, bookTitle, analysisType)
            }

            streamFlow
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    aiExplanation.value = "Gagal menghubungi AI Assistant: ${e.localizedMessage}"
                    isExplainingText.value = false
                }
                .collect { chunk ->
                    aiExplanation.value = (aiExplanation.value ?: "") + chunk
                    isExplainingText.value = false // Toggle typing/loading off once we start streaming
                }
            isExplainingText.value = false
        }
    }

    // AI direct chat
    fun sendAiMessage(message: String) {
        if (message.isBlank()) return
        val history = aiChatHistory.value.toMutableList()
        history.add("user" to message)
        
        // Add a placeholder message for the assistant that we will update as we stream
        val assistantPlaceholderIndex = history.size
        history.add("asisten" to "")
        aiChatHistory.value = history
        isAiTyping.value = true

        viewModelScope.launch {
            var mcpContext = ""
            if (useMcp.value) {
                mcpContext = getMcpContext(message)
            }
            val finalMessage = message + mcpContext

            var accumulatedAnswer = ""
            val streamFlow = if (useOpenAi.value) {
                OpenAiApiClient.askAssistantStream(
                    baseUrl = openAiBaseUrl.value,
                    apiKey = openAiApiKey.value,
                    model = openAiModel.value,
                    userQuery = finalMessage
                )
            } else {
                GeminiApiClient.askAssistantStream(finalMessage)
            }

            streamFlow
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    val currentHistory = aiChatHistory.value.toMutableList()
                    if (assistantPlaceholderIndex < currentHistory.size) {
                        currentHistory[assistantPlaceholderIndex] = "asisten" to "Gagal menghubungi AI Assistant: ${e.localizedMessage}"
                        aiChatHistory.value = currentHistory
                    }
                    isAiTyping.value = false
                }
                .collect { chunk ->
                    accumulatedAnswer += chunk
                    val currentHistory = aiChatHistory.value.toMutableList()
                    if (assistantPlaceholderIndex < currentHistory.size) {
                        currentHistory[assistantPlaceholderIndex] = "asisten" to accumulatedAnswer
                        aiChatHistory.value = currentHistory
                    }
                    isAiTyping.value = false // Toggle typing off once stream starts producing content
                }
            isAiTyping.value = false
        }
    }

    fun clearChat() {
        aiChatHistory.value = listOf(
            "asisten" to "Assalamu'alaikum! Saya adalah AI Assistant Shamela. Tanyakan apa saja tentang kandungan kitab-kitab klasik, tafsir, hukum fikih, hadits, atau tata bahasa Arab."
        )
    }

    // Pasal.id API Operations
    fun searchPasal(query: String, type: String? = null) {
        if (query.isBlank()) {
            _pasalSearchState.value = PasalSearchUiState.Idle
            return
        }
        _pasalSearchState.value = PasalSearchUiState.Loading
        viewModelScope.launch {
            try {
                val authHeader = PasalApiClient.getAuthHeader(pasalToken.value)
                val response = PasalApiClient.service.search(authHeader, query, type)
                _pasalSearchState.value = PasalSearchUiState.Success(response)
            } catch (e: Exception) {
                _pasalSearchState.value = PasalSearchUiState.Error(e.localizedMessage ?: "Gagal melakukan pencarian peraturan.")
            }
        }
    }

    fun loadPasalLaws(type: String? = null, year: Int? = null, status: String? = null, limit: Int = 20, offset: Int = 0) {
        _pasalListState.value = PasalListUiState.Loading
        viewModelScope.launch {
            try {
                val authHeader = PasalApiClient.getAuthHeader(pasalToken.value)
                val response = PasalApiClient.service.getLaws(authHeader, type, year, status, limit, offset)
                _pasalListState.value = PasalListUiState.Success(response)
            } catch (e: Exception) {
                _pasalListState.value = PasalListUiState.Error(e.localizedMessage ?: "Gagal mengambil daftar peraturan.")
            }
        }
    }

    fun loadPasalLawDetail(frbrUri: String) {
        val cleanUri = frbrUri.removePrefix("/")
        _pasalDetailState.value = PasalDetailUiState.Loading
        viewModelScope.launch {
            try {
                val authHeader = PasalApiClient.getAuthHeader(pasalToken.value)
                val response = PasalApiClient.service.getLawDetail(authHeader, cleanUri)
                activePasalDetail.value = response
                _pasalDetailState.value = PasalDetailUiState.Success(response)
            } catch (e: Exception) {
                _pasalDetailState.value = PasalDetailUiState.Error(e.localizedMessage ?: "Gagal memuat detail peraturan.")
            }
        }
    }

    fun togglePasalFavorite(work: PasalWork) {
        viewModelScope.launch {
            val isFav = pasalFavoritesList.value.any { it.frbrUri == work.frbrUri }
            if (isFav) {
                repository.deletePasalFavorite(work.frbrUri)
            } else {
                repository.insertPasalFavorite(
                    PasalFavorite(
                        frbrUri = work.frbrUri,
                        title = work.title,
                        number = work.number,
                        year = work.year,
                        type = work.type,
                        status = work.status
                    )
                )
            }
        }
    }
}
