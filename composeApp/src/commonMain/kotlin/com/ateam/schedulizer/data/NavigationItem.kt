import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

data class NavigationItem(
    val unSelectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val title: String,
    val route: String
)

object Graph {
    const val NAVIGATION_BAR_SCREEN_GRAPH = "navigationBarScreenGraph"
}

sealed class Routes(var route: String) {

    data object Home : Routes("home")
    data object Tasks : Routes("tasks")
    data object Calendar : Routes("Calendar")
    data object HomeDetail : Routes("homeDetail")
    data object SettingDetail : Routes("settingDetail")
}

val navigationItemsLists = listOf(
    NavigationItem(
        unSelectedIcon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        title = "Home",
        route = Routes.Home.route,
    ),
//    NavigationItem(
//        unSelectedIcon = Icons.Outlined.AddTask,
//        selectedIcon = Icons.Filled.AddTask,
//        title = "Tasks",
//        route = Routes.Tasks.route,
//    ),
    NavigationItem(
        unSelectedIcon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth,
        title = "Calendar",
        route = Routes.Tasks.route,
    ),
    NavigationItem(
        unSelectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        title = "Settings",
        route = Routes.SettingDetail.route,
    )
)