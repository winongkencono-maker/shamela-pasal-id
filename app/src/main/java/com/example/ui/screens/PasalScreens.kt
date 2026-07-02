package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasalDashboardScreen(
    viewModel: ShamelaViewModel,
    modifier: Modifier = Modifier
) {
    val activeLaw by viewModel.activePasalLaw.collectAsState()

    AnimatedContent(
        targetState = activeLaw,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        },
        label = "PasalNavigation"
    ) { targetLaw ->
        if (targetLaw != null) {
            PasalLawDetailScreen(
                viewModel = viewModel,
                work = targetLaw,
                onBack = { viewModel.activePasalLaw.value = null }
            )
        } else {
            PasalMainTabs(viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
fun PasalMainTabs(
    viewModel: ShamelaViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Jelajah", "Pencarian", "Favorit")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Balance,
                    contentDescription = "Hukum Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pustaka Regulasi RI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pencarian & Metadata Hukum Indonesia via pasal.id",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Explore
                                    1 -> Icons.Default.Search
                                    else -> Icons.Default.Favorite
                                },
                                contentDescription = title,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = title, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("pasal_tab_$index")
                )
            }
        }

        // Tab Contents
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> PasalBrowseView(viewModel = viewModel)
                1 -> PasalSearchView(viewModel = viewModel)
                2 -> PasalFavoritesView(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasalBrowseView(viewModel: ShamelaViewModel) {
    val listState by viewModel.pasalListState.collectAsState()
    val selectedType by viewModel.pasalSelectedTypeFilter.collectAsState()
    val selectedStatus by viewModel.pasalSelectedStatusFilter.collectAsState()

    val types = listOf(
        null to "Semua",
        "UU" to "Undang-Undang",
        "PP" to "Peraturan Pemerintah",
        "PERPRES" to "Perpres",
        "PERMEN" to "Permen",
        "PERPPU" to "Perppu",
        "TAP_MPR" to "TAP MPR",
        "PERDA" to "Perda"
    )

    val statuses = listOf(
        null to "Semua",
        "berlaku" to "Berlaku",
        "dicabut" to "Dicabut",
        "diubah" to "Diubah"
    )

    // Initial load
    LaunchedEffect(selectedType, selectedStatus) {
        viewModel.loadPasalLaws(type = selectedType, status = selectedStatus)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type Filters
            Text(
                text = "Jenis Peraturan:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                types.forEach { (typeKey, typeLabel) ->
                    FilterChip(
                        selected = selectedType == typeKey,
                        onClick = { viewModel.pasalSelectedTypeFilter.value = typeKey },
                        label = { Text(typeLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Status Filters
            Text(
                text = "Status Peraturan:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { (statusKey, statusLabel) ->
                    FilterChip(
                        selected = selectedStatus == statusKey,
                        onClick = { viewModel.pasalSelectedStatusFilter.value = statusKey },
                        label = { Text(statusLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Laws list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = listState) {
                is PasalListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PasalListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadPasalLaws(type = selectedType, status = selectedStatus) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is PasalListUiState.Success -> {
                    val laws = state.data.laws ?: emptyList()
                    if (laws.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada peraturan yang cocok dengan filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(laws) { law ->
                                LawWorkCard(
                                    work = law,
                                    onClick = { viewModel.activePasalLaw.value = law }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasalSearchView(viewModel: ShamelaViewModel) {
    val searchState by viewModel.pasalSearchState.collectAsState()
    val searchQuery by viewModel.pasalSearchQuery.collectAsState()
    val selectedType by viewModel.pasalSelectedTypeFilter.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.pasalSearchQuery.value = it },
            placeholder = { Text("Cari pasal atau kata kunci hukum...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.pasalSearchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.searchPasal(searchQuery, selectedType)
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pasal_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Type filter quick chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shortTypes = listOf(
                null to "Semua Jenis",
                "UU" to "UU",
                "PP" to "PP",
                "PERPRES" to "Perpres",
                "PERMEN" to "Permen"
            )
            shortTypes.forEach { (typeKey, typeLabel) ->
                FilterChip(
                    selected = selectedType == typeKey,
                    onClick = {
                        viewModel.pasalSelectedTypeFilter.value = typeKey
                        viewModel.searchPasal(searchQuery, typeKey)
                    },
                    label = { Text(typeLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Search Results State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = searchState) {
                is PasalSearchUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Idle",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ketik kata kunci untuk mencari regulasi nasional.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Contoh: \"Upah minimum\", \"Cipta kerja\", \"Korupsi\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is PasalSearchUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PasalSearchUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.searchPasal(searchQuery, selectedType) }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is PasalSearchUiState.Success -> {
                    val results = state.data.results ?: emptyList()
                    val didYouMean = state.data.didYouMean ?: emptyList()

                    if (results.isEmpty() && didYouMean.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hasil pencarian kosong.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // "Mungkin Maksud Anda" spelling suggestion
                            if (didYouMean.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Rekomendasi Peraturan Terkait:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            didYouMean.forEach { suggestion ->
                                                suggestion.work?.let { work ->
                                                    Text(
                                                        text = "• ${work.title} (${work.type} No ${work.number}/${work.year})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier
                                                            .padding(vertical = 4.dp)
                                                            .clickable { viewModel.activePasalLaw.value = work }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Regular search results list
                            items(results) { result ->
                                PasalSearchResultCard(
                                    result = result,
                                    onClick = {
                                        result.work?.let {
                                            viewModel.activePasalLaw.value = it
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasalFavoritesView(viewModel: ShamelaViewModel) {
    val favorites by viewModel.pasalFavoritesList.collectAsState()

    if (favorites.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = "No Favorites",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Belum ada peraturan yang difavoritkan.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ketuk ikon bookmark di pojok kanan atas saat membaca peraturan untuk menyimpannya di sini.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favorites) { fav ->
                val work = PasalWork(
                    id = null,
                    frbrUri = fav.frbrUri,
                    title = fav.title,
                    number = fav.number,
                    year = fav.year,
                    status = fav.status,
                    type = fav.type
                )
                LawWorkCard(
                    work = work,
                    onClick = { viewModel.activePasalLaw.value = work }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasalLawDetailScreen(
    viewModel: ShamelaViewModel,
    work: PasalWork,
    onBack: () -> Unit
) {
    val detailState by viewModel.pasalDetailState.collectAsState()
    val favorites by viewModel.pasalFavoritesList.collectAsState()
    val isFavorited = favorites.any { it.frbrUri == work.frbrUri }

    var articleQuery by remember { mutableStateOf("") }

    LaunchedEffect(work.frbrUri) {
        viewModel.loadPasalLawDetail(work.frbrUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${work.type ?: "REG"} No ${work.number ?: ""}/${work.year ?: ""}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = work.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Favorite/Bookmark button
                    IconButton(onClick = { viewModel.togglePasalFavorite(work) }) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isFavorited) "Hapus dari Favorit" else "Simpan ke Favorit",
                            tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = detailState) {
                is PasalDetailUiState.Idle -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PasalDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PasalDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPasalLawDetail(work.frbrUri) }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is PasalDetailUiState.Success -> {
                    val detail = state.data
                    val articles = detail.articles ?: emptyList()
                    val filteredArticles = if (articleQuery.isBlank()) {
                        articles
                    } else {
                        articles.filter {
                            it.heading?.contains(articleQuery, ignoreCase = true) == true ||
                                    it.content?.contains(articleQuery, ignoreCase = true) == true
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Law Summary Card Header
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(detail.work.typeName ?: work.type ?: "Peraturan") }
                                    )
                                    StatusBadge(status = detail.work.status ?: work.status ?: "berlaku")
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = detail.work.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (detail.relationships?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Hubungan Regulasi:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        detail.relationships.forEach { relation ->
                                            relation.relatedWork?.let { rw ->
                                                AssistChip(
                                                    onClick = {
                                                        viewModel.activePasalLaw.value = rw
                                                    },
                                                    label = {
                                                        Text("${relation.type ?: "Terkait"}: ${rw.type ?: "REG"} ${rw.number ?: ""}/${rw.year ?: ""}")
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(14.dp))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Search within law field
                        OutlinedTextField(
                            value = articleQuery,
                            onValueChange = { articleQuery = it },
                            placeholder = { Text("Cari kata kunci dalam dokumen ini...") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter") },
                            trailingIcon = {
                                if (articleQuery.isNotEmpty()) {
                                    IconButton(onClick = { articleQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        HorizontalDivider()

                        // Articles and Chapter Listing
                        if (filteredArticles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (articleQuery.isEmpty()) "Tidak ada isi pasal / teks yang tersedia." else "Pencarian artikel nihil.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                items(filteredArticles) { article ->
                                    ArticleItemView(article = article, query = articleQuery)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleItemView(article: PasalArticle, query: String) {
    val isChapter = article.type.equals("bab", ignoreCase = true) ||
            article.type.equals("judul", ignoreCase = true) ||
            article.type.equals("daftar", ignoreCase = true)

    if (isChapter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = (article.heading ?: "").uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (!article.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.width(80.dp)
            )
        }
    } else {
        // Regular Pasal/Article Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = article.number ?: "§",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = article.heading ?: "Pasal ${article.number ?: ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!article.content.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = article.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LawWorkCard(
    work: PasalWork,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${work.type ?: "REG"} No ${work.number ?: ""}/${work.year ?: ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = work.typeName ?: when (work.type) {
                            "UU" -> "Undang-Undang"
                            "PP" -> "Peraturan Pemerintah"
                            "PERPRES" -> "Peraturan Presiden"
                            "PERMEN" -> "Peraturan Menteri"
                            "PERPPU" -> "Perppu"
                            "TAP_MPR" -> "Ketetapan MPR"
                            "PERDA" -> "Peraturan Daerah"
                            else -> "Regulasi"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                StatusBadge(status = work.status ?: "berlaku")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = work.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (work.contentVerified == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Konten Terverifikasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PasalSearchResultCard(
    result: PasalSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            result.work?.let { work ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${work.type ?: "REG"} No ${work.number ?: ""}/${work.year ?: ""}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatusBadge(status = work.status ?: "berlaku")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!result.snippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "... " + result.snippet + " ...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            result.metadata?.let { meta ->
                if (!meta.nodeType.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Kecocokan: ${meta.nodeType.uppercase()} ${meta.nodeNumber ?: ""}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val cleanStatus = status.lowercase().trim()
    val containerColor = when (cleanStatus) {
        "berlaku" -> Color(0xFFE8F5E9)
        "diubah" -> Color(0xFFFFF3E0)
        "dicabut" -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (cleanStatus) {
        "berlaku" -> Color(0xFF2E7D32)
        "diubah" -> Color(0xFFE65100)
        "dicabut" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = cleanStatus.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
