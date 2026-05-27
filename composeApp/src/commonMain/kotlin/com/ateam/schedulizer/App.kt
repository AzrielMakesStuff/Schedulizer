package com.ateam.schedulizer

import NavigationItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import navigationItemsLists
import Routes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
@Preview
fun App() {
    LaunchedEffect(Unit) {
        databaseInstance.refresh()
    }

    val rootNavController = rememberNavController()
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = navigationItemsLists,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    rootNavController.navigate(item.route) {
                        popUpTo(Routes.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            NavHost(
                navController = rootNavController,
                startDestination = Routes.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Routes.Home.route) {
                    HomeScreen()
                }
                composable(Routes.Calendar.route) {
                    Calendar()
                }
                composable(Routes.SettingDetail.route)
                {
                    Settings()
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    items: List<NavigationItem>,
    currentRoute: String?,
    onItemClick: (NavigationItem) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { navigationItem ->
            NavigationBarItem(
                selected = currentRoute == navigationItem.route,
                onClick = { onItemClick(navigationItem) },
                icon = {
                    Icon(
                        imageVector = if (navigationItem.route == currentRoute) navigationItem.selectedIcon else navigationItem.unSelectedIcon,
                        contentDescription = navigationItem.title,
                    )
                },
                label = {
                    Text(
                        text = navigationItem.title,
                        style = if (navigationItem.route == currentRoute) MaterialTheme.typography.labelLarge
                        else MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
            )
        }
    }
}