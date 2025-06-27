package takagi.ru.shikahub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import takagi.ru.shikahub.ui.screen.*
import takagi.ru.shikahub.ui.theme.ShikaHubTheme
import android.view.View

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : Screen(
        "home", 
        "首页", 
        { Icon(Icons.Outlined.Home, contentDescription = "首页") }
    )
    object Stats : Screen(
        "stats", 
        "统计", 
        { Icon(Icons.Outlined.Analytics, contentDescription = "统计") }
    )
    object Calendar : Screen(
        "calendar", 
        "日历", 
        { Icon(Icons.Outlined.CalendarMonth, contentDescription = "日历") }
    )
    
    // 新增路由
    object AddShika : Screen(
        "add_shika",
        "添加鹿",
        { Icon(Icons.Outlined.Home, contentDescription = "添加鹿") }
    )
    
    object ShikaDetail : Screen(
        "shika_detail/{shikaId}",
        "鹿详情",
        { Icon(Icons.Outlined.Home, contentDescription = "鹿详情") }
    ) {
        fun createRoute(shikaId: Long): String {
            return "shika_detail/$shikaId"
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边缘到边缘显示
        enableEdgeToEdge()
        
        // 设置全屏沉浸式体验
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置状态栏和导航栏完全透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // 根据浅色/深色模式设置状态栏图标颜色
        val isDarkMode = resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        // 确保系统栏可见性
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 
                                             View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
        
        setContent {
            ShikaHubTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Stats,
        Screen.Calendar
    )
    
    // 跟踪当前路由
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = remember(currentRoute) {
        // 只在主导航页面显示底部导航栏
        listOf(
            Screen.Home.route,
            Screen.Stats.route,
            Screen.Calendar.route
        ).any { it == currentRoute }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = { screen.icon() },
                            label = { Text(screen.label) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddShika = {
                        navController.navigate(Screen.AddShika.route)
                    },
                    onNavigateToShikaDetail = { shikaId ->
                        navController.navigate(Screen.ShikaDetail.createRoute(shikaId))
                    }
                )
            }
            
            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
            
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onNavigateToShikaDetail = { shikaId ->
                        navController.navigate(Screen.ShikaDetail.createRoute(shikaId))
                    }
                )
            }
            
            // 添加鹿页面
            composable(Screen.AddShika.route) {
                AddShikaScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
            
            // 鹿详情页面
            composable(
                route = Screen.ShikaDetail.route,
                arguments = listOf(
                    navArgument("shikaId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val shikaId = backStackEntry.arguments?.getLong("shikaId") ?: 1L
                ShikaDetailScreen(
                    shikaId = shikaId,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
        }
    }
}