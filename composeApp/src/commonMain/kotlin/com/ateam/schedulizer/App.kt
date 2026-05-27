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
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.entities.TaskType
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
@Preview
fun App() {
    val dao = databaseInstance.getDao()
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        tasks = dao.getAll()
    }

    fun createTask() {
        scope.launch {
            dao.upsert(
                Task(
                    type = TaskType.DAILY,
                    title = "New Task",
                    description = "Test description",
                    dateOne = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    dateTwo = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            )
            tasks = dao.getAll()
        }
    }

    fun deleteTask(task: Task) {
        scope.launch {
            dao.delete(task.id)
            tasks = dao.getAll()
        }
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
                    HomeScreen(/*tasks = tasks, onCreateTask = ::createTask, onDeleteTask = ::deleteTask*/)
                }
                composable(Routes.Calendar.route) {
                    Calendar()
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