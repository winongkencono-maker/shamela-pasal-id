package com.example.ui.screens

import android.text.Html
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.launch

// Color Palettes
object ShamelaColors {
    // Sepia Paper Theme
    val SepiaBg = Color(0xFFFCF6EB)
    val SepiaCard = Color(0xFFF3E7D3)
    val SepiaText = Color(0xFF2E1C0C)
    val SepiaGold = Color(0xFFC5A059)
    val SepiaPrimary = Color(0xFF6D4C41)
    val SepiaAccent = Color(0xFF8D6E63)

    // Dark Night Theme (Sophisticated Dark)
    val DarkBg = Color(0xFF0F110E)
    val DarkCard = Color(0xFF1C1E1B)
    val DarkText = Color(0xFFE1E3DE)
    val DarkGold = Color(0xFF7ABA82)
    val DarkPrimary = Color(0xFF386B40)
    val DarkAccent = Color(0xFF2D312C)

    // Classic Light Theme
    val LightBg = Color(0xFFF9F9F9)
    val LightCard = Color(0xFFFFFFFF)
    val LightText = Color(0xFF1A1A1A)
    val LightGold = Color(0xFFB8860B)
    val LightPrimary = Color(0xFF4E342E)
    val LightAccent = Color(0xFF5D4037)

    @Composable
    fun getColors(themeName: String): ReaderThemeColors {
        return when (themeName) {
            "Dark" -> ReaderThemeColors(DarkBg, DarkCard, DarkText, DarkGold, DarkPrimary, DarkAccent)
            "Light" -> ReaderThemeColors(LightBg, LightCard, LightText, LightGold, LightPrimary, LightAccent)
            else -> ReaderThemeColors(SepiaBg, SepiaCard, SepiaText, SepiaGold, SepiaPrimary, SepiaAccent)
        }
    }
}

data class ReaderThemeColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val gold: Color,
    val primary: Color,
    val accent: Color
)

// Helper to Strip HTML Tags or render clean text
fun stripHtml(html: String): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString().trim()
    }
}

fun normalizeNumerals(text: String): String {
    val indicMap = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )
    return text.map { indicMap[it] ?: it }.joinToString("")
}

fun findMatchingFootnote(footnoteText: String?, marker: String): String? {
    if (footnoteText.isNullOrBlank()) return null
    val lines = footnoteText.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    val normMarker = normalizeNumerals(marker).trim()
    
    for (line in lines) {
        val cleanLine = stripHtml(line).trim()
        val normLine = normalizeNumerals(cleanLine)
        
        val prefixWithMarker = "^\\s*[\\(\\[]?$marker[\\)\\]]?\\s*[-:.]".toRegex()
        val prefixWithNormMarker = "^\\s*[\\(\\[]?$normMarker[\\)\\]]?\\s*[-:.]".toRegex()
        val prefixSpaceMarker = "^\\s*[\\(\\[]?$marker[\\)\\]]?\\s+".toRegex()
        val prefixSpaceNormMarker = "^\\s*[\\(\\[]?$normMarker[\\)\\]]?\\s+".toRegex()
        
        if (prefixWithMarker.containsMatchIn(cleanLine) || 
            prefixWithNormMarker.containsMatchIn(normLine) ||
            prefixSpaceMarker.containsMatchIn(cleanLine) ||
            prefixSpaceNormMarker.containsMatchIn(normLine)
        ) {
            return line
        }
    }
    
    for (line in lines) {
        val cleanLine = stripHtml(line).trim()
        val normLine = normalizeNumerals(cleanLine)
        if (normLine.contains("[$normMarker]") || normLine.contains("($normMarker)") || normLine.startsWith(normMarker)) {
            return line
        }
    }
    
    return null
}

// Google Fonts Provider Setup
val gmsFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.example.R.array.com_google_android_gms_fonts_certs
)

val AmiriFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Amiri"), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Amiri"), fontProvider = gmsFontProvider, weight = FontWeight.Bold)
)

val ScheherazadeNewFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Scheherazade New"), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Scheherazade New"), fontProvider = gmsFontProvider, weight = FontWeight.Bold)
)

val NotoNaskhArabicFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Noto Naskh Arabic"), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Noto Naskh Arabic"), fontProvider = gmsFontProvider, weight = FontWeight.Bold)
)

val CairoFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Cairo"), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Cairo"), fontProvider = gmsFontProvider, weight = FontWeight.Bold)
)

val LateefFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = GoogleFont("Lateef"), fontProvider = gmsFontProvider, weight = FontWeight.Normal)
)

data class ProcessedLine(
    val originalText: String,
    val isTitle: Boolean,
    val isBismillah: Boolean = false,
    val cleanText: String,
    val annotatedText: AnnotatedString
)

fun parseArabicTextToAnnotatedString(
    cleanText: String,
    themeColors: ReaderThemeColors,
    fontSize: Float
): AnnotatedString {
    return buildAnnotatedString {
        append(cleanText)
        
        // 1. Highlight Hadith quotes inside « and »
        val hadithRegex = """«([^»]*)»""".toRegex()
        hadithRegex.findAll(cleanText).forEach { matchResult ->
            val range = matchResult.range
            addStyle(
                style = SpanStyle(
                    color = Color(0xFFE65100), // Elegant deep warm amber/orange
                    fontWeight = FontWeight.Bold
                ),
                start = range.first,
                end = range.last + 1
            )
        }

        // 2. Highlight Quran quotes inside ﴿ and ﴾
        val quranRegex = """﴿([^﴾]*)﴾""".toRegex()
        quranRegex.findAll(cleanText).forEach { matchResult ->
            val range = matchResult.range
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF2E7D32), // Beautiful organic leafy green
                    fontWeight = FontWeight.Bold
                ),
                start = range.first,
                end = range.last + 1
            )
        }

        // 3. Highlight Islamic honorific ligatures with elegant gold/teal
        val honorifics = setOf('ﷺ', 'ﷻ', '﵇', '﵂', '﵄', '﵀', '﵁', '﵅', '﵈')
        cleanText.forEachIndexed { index, char ->
            if (char in honorifics) {
                addStyle(
                    style = SpanStyle(
                        color = themeColors.gold,
                        fontWeight = FontWeight.Bold
                    ),
                    start = index,
                    end = index + 1
                )
            }
        }

        // 4. Style Academic/Footnote anchors like (¬١), (¬٢), or (¬...)
        val footnoteRegex = """\(¬([^\)]+)\)""".toRegex()
        footnoteRegex.findAll(cleanText).forEach { matchResult ->
            val range = matchResult.range
            val footnoteMarker = matchResult.groupValues[1].trim()
            addStyle(
                style = SpanStyle(
                    color = themeColors.gold.copy(alpha = 0.8f),
                    fontSize = (fontSize * 0.7f).sp,
                    baselineShift = BaselineShift.Superscript,
                    fontWeight = FontWeight.Bold
                ),
                start = range.first,
                end = range.last + 1
            )
            addStringAnnotation(
                tag = "footnote",
                annotation = footnoteMarker,
                start = range.first,
                end = range.last + 1
            )
        }

        // 5. Highlight paragraph prefix numbering (e.g. "١١٩ - " or "119 - ")
        val prefixNumRegex = """^\s*([0-9\u0660-\u0669]+)\s*[-]""".toRegex()
        prefixNumRegex.find(cleanText)?.let { matchResult ->
            val range = matchResult.range
            addStyle(
                style = SpanStyle(
                    color = themeColors.gold,
                    fontWeight = FontWeight.ExtraBold
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

// Retro classical leather-bound Arabic book cover
@Composable
fun BookCover(title: String, author: String, modifier: Modifier = Modifier, themeColors: ReaderThemeColors) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.primary,
                        themeColors.primary.copy(alpha = 0.85f)
                    )
                )
            )
            .border(2.dp, themeColors.gold, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Gold ornamental border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, themeColors.gold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Classical gold motif top
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = themeColors.gold,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.gold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(1.dp)
                            .background(themeColors.gold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = author,
                        fontSize = 11.sp,
                        color = themeColors.text.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Classical gold motif bottom
                Text(
                    text = "تراث",
                    fontSize = 10.sp,
                    color = themeColors.gold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Book spine overlay shade
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(12.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.CenterStart)
        )
    }
}

@Composable
fun BookItemCard(book: Book, onClick: () -> Unit, themeColors: ReaderThemeColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_card_${book.id}"),
        colors = CardDefaults.cardColors(containerColor = themeColors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (book.cover_url != null && book.cover_url.isNotBlank()) {
                AsyncImage(
                    model = book.cover_url,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                BookCover(
                    title = book.title,
                    author = book.author_name ?: "Al-Mualif",
                    themeColors = themeColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = themeColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = book.author_name ?: "Wafat -- H",
                fontSize = 11.sp,
                color = themeColors.text.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 1. HOME SCREEN (BERANDA)
@Composable
fun BerandaScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors, onNavigateToReader: () -> Unit) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val progressList by viewModel.readingProgressList.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Assalamu'alaikum,",
                        fontSize = 14.sp,
                        color = themeColors.text.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Ahmad Dzakwan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = themeColors.gold
                    )
                }
            }
        }

        // Lanjutkan Membaca (Active reading progress from local DB)
        item {
            Column {
                Text(
                    text = "Lanjutkan Membaca",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.text,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (progressList.isNotEmpty()) {
                    val lastProgress = progressList.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val dummyBook = Book(
                                    id = lastProgress.bookId,
                                    title = lastProgress.bookTitle,
                                    author_name = lastProgress.bookAuthor
                                )
                                viewModel.loadBookDetailAndToc(dummyBook)
                                viewModel.loadPage(lastProgress.bookId, lastProgress.globalOrder)
                                onNavigateToReader()
                            },
                        colors = CardDefaults.cardColors(containerColor = themeColors.card),
                        border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCover(
                                title = lastProgress.bookTitle,
                                author = lastProgress.bookAuthor,
                                modifier = Modifier.width(60.dp),
                                themeColors = themeColors
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lastProgress.bookTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = themeColors.text
                                )
                                Text(
                                    text = lastProgress.bookAuthor,
                                    fontSize = 12.sp,
                                    color = themeColors.text.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lastProgress.sectionTitle,
                                    fontSize = 13.sp,
                                    color = themeColors.gold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { 0.35f }, // Mock read progress
                                        color = themeColors.gold,
                                        trackColor = themeColors.text.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hal. ${lastProgress.pageNum}",
                                        fontSize = 11.sp,
                                        color = themeColors.text.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty reading progress placeholder
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.card)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImportContacts,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = themeColors.gold.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mulai Membaca Kitab",
                                fontWeight = FontWeight.Bold,
                                color = themeColors.text
                            )
                            Text(
                                text = "Kitab yang sedang Anda baca akan ditampilkan di sini untuk mempermudah melanjutkan bacaan.",
                                fontSize = 12.sp,
                                color = themeColors.text.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Aktivitas Terbaru (Room bookmarks and highlights)
        item {
            Column {
                Text(
                    text = "Aktivitas Terbaru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.text,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val bookmarks by viewModel.bookmarksList.collectAsState()
                val highlights by viewModel.highlightsList.collectAsState()

                if (bookmarks.isEmpty() && highlights.isEmpty()) {
                    Text(
                        text = "Belum ada catatan, bookmark, atau highlight.",
                        fontSize = 13.sp,
                        color = themeColors.text.copy(alpha = 0.5f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookmarks.take(2).forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeColors.card, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = themeColors.gold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bookmark di ${b.bookTitle}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = themeColors.text
                                    )
                                    Text(
                                        text = "${b.sectionTitle} - Hal. ${b.pageNum}",
                                        fontSize = 11.sp,
                                        color = themeColors.text.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        highlights.take(2).forEach { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeColors.card, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Highlight,
                                    contentDescription = null,
                                    tint = Color(android.graphics.Color.parseColor(h.color)),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "\"${h.text.take(30)}...\"",
                                        fontSize = 13.sp,
                                        color = themeColors.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Highlight tersimpan",
                                        fontSize = 11.sp,
                                        color = themeColors.text.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notable & Recommended Books (API dashboard integration)
        item {
            Column {
                Text(
                    text = "Kitab Rekomendasi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.text,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when (val state = dashboardState) {
                    is DashboardUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = themeColors.gold)
                        }
                    }
                    is DashboardUiState.Success -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val books = state.data.notableBooks ?: emptyList()
                            items(books) { book ->
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable {
                                            viewModel.loadBookDetailAndToc(book)
                                            // Load first page
                                            viewModel.loadPage(book.id, 1)
                                            onNavigateToReader()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column {
                                        BookCover(
                                            title = book.title,
                                            author = book.author_name ?: "Turats",
                                            themeColors = themeColors,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = book.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = themeColors.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is DashboardUiState.Error -> {
                        Text(
                            text = "Gagal memuat rekomendasi: ${state.message}",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// 2. LIBRARY SCREEN (PERPUSTAKAAN)
@Composable
fun PerpustakaanScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors, onNavigateToReader: () -> Unit) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Semua, 1: Kategori, 2: Favorit
    val categoriesState by viewModel.categoriesState.collectAsState()
    val booksState by viewModel.booksState.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = themeColors.card,
            contentColor = themeColors.gold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = themeColors.gold
                )
            }
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text(text = "Semua Kitab", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text(text = "Kategori", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }) {
                Text(text = "Favorit", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }

        // Search bar
        if (selectedSubTab == 0) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = {
                    searchKeyword = it
                    viewModel.searchBooks(it)
                },
                placeholder = { Text("Cari kitab di katalog...", color = themeColors.text.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.gold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.gold,
                    unfocusedBorderColor = themeColors.gold.copy(alpha = 0.4f),
                    focusedContainerColor = themeColors.card,
                    unfocusedContainerColor = themeColors.card,
                    focusedTextColor = themeColors.text,
                    unfocusedTextColor = themeColors.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> {
                    // Semua Kitab Grid
                    when (val state = booksState) {
                        is BooksUiState.Loading -> {
                            CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.align(Alignment.Center))
                        }
                        is BooksUiState.Success -> {
                            if (state.books.isEmpty()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = themeColors.gold, modifier = Modifier.size(48.dp))
                                    Text("Kitab tidak ditemukan", color = themeColors.text, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.books) { book ->
                                        BookItemCard(book, onClick = {
                                            viewModel.loadBookDetailAndToc(book)
                                            viewModel.loadPage(book.id, 1)
                                            onNavigateToReader()
                                        }, themeColors = themeColors)
                                    }
                                }
                            }
                        }
                        is BooksUiState.Error -> {
                            Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
                1 -> {
                    // Kategori List
                    when (val state = categoriesState) {
                        is CategoriesUiState.Loading -> {
                            CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.align(Alignment.Center))
                        }
                        is CategoriesUiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(state.categories) { cat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.loadBooksByCategory(cat)
                                                selectedSubTab = 0
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = themeColors.gold,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(text = cat.name, fontWeight = FontWeight.Bold, color = themeColors.text)
                                                Text(text = "${cat.book_count ?: 0} kitab", fontSize = 12.sp, color = themeColors.text.copy(alpha = 0.5f))
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = themeColors.gold)
                                    }
                                    HorizontalDivider(color = themeColors.gold.copy(alpha = 0.15f))
                                }
                            }
                        }
                        is CategoriesUiState.Error -> {
                            Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
                2 -> {
                    // Favorit
                    if (favorites.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = themeColors.gold.copy(alpha = 0.5f), modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada favorit", fontWeight = FontWeight.Bold, color = themeColors.text)
                            Text("Favoritkan kitab agar mudah dibaca offline nanti.", fontSize = 12.sp, color = themeColors.text.copy(alpha = 0.6f))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favorites) { fav ->
                                val favBook = Book(id = fav.bookId, title = fav.bookTitle, author_name = fav.bookAuthor)
                                BookItemCard(favBook, onClick = {
                                    viewModel.loadBookDetailAndToc(favBook)
                                    viewModel.loadPage(favBook.id, 1)
                                    onNavigateToReader()
                                }, themeColors = themeColors)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. SEARCH SCREEN (CARI)
@Composable
fun CariScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors, onNavigateToReader: () -> Unit) {
    var searchScopeIsPages by remember { mutableStateOf(true) } // true: Isi Kitab, false: Tanya AI
    var searchKeyword by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val useOpenAi by viewModel.useOpenAi.collectAsState()

    val useAiSearchTranslation by viewModel.useAiSearchTranslation.collectAsState()
    val isTranslatingQuery by viewModel.isTranslatingQuery.collectAsState()
    val translatedQuery by viewModel.translatedQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
    ) {
        // Top Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Pencarian & AI",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Temukan referensi dari khazanah turats secara instan atau konsultasikan dengan kecerdasan buatan",
                fontSize = 12.sp,
                color = themeColors.text.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )
        }

        // Toggle Scope Tabs (Segmented Control style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(themeColors.card, RoundedCornerShape(16.dp))
                .border(1.dp, themeColors.gold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (searchScopeIsPages) themeColors.gold else Color.Transparent)
                    .clickable { searchScopeIsPages = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageSearch,
                        contentDescription = null,
                        tint = if (searchScopeIsPages) Color.White else themeColors.text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Eksak (Isi Kitab)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (searchScopeIsPages) Color.White else themeColors.text
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!searchScopeIsPages) themeColors.gold else Color.Transparent)
                    .clickable { searchScopeIsPages = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (!searchScopeIsPages) Color.White else themeColors.text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (useOpenAi) "Tanya AI (Custom)" else "Tanya AI (Gemini)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!searchScopeIsPages) Color.White else themeColors.text
                    )
                }
            }
        }

        if (searchScopeIsPages) {
            // Exact full-text search in page database
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = {
                    searchKeyword = it
                    if (it.isBlank()) {
                        viewModel.clearSearchState()
                    } else if (!useAiSearchTranslation) {
                        viewModel.searchTextInPages(it)
                    }
                },
                placeholder = {
                    Text(
                        text = if (useAiSearchTranslation) "Ketik dalam Indonesia (misal: tata cara wudhu)..." else "Masukkan kata/kalimat Arab (misal: الطهارة)...",
                        color = themeColors.text.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.gold) },
                trailingIcon = {
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(onClick = {
                            searchKeyword = ""
                            viewModel.clearSearchState()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = themeColors.text.copy(alpha = 0.5f))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchKeyword.isNotBlank()) {
                            viewModel.searchTextInPages(searchKeyword)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.gold,
                    unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                    focusedTextColor = themeColors.text,
                    unfocusedTextColor = themeColors.text,
                    focusedContainerColor = themeColors.card,
                    unfocusedContainerColor = themeColors.card
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // AI Search Translation Control Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.updateUseAiSearchTranslation(!useAiSearchTranslation)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = if (useAiSearchTranslation) themeColors.gold else themeColors.text.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Optimasi Arab dengan AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (useAiSearchTranslation) themeColors.text else themeColors.text.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(themeColors.gold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Keren",
                            color = themeColors.gold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = useAiSearchTranslation,
                        onCheckedChange = { viewModel.updateUseAiSearchTranslation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = themeColors.gold,
                            uncheckedThumbColor = themeColors.text.copy(alpha = 0.4f),
                            uncheckedTrackColor = themeColors.bg
                        )
                    )

                    if (useAiSearchTranslation) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (searchKeyword.isNotBlank()) {
                                    viewModel.searchTextInPages(searchKeyword)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cari", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (isTranslatingQuery) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(themeColors.gold.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.gold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = themeColors.gold,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI menganalisis kueri & menerjemahkan ke Arab klasik...",
                        color = themeColors.gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (translatedQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(themeColors.card, RoundedCornerShape(12.dp))
                        .border(1.dp, themeColors.gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = themeColors.gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kueri Hasil Analisis AI (Arab Klasik):",
                            color = themeColors.gold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = translatedQuery,
                            color = themeColors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                when (val state = searchState) {
                    is SearchUiState.Idle -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageSearch,
                                contentDescription = null,
                                tint = themeColors.gold.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cari Kata dalam Kitab Klasik",
                                fontWeight = FontWeight.Bold,
                                color = themeColors.text,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Masukkan potongan teks atau istilah fikih, hadits, tafsir, dll. untuk melacak letak halaman aslinya secara instan.",
                                color = themeColors.text.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    is SearchUiState.Loading -> {
                        CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                        ) {
                            items(state.results) { res ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable {
                                            val dummyBook = Book(id = res.book_id, title = res.book_title, author_name = res.author_name)
                                            viewModel.loadBookDetailAndToc(dummyBook)
                                            viewModel.loadPage(res.book_id, res.global_order)
                                            onNavigateToReader()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = themeColors.card),
                                    border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = null,
                                                    tint = themeColors.gold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = res.book_title,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = themeColors.gold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = "Jilid ${res.part?.value ?: "-"}, Hal. ${res.page_num ?: "-"}",
                                                fontSize = 11.sp,
                                                color = themeColors.text.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Bab: ${res.section_title ?: "Tanpa Judul Bab"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.text.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Arabic Snippet text with RTL alignment
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(themeColors.bg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = stripHtml(res.snippet ?: ""),
                                                fontSize = 15.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = themeColors.text,
                                                lineHeight = 24.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Buka Halaman",
                                                fontSize = 11.sp,
                                                color = themeColors.gold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = themeColors.gold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is SearchUiState.Error -> {
                        // Intelligent AI-Assisted fallback search helper card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                                border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = themeColors.gold,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Hasil Eksak Tidak Ditemukan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = themeColors.text,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Pencarian teks \"$searchKeyword\" tidak menghasilkan kecocokan kata demi kata yang persis pada kitab database lokal.",
                                        fontSize = 12.sp,
                                        color = themeColors.text.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            searchScopeIsPages = false
                                            viewModel.sendAiMessage("Saya ingin mencari info tentang: \"$searchKeyword\" di kitab klasik. Tolong jelaskan referensinya!")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (useOpenAi) "Tanyakan ke AI Custom" else "Tanyakan ke AI Gemini", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tanya AI Assistant dedicated chat prompt layout
            AiAssistantScreen(viewModel = viewModel, themeColors = themeColors)
        }
    }
}

// 4. READER SCREEN (DASHBOARD PEMBACA)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors, onNavigateBack: () -> Unit) {
    val readerState by viewModel.readerState.collectAsState()
    val activeBook by viewModel.activeBook.collectAsState()
    val activePage by viewModel.activePage.collectAsState()
    val bookmarks by viewModel.bookmarksList.collectAsState()

    var showDisplaySettings by remember { mutableStateOf(false) }
    var showTocDialog by remember { mutableStateOf(false) }
    var showBookmarkNoteDialog by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }
    
    // Selecting Text State for floating context AI
    var selectedTextForAi by remember { mutableStateOf("") }
    var showAiExplanationSheet by remember { mutableStateOf(false) }

    var activeSelectedParagraphIndex by remember { mutableStateOf<Int?>(null) }
    var activeSelectedParagraphText by remember { mutableStateOf("") }
    var showSelectionCapsuleMenu by remember { mutableStateOf(false) }
    var showSelectionSubMenu by remember { mutableStateOf(false) }

    var showFootnotePopup by remember { mutableStateOf(false) }
    var activeFootnoteMarker by remember { mutableStateOf("") }

    LaunchedEffect(activePage) {
        activeSelectedParagraphIndex = null
        activeSelectedParagraphText = ""
        showSelectionCapsuleMenu = false
        showSelectionSubMenu = false
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeBook?.title ?: "Membaca Kitab",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activePage?.section_title ?: "Menyiapkan...",
                            fontSize = 11.sp,
                            color = themeColors.text.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = themeColors.text)
                    }
                },
                actions = {
                    // Check if current page is bookmarked
                    val isBookmarked = bookmarks.any { b ->
                        b.bookId == activePage?.book_id && b.globalOrder == activePage?.global_order
                    }
                    IconButton(onClick = {
                        if (isBookmarked) {
                            val target = bookmarks.first { b ->
                                b.bookId == activePage?.book_id && b.globalOrder == activePage?.global_order
                            }
                            viewModel.removeBookmark(target.id)
                        } else {
                            showBookmarkNoteDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) themeColors.gold else themeColors.text
                        )
                    }
                    IconButton(onClick = { showTocDialog = true }) {
                        Icon(Icons.Default.List, contentDescription = "Daftar Isi", tint = themeColors.text)
                    }
                    IconButton(onClick = { showDisplaySettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = themeColors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.card)
            )
        },
        bottomBar = {
            activePage?.let { page ->
                BottomAppBar(
                    containerColor = themeColors.card,
                    modifier = Modifier.height(72.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            enabled = page.navigation?.prevGlobalOrder != null,
                            onClick = {
                                page.navigation?.prevGlobalOrder?.let {
                                    viewModel.loadPage(page.book_id, it)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Sebelumnya", tint = themeColors.text)
                        }

                        // Jilid and Halaman Display
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Jilid ${page.part?.value ?: "-"}, Hal. ${page.page_num}",
                                fontWeight = FontWeight.Bold,
                                color = themeColors.text,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Indeks Global: ${page.global_order}",
                                fontSize = 10.sp,
                                color = themeColors.text.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            enabled = page.navigation?.hasNext == true && page.navigation?.nextGlobalOrder != null,
                            onClick = {
                                page.navigation?.nextGlobalOrder?.let {
                                    viewModel.loadPage(page.book_id, it)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Selanjutnya", tint = themeColors.text)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            activePage?.let { page ->
                FloatingActionButton(
                    onClick = {
                        selectedTextForAi = stripHtml(page.content)
                        viewModel.explainText(selectedTextForAi, "makna")
                        showAiExplanationSheet = true
                    },
                    containerColor = themeColors.gold,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.padding(bottom = 16.dp).testTag("sparkle_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant Sparkle",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = themeColors.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = readerState) {
                is ReaderUiState.Loading -> {
                    CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.align(Alignment.Center))
                }
                is ReaderUiState.Success -> {
                    val page = state.page
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Book Title watermarked or decorated header
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.gold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        // Highly Polished Arabic reading content
                        val fontSize by viewModel.readerFontSize.collectAsState()
                        val lineSpacing by viewModel.readerLineSpacing.collectAsState()
                        val fontFamilyStr by viewModel.readerFontFamily.collectAsState()
                        val fontSelection = when (fontFamilyStr) {
                            "Amiri" -> AmiriFontFamily
                            "Scheherazade" -> ScheherazadeNewFontFamily
                            "Noto Naskh" -> NotoNaskhArabicFontFamily
                            "Cairo" -> CairoFontFamily
                            "Lateef" -> LateefFontFamily
                            "Sans-serif" -> FontFamily.SansSerif
                            "Serif" -> FontFamily.Serif
                            else -> FontFamily.Default
                        }

                        val highlights by viewModel.highlightsList.collectAsState()
                        val processedLines = remember(page.content, fontSize, themeColors) {
                            val titleRegex = """<span\s+data-type=['"]title['"][^>]*>(.*?)</span>""".toRegex(RegexOption.IGNORE_CASE)
                            val rawLines = page.content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
                            
                            rawLines.map { line ->
                                val trimmedLine = line.trim()
                                val titleMatch = titleRegex.find(trimmedLine)
                                
                                if (titleMatch != null) {
                                    val titleText = stripHtml(titleMatch.groupValues[1]).trim()
                                    val isLineBismillah = titleText.length < 100 && (
                                        titleText.contains("بِسْمِ اللَّهِ") ||
                                        titleText.contains("بِسْمِ اللهِ") ||
                                        titleText.startsWith("بسم الله")
                                    )
                                    ProcessedLine(
                                        originalText = trimmedLine,
                                        isTitle = true,
                                        isBismillah = isLineBismillah,
                                        cleanText = titleText,
                                        annotatedText = AnnotatedString(titleText)
                                    )
                                } else {
                                    val cleanText = stripHtml(trimmedLine).trim()
                                    val isLineBismillah = cleanText.length < 100 && (
                                        cleanText.contains("بِسْمِ اللَّهِ") ||
                                        cleanText.contains("بِسْمِ اللهِ") ||
                                        cleanText.startsWith("بسم الله")
                                    )
                                    val isHeuristicHeader = isLineBismillah || (cleanText.length < 80 && (
                                        cleanText.contains("باب") ||
                                        cleanText.contains("الباب") ||
                                        cleanText.contains("فصل") ||
                                        cleanText.contains("الفصل") ||
                                        cleanText.contains("كتاب") ||
                                        cleanText.contains("كِتَابُ") ||
                                        cleanText.contains("مسألة") ||
                                        cleanText.contains("تنبيه") ||
                                        cleanText.contains("فرع") ||
                                        cleanText.contains("خatمه") ||
                                        cleanText.contains("خاتمة") ||
                                        (cleanText.startsWith("[") && cleanText.endsWith("]")) ||
                                        (cleanText.startsWith("(") && cleanText.endsWith(")"))
                                    ))
                                    
                                    ProcessedLine(
                                        originalText = trimmedLine,
                                        isTitle = isHeuristicHeader,
                                        isBismillah = isLineBismillah,
                                        cleanText = cleanText,
                                        annotatedText = if (isHeuristicHeader) {
                                            AnnotatedString(cleanText)
                                        } else {
                                            parseArabicTextToAnnotatedString(cleanText, themeColors, fontSize)
                                        }
                                    )
                                }
                            }.filter { it.cleanText.isNotEmpty() }
                        }
                        val context = LocalContext.current
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                processedLines.forEachIndexed { idx, line ->
                                    if (line.isTitle) {
                                        // Judul Bab / Sub Bab / Fasl / Bismillah
                                        val isMajor = line.cleanText.contains("باب") || line.cleanText.contains("الباب") || line.cleanText.contains("كتاب") || line.cleanText.contains("كِتَابُ")
                                        val headingText = if (line.isBismillah) {
                                            line.cleanText
                                        } else if (isMajor) {
                                            "❖ ${line.cleanText} ❖"
                                        } else {
                                            "〔 ${line.cleanText} 〕"
                                        }
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = headingText,
                                                fontSize = if (line.isBismillah) (fontSize + 4).sp else if (isMajor) (fontSize + 3).sp else (fontSize + 1).sp,
                                                color = themeColors.gold,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = fontSelection,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                            )
                                            if (!line.isBismillah) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .height(1.dp)
                                                        .background(themeColors.gold.copy(alpha = 0.3f))
                                                )
                                            }
                                        }
                                    } else {
                                        // Standard Body Paragraph (Clickable & Selectable)
                                        val isSelected = activeSelectedParagraphIndex == idx
                                        val isHighlighted = highlights.any { h ->
                                            h.bookId == page.book_id && h.globalOrder == page.global_order && h.text.trim() == line.cleanText
                                        }
                                        
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Handle Capsule Menu displaying right above the selected paragraph
                                            if (isSelected) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    // Main Capsule Menu
                                                    Card(
                                                        shape = RoundedCornerShape(50.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            TextButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString(line.cleanText))
                                                                    Toast.makeText(context, "Teks disalin", Toast.LENGTH_SHORT).show()
                                                                    activeSelectedParagraphIndex = null
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Salin", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            
                                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))
                                                            
                                                            TextButton(
                                                                onClick = {
                                                                    viewModel.addHighlight(line.cleanText)
                                                                    Toast.makeText(context, "Highlight ditambahkan", Toast.LENGTH_SHORT).show()
                                                                    activeSelectedParagraphIndex = null
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Highlight", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            
                                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))
                                                            
                                                            TextButton(
                                                                onClick = {
                                                                    bookmarkNoteText = "Kutipan: ${line.cleanText.take(30)}..."
                                                                    showBookmarkNoteDialog = true
                                                                    activeSelectedParagraphIndex = null
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Catatan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            
                                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))
                                                            
                                                            TextButton(
                                                                onClick = {
                                                                    selectedTextForAi = line.cleanText
                                                                    viewModel.explainText(line.cleanText, "makna")
                                                                    showAiExplanationSheet = true
                                                                    activeSelectedParagraphIndex = null
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("AI Jelaskan", color = themeColors.gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                            }
                                                            
                                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))
                                                            
                                                            TextButton(
                                                                onClick = {
                                                                    showSelectionSubMenu = !showSelectionSubMenu
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(if (showSelectionSubMenu) "Tutup" else "Lainnya", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    
                                                    // Secondary Sub-menu for specialized analyses
                                                    if (showSelectionSubMenu) {
                                                        Card(
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                                            modifier = Modifier.padding(bottom = 6.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                val options = listOf(
                                                                    Triple("translate", "Terjemah", Icons.Default.Translate),
                                                                    Triple("nahwu", "Nahwu", Icons.Default.TextFields),
                                                                    Triple("irab", "I'rab", Icons.Default.Subject),
                                                                    Triple("fiqh", "Fikih", Icons.Default.Gavel)
                                                                )
                                                                options.forEachIndexed { optIdx, (type, label, icon) ->
                                                                    if (optIdx > 0) {
                                                                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color(0x22FFFFFF)))
                                                                    }
                                                                    TextButton(
                                                                        onClick = {
                                                                            selectedTextForAi = line.cleanText
                                                                            viewModel.explainText(line.cleanText, type)
                                                                            showAiExplanationSheet = true
                                                                            activeSelectedParagraphIndex = null
                                                                            showSelectionSubMenu = false
                                                                        },
                                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Icon(icon, contentDescription = null, tint = themeColors.gold, modifier = Modifier.size(11.dp))
                                                                        Spacer(modifier = Modifier.width(3.dp))
                                                                        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // The Paragraph box
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> themeColors.gold.copy(alpha = 0.15f)
                                                            isHighlighted -> Color(0xFFFFF176).copy(alpha = 0.35f) // Glowing soft yellow highlighter
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) themeColors.gold.copy(alpha = 0.4f) else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (isSelected) {
                                                            activeSelectedParagraphIndex = null
                                                            showSelectionSubMenu = false
                                                        } else {
                                                            activeSelectedParagraphIndex = idx
                                                            showSelectionSubMenu = false
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                // Decorative Select Handle (teardrop) on Selected Paragraph to make it highly authentic!
                                                if (isSelected) {
                                                    // Start handle (top-right in RTL)
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 4.dp, y = (-4).dp)
                                                            .size(8.dp)
                                                            .background(themeColors.gold, RoundedCornerShape(50.dp))
                                                    )
                                                    // End handle (bottom-left in RTL)
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .offset(x = (-4).dp, y = 4.dp)
                                                            .size(8.dp)
                                                            .background(themeColors.gold, RoundedCornerShape(50.dp))
                                                    )
                                                }
                                                
                                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                                Text(
                                                    text = line.annotatedText,
                                                    fontSize = fontSize.sp,
                                                    color = themeColors.text,
                                                    lineHeight = (fontSize * lineSpacing).sp,
                                                    textAlign = TextAlign.Justify,
                                                    fontFamily = fontSelection,
                                                    fontWeight = FontWeight.Medium,
                                                    onTextLayout = { layoutResult = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .pointerInput(line.annotatedText) {
                                                            detectTapGestures { offsetPosition ->
                                                                layoutResult?.let { layout ->
                                                                    val offset = layout.getOffsetForPosition(offsetPosition)
                                                                    val annotations = line.annotatedText.getStringAnnotations(
                                                                        tag = "footnote",
                                                                        start = offset,
                                                                        end = offset
                                                                    )
                                                                    val annotation = annotations.firstOrNull()
                                                                    if (annotation != null) {
                                                                        activeFootnoteMarker = annotation.item
                                                                        showFootnotePopup = true
                                                                    } else {
                                                                        if (isSelected) {
                                                                            activeSelectedParagraphIndex = null
                                                                            showSelectionSubMenu = false
                                                                        } else {
                                                                            activeSelectedParagraphIndex = idx
                                                                            showSelectionSubMenu = false
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Catatan Kaki / Footnote layered beautifully at the bottom with a distinct styled container card
                        page.footnote?.takeIf { it.isNotBlank() && it != "None" }?.let { ft ->
                            Spacer(modifier = Modifier.height(32.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, themeColors.gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = themeColors.card.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = themeColors.gold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "CATATAN KAKI (FOOTNOTE)",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = themeColors.gold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = themeColors.gold.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        val rawFootnoteSize = if (fontSize - 5f < 11f) 11f else fontSize - 5f
                                        val rawFootnoteLineHeight = rawFootnoteSize * 1.5f
                                        val footnoteLines = remember(ft, rawFootnoteSize, themeColors) {
                                            ft.replace("\r\n", "\n").replace("\r", "\n").split("\n")
                                                .map { stripHtml(it).trim() }
                                                .filter { it.isNotEmpty() }
                                                .map { parseArabicTextToAnnotatedString(it, themeColors, rawFootnoteSize) }
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            footnoteLines.forEach { fnLine ->
                                                Text(
                                                    text = fnLine,
                                                    fontSize = rawFootnoteSize.sp,
                                                    color = themeColors.text.copy(alpha = 0.85f),
                                                    lineHeight = rawFootnoteLineHeight.sp,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Justify,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is ReaderUiState.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is ReaderUiState.Idle -> {
                    Text(text = "Silakan pilih kitab dari perpustakaan.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    // Displays settings panel dialog
    if (showDisplaySettings) {
        Dialog(onDismissRequest = { showDisplaySettings = false }) {
            val fSize by viewModel.readerFontSize.collectAsState()
            val lSpacing by viewModel.readerLineSpacing.collectAsState()
            val rTheme by viewModel.readerTheme.collectAsState()
            val rFont by viewModel.readerFontFamily.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.gold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pengaturan Tampilan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = themeColors.text
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Ukuran Teks Slider
                    Text(
                        text = "Ukuran Teks: ${fSize.toInt()} sp",
                        fontSize = 14.sp,
                        color = themeColors.text
                    )
                    Slider(
                        value = fSize,
                        onValueChange = { viewModel.readerFontSize.value = it },
                        valueRange = 16f..36f,
                        colors = SliderDefaults.colors(thumbColor = themeColors.gold, activeTrackColor = themeColors.gold)
                    )

                    // Jarak Baris Slider
                    Text(
                        text = "Jarak Baris: ${String.format("%.1f", lSpacing)}",
                        fontSize = 14.sp,
                        color = themeColors.text
                    )
                    Slider(
                        value = lSpacing,
                        onValueChange = { viewModel.readerLineSpacing.value = it },
                        valueRange = 1.2f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = themeColors.gold, activeTrackColor = themeColors.gold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tema (Sepia, Light, Dark)
                    Text("Pilih Tema", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Sepia", "Light", "Dark").forEach { t ->
                            Button(
                                onClick = { viewModel.readerTheme.value = t },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (rTheme == t) themeColors.gold else themeColors.text.copy(alpha = 0.1f),
                                    contentColor = if (rTheme == t) Color.White else themeColors.text
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(t)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Jenis Huruf (Font Family)
                    Text("Pilih Jenis Huruf", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.text)
                    val fontsList = remember {
                        listOf(
                            "Amiri" to "Amiri",
                            "Scheherazade" to "Scheherazade",
                            "Noto Naskh" to "Noto Naskh",
                            "Cairo" to "Cairo",
                            "Lateef" to "Lateef",
                            "Bawaan" to "Default"
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(fontsList) { (displayName, fontVal) ->
                            val isSelected = rFont == fontVal
                            Button(
                                onClick = {
                                    viewModel.readerFontFamily.value = fontVal
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) themeColors.gold else themeColors.text.copy(alpha = 0.1f),
                                    contentColor = if (isSelected) Color.White else themeColors.text
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDisplaySettings = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesai")
                    }
                }
            }
        }
    }

    // Bookmark note creator dialog
    if (showBookmarkNoteDialog) {
        Dialog(onDismissRequest = { showBookmarkNoteDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tambahkan Catatan Bookmark", fontWeight = FontWeight.Bold, color = themeColors.text)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookmarkNoteText,
                        onValueChange = { bookmarkNoteText = it },
                        placeholder = { Text("Tulis catatan opsional...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBookmarkNoteDialog = false }) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                viewModel.addBookmark(bookmarkNoteText)
                                bookmarkNoteText = ""
                                showBookmarkNoteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold)
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    // Footnote popup modal
    if (showFootnotePopup) {
        val fSize by viewModel.readerFontSize.collectAsState()
        val rFont by viewModel.readerFontFamily.collectAsState()
        val fSelection = when (rFont) {
            "Amiri" -> AmiriFontFamily
            "Scheherazade" -> ScheherazadeNewFontFamily
            "Noto Naskh" -> NotoNaskhArabicFontFamily
            "Cairo" -> CairoFontFamily
            "Lateef" -> LateefFontFamily
            "Sans-serif" -> FontFamily.SansSerif
            "Serif" -> FontFamily.Serif
            else -> FontFamily.Default
        }
        val footnoteText = activePage?.footnote
        val matchedFootnote = remember(footnoteText, activeFootnoteMarker) {
            findMatchingFootnote(footnoteText, activeFootnoteMarker)
        }
        
        Dialog(onDismissRequest = { showFootnotePopup = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, themeColors.gold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Title bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = themeColors.gold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Catatan Kaki (¬$activeFootnoteMarker)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = themeColors.text,
                                    fontFamily = fSelection
                                )
                            }
                            IconButton(
                                onClick = { showFootnotePopup = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = themeColors.text.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Matched footnote highlighted container
                        if (matchedFootnote != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = themeColors.gold.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Catatan Terpilih:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.gold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = stripHtml(matchedFootnote),
                                        fontSize = (fSize - 1f).coerceAtLeast(14f).sp,
                                        lineHeight = ((fSize - 1f).coerceAtLeast(14f) * 1.5f).sp,
                                        color = themeColors.text,
                                        fontFamily = fSelection,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Justify,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Full footnotes list scrollable container for academic context
                        Text(
                            text = "Semua Catatan Kaki Halaman ini:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.text.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val parsedAllFootnotes = remember(footnoteText) {
                            footnoteText?.replace("\r\n", "\n")?.replace("\r", "\n")?.split("\n")
                                ?.map { stripHtml(it).trim() }
                                ?.filter { it.isNotEmpty() }
                                ?: emptyList()
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 220.dp)
                                .fillMaxWidth()
                                .border(1.dp, themeColors.text.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .background(themeColors.text.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (parsedAllFootnotes.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada catatan kaki pada halaman ini.",
                                        fontSize = 14.sp,
                                        color = themeColors.text.copy(alpha = 0.5f),
                                        fontFamily = fSelection
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(parsedAllFootnotes) { fnLine ->
                                        val isCurrent = fnLine == matchedFootnote
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isCurrent) themeColors.gold.copy(alpha = 0.05f) else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    width = if (isCurrent) 1.dp else 0.dp,
                                                    color = if (isCurrent) themeColors.gold.copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = fnLine,
                                                fontSize = (fSize - 2f).coerceAtLeast(12f).sp,
                                                lineHeight = ((fSize - 2f).coerceAtLeast(12f) * 1.5f).sp,
                                                color = if (isCurrent) themeColors.text else themeColors.text.copy(alpha = 0.8f),
                                                fontFamily = fSelection,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Justify,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Dismiss action button
                        Button(
                            onClick = { showFootnotePopup = false },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tutup",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Table of contents Dialog
    if (showTocDialog) {
        val tocState by viewModel.tocState.collectAsState()
        var selectedTocTab by remember { mutableStateOf("struktur") } // "struktur" or "halaman"
        var jumpPageInput by remember { mutableStateOf("") }
        var expandedTocIds by remember { mutableStateOf(setOf<Int>()) }

        Dialog(onDismissRequest = { showTocDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.gold),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(vertical = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daftar Isi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = themeColors.text,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Tab Selector: Struktur & Halaman
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(themeColors.text.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedTocTab = "struktur" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTocTab == "struktur") themeColors.gold else Color.Transparent,
                                contentColor = if (selectedTocTab == "struktur") Color.White else themeColors.text
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Struktur", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { selectedTocTab = "halaman" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTocTab == "halaman") themeColors.gold else Color.Transparent,
                                contentColor = if (selectedTocTab == "halaman") Color.White else themeColors.text
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Halaman", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTocTab == "struktur") {
                            when (val state = tocState) {
                                is TocUiState.Loading -> {
                                    CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.align(Alignment.Center))
                                }
                                is TocUiState.Success -> {
                                    fun getVisibleTocItems(items: List<TocItem>, expandedIds: Set<Int>): List<TocItem> {
                                        val result = mutableListOf<TocItem>()
                                        fun traverse(item: TocItem) {
                                            result.add(item)
                                            val id = item.title_id ?: 0
                                            if (expandedIds.contains(id) && !item.children.isNullOrEmpty()) {
                                                item.children.forEach { traverse(it) }
                                            }
                                        }
                                        items.forEach { traverse(it) }
                                        return result
                                    }

                                    val visibleItems = getVisibleTocItems(state.toc, expandedTocIds)
                                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                                    val collapsedIcon = if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight

                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(visibleItems) { item ->
                                            val hasChildren = !item.children.isNullOrEmpty()
                                            val id = item.title_id ?: 0
                                            val isExpanded = expandedTocIds.contains(id)
                                            val indent = (item.level * 16).dp

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (hasChildren && item.global_order == 0) {
                                                            expandedTocIds = if (isExpanded) {
                                                                expandedTocIds - id
                                                            } else {
                                                                expandedTocIds + id
                                                            }
                                                        } else {
                                                            viewModel.loadPageFromToc(item.id, item)
                                                            showTocDialog = false
                                                        }
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                                                    .padding(start = indent),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasChildren) {
                                                    IconButton(
                                                        onClick = {
                                                            expandedTocIds = if (isExpanded) {
                                                                expandedTocIds - id
                                                            } else {
                                                                expandedTocIds + id
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else collapsedIcon,
                                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                            tint = themeColors.gold,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.MenuBook,
                                                        contentDescription = null,
                                                        tint = themeColors.gold.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(16.dp).padding(2.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                }

                                                Text(
                                                    text = item.title,
                                                    fontSize = if (item.level == 0) 15.sp else 14.sp,
                                                    color = themeColors.text,
                                                    fontWeight = if (item.level == 0) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            HorizontalDivider(color = themeColors.gold.copy(alpha = 0.08f))
                                        }
                                    }
                                }
                                is TocUiState.Error -> {
                                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        } else {
                            // Halaman Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Jump form
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = jumpPageInput,
                                        onValueChange = { jumpPageInput = it },
                                        placeholder = { Text("No. Halaman", fontSize = 12.sp, color = themeColors.text.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = themeColors.gold,
                                            unfocusedBorderColor = themeColors.gold.copy(alpha = 0.4f),
                                            focusedTextColor = themeColors.text
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val pNum = jumpPageInput.toIntOrNull()
                                            if (pNum != null && activeBook != null) {
                                                viewModel.jumpToPage(activeBook!!.id, null, pNum)
                                                showTocDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Lompat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "Pilih Halaman Cetak:",
                                    fontSize = 11.sp,
                                    color = themeColors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val maxPages = activeBook?.page_count?.takeIf { it > 0 } ?: 200
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(54.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(maxPages) { idx ->
                                        val pageNum = idx + 1
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .background(themeColors.text.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (activeBook != null) {
                                                        viewModel.jumpToPage(activeBook!!.id, null, pageNum)
                                                        showTocDialog = false
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pageNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = themeColors.text
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showTocDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    // AI Explanation Bottom Sheet / Custom Dialog
    if (showAiExplanationSheet) {
        val explanation by viewModel.aiExplanation.collectAsState()
        val isExplaining by viewModel.isExplainingText.collectAsState()
        var activeAnalysisType by remember { mutableStateOf("makna") }

        // Trigger analysis when tab changes or initially
        LaunchedEffect(activeAnalysisType) {
            viewModel.explainText(selectedTextForAi, activeAnalysisType)
        }

        Dialog(onDismissRequest = { showAiExplanationSheet = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.card),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, themeColors.gold),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = themeColors.gold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Assistant Shamela", fontWeight = FontWeight.Bold, color = themeColors.text, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showAiExplanationSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = themeColors.text)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stylized Quote Box for Arabic text selection
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.text.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Teks Terpilih:",
                                fontSize = 11.sp,
                                color = themeColors.gold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = selectedTextForAi,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 16.sp,
                                    color = themeColors.text,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Horizontal Scrollable Row of 5 specialized tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val analyses = listOf(
                            Triple("makna", "Makna", Icons.Default.AutoAwesome),
                            Triple("translate", "Terjemah", Icons.Default.Translate),
                            Triple("nahwu", "Nahwu", Icons.Default.TextFields),
                            Triple("irab", "I'rab", Icons.Default.Subject),
                            Triple("fiqh", "Fikih", Icons.Default.Gavel)
                        )

                        analyses.forEach { (type, label, icon) ->
                            val isSelected = activeAnalysisType == type
                            Button(
                                onClick = { activeAnalysisType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) themeColors.gold else themeColors.text.copy(alpha = 0.05f),
                                    contentColor = if (isSelected) Color.White else themeColors.text
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content Box with proper scrolling
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(themeColors.text.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                            .border(1.dp, themeColors.text.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (isExplaining) {
                            val loadingText = when (activeAnalysisType) {
                                "translate" -> "Menerjemahkan teks ke Bahasa Indonesia..."
                                "nahwu" -> "Menganalisis kaidah Nahwu, Sharaf & Balaghah..."
                                "irab" -> "Menyusun tabel/poin analisis I'rab detail..."
                                "fiqh" -> "Mengekstrak hukum fikih & pandangan madzhab..."
                                else -> "Menganalisis makna umum & pesan teks..."
                            }
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = themeColors.gold, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = loadingText,
                                    fontSize = 12.sp,
                                    color = themeColors.text.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val currentExplanation = explanation
                                if (currentExplanation.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = themeColors.gold)
                                    }
                                } else {
                                    MarkdownText(
                                        markdown = currentExplanation,
                                        themeColors = themeColors,
                                        fontSize = 14f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showAiExplanationSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Selesai", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom Markdown Renderer with Arabic script optimizations
fun isArabicText(text: String): Boolean {
    return text.any { it.code in 0x0600..0x06FF }
}

@Composable
fun MarkdownText(
    markdown: String,
    themeColors: ReaderThemeColors,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f
) {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            
            // Check for code blocks
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Render accumulated code block
                    CodeBlockCard(codeBlockContent.toString().trim(), themeColors)
                    codeBlockContent = StringBuilder()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
                continue
            }

            // Headers
            if (trimmed.startsWith("# ")) {
                val text = trimmed.removePrefix("# ").trim()
                Text(
                    text = text,
                    fontSize = (fontSize + 6).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColors.gold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                continue
            }
            if (trimmed.startsWith("## ")) {
                val text = trimmed.removePrefix("## ").trim()
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                    Text(
                        text = text,
                        fontSize = (fontSize + 4).sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.gold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.width(60.dp).height(2.dp).background(themeColors.gold.copy(alpha = 0.5f)))
                }
                continue
            }
            if (trimmed.startsWith("### ")) {
                val text = trimmed.removePrefix("### ").trim()
                Text(
                    text = text,
                    fontSize = (fontSize + 2).sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.text,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                continue
            }

            // Blockquotes
            if (trimmed.startsWith(">")) {
                val text = trimmed.removePrefix(">").trim()
                BlockquoteCard(text, themeColors, fontSize)
                continue
            }

            // Bullet Lists
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val text = trimmed.substring(2).trim()
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = (fontSize + 2).sp,
                        color = themeColors.gold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (isArabicText(text)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = parseMarkdownInline(text, themeColors),
                                fontSize = (fontSize + 2).sp,
                                fontFamily = FontFamily.Serif,
                                color = themeColors.text,
                                lineHeight = ((fontSize + 2) * 1.5).sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = parseMarkdownInline(text, themeColors),
                            fontSize = fontSize.sp,
                            color = themeColors.text,
                            lineHeight = (fontSize * 1.5).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                continue
            }

            // Numbered Lists
            val numberedListMatch = "^(\\d+)\\.\\s+(.*)$".toRegex().find(trimmed)
            if (numberedListMatch != null) {
                val num = numberedListMatch.groupValues[1]
                val text = numberedListMatch.groupValues[2]
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "$num.",
                        fontSize = fontSize.sp,
                        color = themeColors.gold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (isArabicText(text)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = parseMarkdownInline(text, themeColors),
                                fontSize = (fontSize + 2).sp,
                                fontFamily = FontFamily.Serif,
                                color = themeColors.text,
                                lineHeight = ((fontSize + 2) * 1.5).sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = parseMarkdownInline(text, themeColors),
                            fontSize = fontSize.sp,
                            color = themeColors.text,
                            lineHeight = (fontSize * 1.5).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                continue
            }

            // Standard Paragraph
            if (trimmed.isNotEmpty()) {
                if (isArabicText(trimmed)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = parseMarkdownInline(trimmed, themeColors),
                            fontSize = (fontSize + 3).sp,
                            fontFamily = FontFamily.Serif,
                            color = themeColors.text,
                            lineHeight = ((fontSize + 3) * 1.6).sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = parseMarkdownInline(trimmed, themeColors),
                        fontSize = fontSize.sp,
                        color = themeColors.text,
                        lineHeight = (fontSize * 1.5).sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        
        // Render unclosed code block if any
        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            CodeBlockCard(codeBlockContent.toString().trim(), themeColors)
        }
    }
}

@Composable
fun CodeBlockCard(code: String, themeColors: ReaderThemeColors) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.text.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SelectionContainer {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = themeColors.text,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BlockquoteCard(text: String, themeColors: ReaderThemeColors, fontSize: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(themeColors.text.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                drawLine(
                    color = themeColors.gold,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isArabicText(text)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = parseMarkdownInline(text, themeColors),
                    fontSize = (fontSize + 2).sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = themeColors.text.copy(alpha = 0.85f),
                    lineHeight = ((fontSize + 2) * 1.5).sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                text = parseMarkdownInline(text, themeColors),
                fontSize = fontSize.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = themeColors.text.copy(alpha = 0.85f),
                lineHeight = (fontSize * 1.4).sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun parseMarkdownInline(text: String, themeColors: ReaderThemeColors): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Check for bold (**)
            val boldIndex = text.indexOf("**", cursor)
            // Check for code (`)
            val codeIndex = text.indexOf("`", cursor)
            
            val nextSpecial = when {
                boldIndex != -1 && codeIndex != -1 -> minOf(boldIndex, codeIndex)
                boldIndex != -1 -> boldIndex
                codeIndex != -1 -> codeIndex
                else -> -1
            }

            if (nextSpecial == -1) {
                append(text.substring(cursor))
                break
            }

            // Append plain text before special token
            if (nextSpecial > cursor) {
                append(text.substring(cursor, nextSpecial))
                cursor = nextSpecial
            }

            if (cursor == boldIndex) {
                val endBold = text.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldText = text.substring(cursor + 2, endBold)
                    val startPos = this.length
                    append(boldText)
                    addStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold, color = themeColors.text),
                        start = startPos,
                        end = this.length
                    )
                    cursor = endBold + 2
                } else {
                    append("**")
                    cursor += 2
                }
            } else if (cursor == codeIndex) {
                val endCode = text.indexOf("`", cursor + 1)
                if (endCode != -1) {
                    val codeText = text.substring(cursor + 1, endCode)
                    val startPos = this.length
                    append(codeText)
                    addStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = themeColors.text.copy(alpha = 0.1f),
                            color = themeColors.gold,
                            fontWeight = FontWeight.Medium
                        ),
                        start = startPos,
                        end = this.length
                    )
                    cursor = endCode + 1
                } else {
                    append("`")
                    cursor += 1
                }
            }
        }
    }
}

// 5. AI ASSISTANT DIRECT CHAT LAYOUT
@Composable
fun AiAssistantScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors) {
    val chatHistory by viewModel.aiChatHistory.collectAsState()
    val isTyping by viewModel.isAiTyping.collectAsState()
    var userMessageText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when history size changes or when the last message's text updates
    LaunchedEffect(chatHistory.size, chatHistory.lastOrNull()?.second?.length) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
    ) {
        // Chat Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatHistory) { message ->
                val isUser = message.first == "user"
                val textContent = message.second
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!isUser) {
                            // Robot/AI Avatar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(themeColors.gold.copy(alpha = 0.1f))
                                    .border(1.dp, themeColors.gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = themeColors.gold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Column(
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) themeColors.gold else themeColors.card
                                ),
                                shape = RoundedCornerShape(
                                    topStart = if (isUser) 16.dp else 4.dp,
                                    topEnd = if (isUser) 4.dp else 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                ),
                                border = if (!isUser) BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.15f)) else null,
                                modifier = Modifier.shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = if (isUser) 16.dp else 4.dp,
                                        topEnd = if (isUser) 4.dp else 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    if (isUser) {
                                        Text(
                                            text = textContent,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            lineHeight = 20.sp
                                        )
                                    } else {
                                        if (textContent.isEmpty()) {
                                            // Typing skeleton or indicator
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = themeColors.gold,
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Asisten sedang merumuskan jawaban...",
                                                    fontSize = 12.sp,
                                                    color = themeColors.text.copy(alpha = 0.5f)
                                                )
                                            }
                                        } else {
                                            MarkdownText(
                                                markdown = textContent,
                                                themeColors = themeColors,
                                                fontSize = 14f
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUser) "Anda" else "Asisten AI Shamela",
                                fontSize = 10.sp,
                                color = themeColors.text.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isTyping && chatHistory.lastOrNull()?.first == "user") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.gold,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Menghubungi AI...",
                            fontSize = 12.sp,
                            color = themeColors.text.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Suggestions row for quick questions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.bg)
                .padding(vertical = 4.dp)
        ) {
            HorizontalDivider(color = themeColors.gold.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "Bagaimana hukum air yang berubah warna karena daun?",
                    "Apa perbedaan Hadits Shahih dan Hasan secara singkat?",
                    "Jelaskan rukun wudhu secara singkat",
                    "Bagaimana cara menganalisis I'rab kalimat?"
                )
                items(suggestions) { sug ->
                    Card(
                        modifier = Modifier
                            .clickable { viewModel.sendAiMessage(sug) }
                            .padding(bottom = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.card),
                        border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = sug,
                            fontSize = 11.sp,
                            color = themeColors.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Input Field Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier
                    .size(44.dp)
                    .background(themeColors.card, RoundedCornerShape(22.dp))
                    .border(1.dp, themeColors.gold.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = themeColors.text.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = userMessageText,
                onValueChange = { userMessageText = it },
                placeholder = { Text("Tanyakan kandungan kitab...", color = themeColors.text.copy(alpha = 0.4f)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.gold,
                    unfocusedBorderColor = themeColors.gold.copy(alpha = 0.4f),
                    focusedContainerColor = themeColors.card,
                    unfocusedContainerColor = themeColors.card,
                    focusedTextColor = themeColors.text,
                    unfocusedTextColor = themeColors.text
                ),
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendAiMessage(userMessageText)
                        userMessageText = ""
                    }
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendAiMessage(userMessageText)
                        userMessageText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(themeColors.gold, RoundedCornerShape(22.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp)) // Standard small bottom breathing room
    }
}

// 6. PROFIL SCREEN (PROFIL)
@Composable
fun ProfilScreen(viewModel: ShamelaViewModel, themeColors: ReaderThemeColors) {
    val progressList by viewModel.readingProgressList.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()
    val bookmarks by viewModel.bookmarksList.collectAsState()
    val highlights by viewModel.highlightsList.collectAsState()

    val useOpenAi by viewModel.useOpenAi.collectAsState()
    val openAiApiKey by viewModel.openAiApiKey.collectAsState()
    val openAiBaseUrl by viewModel.openAiBaseUrl.collectAsState()
    val openAiModel by viewModel.openAiModel.collectAsState()
    val openAiAvailableModels by viewModel.openAiAvailableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val fetchModelsError by viewModel.fetchModelsError.collectAsState()

    val useMcp by viewModel.useMcp.collectAsState()
    val mcpBaseUrl by viewModel.mcpBaseUrl.collectAsState()
    val mcpStatusText by viewModel.mcpStatusText.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(45.dp))
                .background(themeColors.card)
                .border(2.dp, themeColors.gold, RoundedCornerShape(45.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = themeColors.gold,
                modifier = Modifier.size(54.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ahmad Dzakwan",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.text
        )
        Text(
            text = "ahmad.dzakwan@turats.id",
            fontSize = 13.sp,
            color = themeColors.text.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats card Grid row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = themeColors.card)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "${progressList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColors.gold)
                    Text(text = "Sedang Dibaca", fontSize = 11.sp, color = themeColors.text.copy(alpha = 0.6f))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = themeColors.card)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "${favorites.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColors.gold)
                    Text(text = "Favorit", fontSize = 11.sp, color = themeColors.text.copy(alpha = 0.6f))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = themeColors.card)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "${bookmarks.size + highlights.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColors.gold)
                    Text(text = "Mark & Highlight", fontSize = 11.sp, color = themeColors.text.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Configuration Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.card),
            border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = themeColors.gold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Gunakan OpenAI-Compatible AI",
                                fontWeight = FontWeight.Bold,
                                color = themeColors.text,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (useOpenAi) "Menggunakan OpenAI/Sumopod/Custom API" else "Menggunakan Gemini API bawaan",
                                color = themeColors.text.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = useOpenAi,
                        onCheckedChange = { viewModel.updateUseOpenAi(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = themeColors.gold,
                            uncheckedThumbColor = themeColors.text.copy(alpha = 0.4f),
                            uncheckedTrackColor = themeColors.bg
                        )
                    )
                }

                if (useOpenAi) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = themeColors.gold.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // URL Base
                    OutlinedTextField(
                        value = openAiBaseUrl,
                        onValueChange = { viewModel.updateOpenAiBaseUrl(it) },
                        label = { Text("Base URL API", color = themeColors.gold, fontSize = 12.sp) },
                        placeholder = { Text("https://ai.sumopod.com/v1", color = themeColors.text.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.gold,
                            unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text,
                            focusedContainerColor = themeColors.bg.copy(alpha = 0.3f),
                            unfocusedContainerColor = themeColors.bg.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key
                    OutlinedTextField(
                        value = openAiApiKey,
                        onValueChange = { viewModel.updateOpenAiApiKey(it) },
                        label = { Text("API Key", color = themeColors.gold, fontSize = 12.sp) },
                        placeholder = { Text("sk-...", color = themeColors.text.copy(alpha = 0.4f)) },
                        visualTransformation = if (apiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Sembunyikan" else "Tampilkan",
                                    tint = themeColors.gold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.gold,
                            unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text,
                            focusedContainerColor = themeColors.bg.copy(alpha = 0.3f),
                            unfocusedContainerColor = themeColors.bg.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fetch models button + Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown selection Box
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = openAiModel,
                                onValueChange = { viewModel.updateOpenAiModel(it) },
                                label = { Text("Pilih Model", color = themeColors.gold, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Model",
                                            tint = themeColors.gold
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.gold,
                                    unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                                    focusedTextColor = themeColors.text,
                                    unfocusedTextColor = themeColors.text,
                                    focusedContainerColor = themeColors.bg.copy(alpha = 0.3f),
                                    unfocusedContainerColor = themeColors.bg.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .background(themeColors.card)
                                    .border(1.dp, themeColors.gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                openAiAvailableModels.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, color = themeColors.text, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.updateOpenAiModel(m)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.fetchModels() },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.gold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp).align(Alignment.Bottom)
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Muat Model",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fetch", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    fetchModelsError?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = if (msg.contains("Berhasil")) Color.Green else Color.Red,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model Context Protocol (MCP) Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.card),
            border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = themeColors.gold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Model Context Protocol (MCP)",
                                fontWeight = FontWeight.Bold,
                                color = themeColors.text,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Integrasi AI via Server-Sent Events (SSE)",
                                color = themeColors.text.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = useMcp,
                        onCheckedChange = { viewModel.updateUseMcp(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = themeColors.gold,
                            uncheckedThumbColor = themeColors.text.copy(alpha = 0.4f),
                            uncheckedTrackColor = themeColors.bg
                        )
                    )
                }

                if (useMcp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = themeColors.gold.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // MCP URL Base
                    OutlinedTextField(
                        value = mcpBaseUrl,
                        onValueChange = { viewModel.updateMcpBaseUrl(it) },
                        label = { Text("MCP Server Base URL", color = themeColors.gold, fontSize = 12.sp) },
                        placeholder = { Text("https://winongkencono-shamelah.hf.space", color = themeColors.text.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.gold,
                            unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text,
                            focusedContainerColor = themeColors.bg.copy(alpha = 0.3f),
                            unfocusedContainerColor = themeColors.bg.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Connection status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ",
                            color = themeColors.text.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = mcpStatusText,
                            color = if (mcpStatusText.contains("Terhubung")) Color.Green else themeColors.gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Available Tools Info Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.bg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, themeColors.gold.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Fitur & Kapabilitas MCP:",
                                color = themeColors.gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• search_arabic_text: Cari kutipan kitab riil.\n" +
                                       "• read_book_page: Membaca halaman & bab kitab.\n" +
                                       "• get_book_metadata: Informasi penerbit & naskah.\n" +
                                       "• Navigasi: Daftar Isi (TOC), pencarian katalog, pencarian penulis, & sitasi halaman otomatis.",
                                color = themeColors.text.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pasal.id Configuration Card
        val pasalToken by viewModel.pasalToken.collectAsState()
        var pasalTokenVisible by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.card),
            border = BorderStroke(1.dp, themeColors.gold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = null,
                        tint = themeColors.gold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Token API pasal.id",
                            fontWeight = FontWeight.Bold,
                            color = themeColors.text,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Konfigurasi token akses gratis regulasi Indonesia",
                            color = themeColors.text.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pasalToken,
                    onValueChange = { viewModel.updatePasalToken(it) },
                    placeholder = { Text("Masukkan token pasal_mcp_...", color = themeColors.text.copy(alpha = 0.4f)) },
                    visualTransformation = if (pasalTokenVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { pasalTokenVisible = !pasalTokenVisible }) {
                            Icon(
                                imageVector = if (pasalTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pasalTokenVisible) "Sembunyikan" else "Tampilkan",
                                tint = themeColors.gold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.gold,
                        unfocusedBorderColor = themeColors.gold.copy(alpha = 0.3f),
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text,
                        focusedContainerColor = themeColors.bg.copy(alpha = 0.3f),
                        unfocusedContainerColor = themeColors.bg.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Information list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.card)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Offline Synchronization Mode", fontWeight = FontWeight.Bold, color = themeColors.text)
                    Text("Aktif", color = Color.Green, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = themeColors.gold.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Versi Database Shamela", color = themeColors.text)
                    Text("v2.1 (Full Turats)", color = themeColors.text.copy(alpha = 0.6f))
                }
                HorizontalDivider(color = themeColors.gold.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Versi Aplikasi", color = themeColors.text)
                    Text("1.0.0 (Modern Edition)", color = themeColors.text.copy(alpha = 0.6f))
                }
            }
        }
    }
}
