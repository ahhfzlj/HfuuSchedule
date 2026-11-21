package com.hfuu.schedule.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hfuu.schedule.ui.agenda.AgendaEditorScreen
import com.hfuu.schedule.ui.agenda.AgendaScreen
import com.hfuu.schedule.ui.profile.AccountManageScreen
import com.hfuu.schedule.ui.profile.ProfileScreen
import com.hfuu.schedule.ui.profile.SemesterSettingsScreen
import com.hfuu.schedule.ui.profile.TimeTableSettingsScreen
import com.hfuu.schedule.ui.profile.UiStyleScreen
import com.hfuu.schedule.ui.schedule.JwxtImportScreen
import com.hfuu.schedule.ui.schedule.ScheduleScreen
import com.hfuu.schedule.ui.study.ChaoxingLoginScreen
import com.hfuu.schedule.ui.study.StudyScreen
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.CardSurface
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesDivider
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.MassesMuted
import com.hfuu.schedule.ui.theme.MassesSlate
import com.hfuu.schedule.ui.theme.MassesSurface
import com.hfuu.schedule.ui.theme.TextPrimary
import com.hfuu.schedule.ui.theme.TextSecondary
import com.hfuu.schedule.ui.viewmodel.MainViewModel

object Routes {
    const val SCHEDULE = "schedule"
    const val STUDY = "study"
    const val AGENDA = "agenda"
    const val PROFILE = "profile"

    const val AGENDA_EDITOR = "agenda_editor?eventId={eventId}&presetTag={presetTag}&presetDate={presetDate}"
    const val JWXT_IMPORT = "jwxt_import"
    const val CHAOXING_LOGIN = "chaoxing_login"
    const val ACCOUNT_MANAGE = "account_manage"
    const val TIME_SETTINGS = "time_settings"
    const val SEMESTER_SETTINGS = "semester_settings"
    const val UI_STYLE = "ui_style"

    fun agendaEditorRoute(
        eventId: Long = 0L,
        presetTag: String = "",
        presetDate: String = ""
    ): String = "agenda_editor?eventId=$eventId&presetTag=$presetTag&presetDate=$presetDate"
}

private data class BottomTab(
    val route: String,
    val indexCode: String,
    val label: String,
    val englishLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.SCHEDULE, "01", "课表", "SCHEDULE", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomTab(Routes.STUDY, "02", "学习", "COVENANT", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    BottomTab(Routes.AGENDA, "03", "日程", "AGENDA", Icons.Rounded.EventAvailable, Icons.Outlined.EventAvailable),
    BottomTab(Routes.PROFILE, "04", "我的", "ARCHIVE", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun AppNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isMasses = AppTheme.isMasses
    val startDestination = remember {
        if (viewModel.initialStartInAgenda) Routes.AGENDA else Routes.SCHEDULE
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearToast()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    fun navigateBottomTab(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                if (isMasses) {
                    Snackbar(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(2.dp),
                        containerColor = MassesInk,
                        contentColor = Color.White,
                        actionContentColor = MassesCobaltLight
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .background(MassesCobalt)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = data.visuals.message,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF1F242B),
                        contentColor = Color.White
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                if (isMasses) {
                    MassesBottomBar(
                        currentRoute = currentRoute,
                        onSelectTab = ::navigateBottomTab
                    )
                } else {
                    NavigationBar(
                        containerColor = CardSurface,
                        tonalElevation = 0.dp
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateBottomTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(text = tab.label, fontSize = 11.sp)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    viewModel = viewModel,
                    onOpenJwxtImport = { navController.navigate(Routes.JWXT_IMPORT) },
                    onOpenTimeSettings = { navController.navigate(Routes.TIME_SETTINGS) }
                )
            }
            composable(Routes.STUDY) {
                StudyScreen(
                    viewModel = viewModel,
                    onOpenChaoxingLogin = { navController.navigate(Routes.CHAOXING_LOGIN) }
                )
            }
            composable(Routes.AGENDA) {
                AgendaScreen(
                    viewModel = viewModel,
                    onOpenAddAgenda = { presetTag, presetDateIso ->
                        navController.navigate(
                            Routes.agendaEditorRoute(
                                eventId = 0L,
                                presetTag = presetTag?.name.orEmpty(),
                                presetDate = presetDateIso
                            )
                        )
                    },
                    onOpenEditAgenda = { eventId ->
                        navController.navigate(Routes.agendaEditorRoute(eventId = eventId))
                    }
                )
            }
            composable(
                route = Routes.AGENDA_EDITOR,
                arguments = listOf(
                    navArgument("eventId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("presetTag") { type = NavType.StringType; defaultValue = "" },
                    navArgument("presetDate") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val eventId = entry.arguments?.getLong("eventId") ?: 0L
                val presetTag = entry.arguments?.getString("presetTag").orEmpty().ifBlank { null }
                val presetDate = entry.arguments?.getString("presetDate").orEmpty().ifBlank { null }
                AgendaEditorScreen(
                    viewModel = viewModel,
                    eventId = eventId.takeIf { it > 0L },
                    presetTagName = presetTag,
                    presetDateIso = presetDate,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    onOpenAccountManage = { navController.navigate(Routes.ACCOUNT_MANAGE) },
                    onOpenJwxtImport = { navController.navigate(Routes.JWXT_IMPORT) },
                    onOpenSemesterSettings = { navController.navigate(Routes.SEMESTER_SETTINGS) },
                    onOpenTimeSettings = { navController.navigate(Routes.TIME_SETTINGS) },
                    onOpenUiStyle = { navController.navigate(Routes.UI_STYLE) }
                )
            }
            composable(Routes.JWXT_IMPORT) {
                JwxtImportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CHAOXING_LOGIN) {
                ChaoxingLoginScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ACCOUNT_MANAGE) {
                AccountManageScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenJwxtImport = { navController.navigate(Routes.JWXT_IMPORT) },
                    onOpenChaoxingLogin = { navController.navigate(Routes.CHAOXING_LOGIN) }
                )
            }
            composable(Routes.TIME_SETTINGS) {
                TimeTableSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SEMESTER_SETTINGS) {
                SemesterSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.UI_STYLE) {
                UiStyleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * 众生行迹 · 拉特兰终端风格 4-Tab 底栏：
 * 01 课表 | 02 学习 | 03 日程 | 04 我的
 */
@Composable
private fun MassesBottomBar(
    currentRoute: String?,
    onSelectTab: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MassesSurface)
            .navigationBarsPadding()
    ) {
        // 顶部克莱因蓝与曜石黑复合轴线
        Row(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MassesCobalt.copy(alpha = 0.65f)))
            Box(modifier = Modifier.width(28.dp).fillMaxHeight().background(MassesInk))
            Box(modifier = Modifier.width(10.dp).fillMaxHeight().background(MassesCrimson))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.forEachIndexed { idx, tab ->
                val selected = currentRoute == tab.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) MassesCobaltLight.copy(alpha = 0.6f) else Color.Transparent)
                        .clickable { onSelectTab(tab.route) }
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .align(Alignment.TopCenter)
                                .background(MassesCobalt)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(4.dp)
                                .background(MassesCrimson)
                        )
                    }

                    Text(
                        text = tab.indexCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MassesCobalt else MassesMuted,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 6.dp, top = 5.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (selected) MassesCobalt else MassesSlate,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MassesInk else MassesSlate
                        )
                        Text(
                            text = tab.englishLabel,
                            fontFamily = FontFamily.Serif,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            letterSpacing = 0.5.sp,
                            color = if (selected) MassesCobalt else MassesMuted
                        )
                    }
                }

                if (idx < bottomTabs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(32.dp)
                            .background(MassesDivider)
                    )
                }
            }
        }
    }
}
