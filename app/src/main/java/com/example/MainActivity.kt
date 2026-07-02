package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShamelaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ShamelaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val readerTheme by viewModel.readerTheme.collectAsState()
            val themeColors = ShamelaColors.getColors(readerTheme)
            
            MyApplicationTheme {
                var activeScreen by remember { mutableStateOf("tabs") } // "tabs" or "reader"
                
                if (activeScreen == "reader") {
                    ReaderScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onNavigateBack = { activeScreen = "tabs" }
                    )
                } else {
                    val currentTab by viewModel.currentTab.collectAsState()
                    
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = themeColors.bg,
                        bottomBar = {
                            NavigationBar(
                                containerColor = themeColors.card,
                                contentColor = themeColors.gold,
                                modifier = Modifier.testTag("bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "beranda",
                                    onClick = { viewModel.currentTab.value = "beranda" },
                                    label = { Text("Beranda") },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = themeColors.gold,
                                        selectedTextColor = themeColors.gold,
                                        indicatorColor = themeColors.gold.copy(alpha = 0.15f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "perpustakaan",
                                    onClick = { viewModel.currentTab.value = "perpustakaan" },
                                    label = { Text("Pustaka") },
                                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Pustaka") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = themeColors.gold,
                                        selectedTextColor = themeColors.gold,
                                        indicatorColor = themeColors.gold.copy(alpha = 0.15f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "cari",
                                    onClick = { viewModel.currentTab.value = "cari" },
                                    label = { Text("Cari") },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = themeColors.gold,
                                        selectedTextColor = themeColors.gold,
                                        indicatorColor = themeColors.gold.copy(alpha = 0.15f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "peraturan",
                                    onClick = { viewModel.currentTab.value = "peraturan" },
                                    label = { Text("Regulasi") },
                                    icon = { Icon(Icons.Default.Balance, contentDescription = "Regulasi") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = themeColors.gold,
                                        selectedTextColor = themeColors.gold,
                                        indicatorColor = themeColors.gold.copy(alpha = 0.15f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "profil",
                                    onClick = { viewModel.currentTab.value = "profil" },
                                    label = { Text("Profil") },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = themeColors.gold,
                                        selectedTextColor = themeColors.gold,
                                        indicatorColor = themeColors.gold.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                "beranda" -> BerandaScreen(
                                    viewModel = viewModel,
                                    themeColors = themeColors,
                                    onNavigateToReader = { activeScreen = "reader" }
                                )
                                "perpustakaan" -> PerpustakaanScreen(
                                    viewModel = viewModel,
                                    themeColors = themeColors,
                                    onNavigateToReader = { activeScreen = "reader" }
                                )
                                "cari" -> CariScreen(
                                    viewModel = viewModel,
                                    themeColors = themeColors,
                                    onNavigateToReader = { activeScreen = "reader" }
                                )
                                "peraturan" -> PasalDashboardScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "profil" -> ProfilScreen(
                                    viewModel = viewModel,
                                    themeColors = themeColors
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
