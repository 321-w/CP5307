@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

/* -------------------- Theme -------------------- */

@Composable
fun PulseTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkMode) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}

/* -------------------- Data & Enums -------------------- */

enum class TimerMode { COUNTDOWN, STOPWATCH }

data class WorkoutMeta(
    val title: String,
    val level: String,
    val minutes: Int,
    val mode: TimerMode
)

enum class TopTab(val label: String) { RECOMMEND("Recommend"), COURSES("Courses"), PLANS("Plans"), ROUTES("Routes") }

// ✅ 只保留 Home + Settings
enum class BottomTab(val label: String) { HOME("Home"), SETTINGS("Settings") }

// ✅ 已删除 Challenge / More
enum class QuickFeature(val label: String) {
    FIND_COURSES("Find Courses"),
    RUNNING("Running"),
    MOVES("Moves"),
    LIVE("Live"),
    YOGA("Yoga"),
    WALKING("Walking")
}

/* -------------------- Activity -------------------- */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

/* -------------------- Navigation Root -------------------- */

@Composable
fun AppRoot() {
    var darkMode by rememberSaveable { mutableStateOf(false) }

    PulseTheme(darkMode = darkMode) {
        val nav = rememberNavController()

        NavHost(navController = nav, startDestination = "login") {
            composable("login") { LoginScreen(nav) }

            composable("main") {
                MainScreen(
                    nav = nav,
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it }
                )
            }

            composable(
                route = "training/{title}/{minutes}/{mode}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType },
                    navArgument("mode") { type = NavType.StringType }
                )
            ) { backStack ->
                val title = backStack.arguments?.getString("title") ?: "Workout"
                val minutes = backStack.arguments?.getInt("minutes") ?: 10
                val mode = TimerMode.valueOf(backStack.arguments?.getString("mode") ?: "COUNTDOWN")
                TrainingScreen(
                    title = title,
                    minutes = minutes,
                    mode = mode,
                    nav = nav,
                    onBack = { nav.popBackStack() } // ✅ 永远回上一层
                )
            }

            composable(
                route = "session/{title}/{minutes}/{mode}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType },
                    navArgument("mode") { type = NavType.StringType }
                )
            ) { backStack ->
                val title = backStack.arguments?.getString("title") ?: "Workout"
                val minutes = backStack.arguments?.getInt("minutes") ?: 10
                val mode = TimerMode.valueOf(backStack.arguments?.getString("mode") ?: "COUNTDOWN")

                TrainingSessionScreen(
                    title = title,
                    minutes = minutes,
                    mode = mode,
                    onBack = { nav.popBackStack() },   // ✅ 回上一层（回 Training Detail）
                    onFinish = { nav.popBackStack() }  // ✅ 只退一层（不跳 main）
                )
            }

            composable("moves") {
                MovesScreen(
                    onOpen = { title ->
                        nav.navigate("training/${Uri.encode(title)}/8/${TimerMode.COUNTDOWN.name}")
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable("live") {
                LiveScreen(
                    onOpen = { title ->
                        nav.navigate("training/${Uri.encode(title)}/20/${TimerMode.COUNTDOWN.name}")
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                route = "category/{name}",
                arguments = listOf(navArgument("name") { type = NavType.StringType })
            ) { backStack ->
                val name = backStack.arguments?.getString("name") ?: "Category"
                CategoryScreen(
                    name = name,
                    onOpen = { title ->
                        nav.navigate("training/${Uri.encode(title)}/10/${TimerMode.COUNTDOWN.name}")
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

/* -------------------- Login -------------------- */

@Composable
fun LoginScreen(nav: NavHostController) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Sign in") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Pulse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Login first to continue.")

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (email.isBlank() || pwd.isBlank()) error = "Please enter email and password."
                    else nav.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Login") }
        }
    }
}

/* -------------------- Main -------------------- */

@Composable
fun MainScreen(
    nav: NavHostController,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    // ✅ 关键修复：用 rememberSaveable 保存 tab 状态，返回时不会跳回 Recommend
    val topTabSaver = remember {
        Saver<TopTab, String>(
            save = { it.name },
            restore = { TopTab.valueOf(it) }
        )
    }
    val bottomTabSaver = remember {
        Saver<BottomTab, String>(
            save = { it.name },
            restore = { BottomTab.valueOf(it) }
        )
    }

    var topTab by rememberSaveable(stateSaver = topTabSaver) { mutableStateOf(TopTab.RECOMMEND) }
    var bottomTab by rememberSaveable(stateSaver = bottomTabSaver) { mutableStateOf(BottomTab.HOME) }

    val openTrainingMeta: (WorkoutMeta) -> Unit = { w ->
        nav.navigate("training/${Uri.encode(w.title)}/${w.minutes}/${w.mode.name}")
    }
    val openStopwatch: (String) -> Unit = { title ->
        nav.navigate("session/${Uri.encode(title)}/0/${TimerMode.STOPWATCH.name}")
    }

    fun handleQuick(feature: QuickFeature) {
        bottomTab = BottomTab.HOME
        when (feature) {
            QuickFeature.FIND_COURSES -> topTab = TopTab.COURSES
            QuickFeature.RUNNING -> openStopwatch("Running")
            QuickFeature.MOVES -> nav.navigate("moves")
            QuickFeature.LIVE -> nav.navigate("live")
            QuickFeature.YOGA -> nav.navigate("category/${Uri.encode("Yoga")}")
            QuickFeature.WALKING -> nav.navigate("category/${Uri.encode("Walking")}")
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Pulse") })
                if (bottomTab == BottomTab.HOME) {
                    ScrollableTabRow(selectedTabIndex = topTab.ordinal, edgePadding = 16.dp) {
                        TopTab.entries.forEachIndexed { i, t ->
                            Tab(
                                selected = i == topTab.ordinal,
                                onClick = { topTab = t },
                                text = { Text(t.label) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = bottomTab == t,
                        onClick = { bottomTab = t },
                        label = { Text(t.label) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        when (bottomTab) {
            BottomTab.HOME -> {
                when (topTab) {
                    TopTab.RECOMMEND -> {
                        val searchPool = listOf(
                            WorkoutMeta("Abs Beginner", "K1", 9, TimerMode.COUNTDOWN),
                            WorkoutMeta("Standing HIIT Fat Burn", "K3", 13, TimerMode.COUNTDOWN),
                            WorkoutMeta("Full Body Stretch", "K1", 10, TimerMode.COUNTDOWN),

                            WorkoutMeta("Beginner Fat Burn", "K1", 10, TimerMode.COUNTDOWN),
                            WorkoutMeta("Core Strength", "K2", 12, TimerMode.COUNTDOWN),
                            WorkoutMeta("HIIT Quick Session", "K3", 13, TimerMode.COUNTDOWN),
                            WorkoutMeta("Stretch & Relax", "K1", 10, TimerMode.COUNTDOWN),

                            WorkoutMeta("7-Day Beginner Plan", "K1", 14, TimerMode.COUNTDOWN),
                            WorkoutMeta("14-Day Fat Burn Plan", "K2", 16, TimerMode.COUNTDOWN),
                            WorkoutMeta("30-Day Strength Plan", "K3", 18, TimerMode.COUNTDOWN),

                            WorkoutMeta("Running", "Route", 0, TimerMode.STOPWATCH),
                            WorkoutMeta("2 km Easy Route", "Route", 0, TimerMode.STOPWATCH),
                            WorkoutMeta("5 km City Loop", "Route", 0, TimerMode.STOPWATCH),
                            WorkoutMeta("10 km Long Run", "Route", 0, TimerMode.STOPWATCH),

                            // Yoga / Walking 仍然在列表里，但不会用任何 yoga 图片
                            WorkoutMeta("Yoga Beginner", "K1", 10, TimerMode.COUNTDOWN),
                            WorkoutMeta("Yoga Stretch", "K1", 12, TimerMode.COUNTDOWN),
                            WorkoutMeta("Yoga Balance", "K2", 15, TimerMode.COUNTDOWN),

                            WorkoutMeta("10-min Walk", "K1", 10, TimerMode.COUNTDOWN),
                            WorkoutMeta("20-min Brisk Walk", "K2", 20, TimerMode.COUNTDOWN),
                            WorkoutMeta("Outdoor Walk Plan", "K2", 15, TimerMode.COUNTDOWN)
                        )

                        RecommendPage(
                            modifier = Modifier.padding(padding),
                            onStart = { meta -> openTrainingMeta(meta) },
                            onQuick = { handleQuick(it) },
                            searchPool = searchPool,
                            onSearchPick = { meta ->
                                if (meta.mode == TimerMode.STOPWATCH) openStopwatch(meta.title)
                                else openTrainingMeta(meta)
                            }
                        )
                    }

                    TopTab.COURSES -> CoursesPage(
                        modifier = Modifier.padding(padding),
                        onOpen = { title -> openTrainingMeta(WorkoutMeta(title, "K1", 10, TimerMode.COUNTDOWN)) }
                    )

                    TopTab.PLANS -> PlansPage(
                        modifier = Modifier.padding(padding),
                        onOpen = { title -> openTrainingMeta(WorkoutMeta(title, "K2", 14, TimerMode.COUNTDOWN)) }
                    )

                    TopTab.ROUTES -> RoutesPage(
                        modifier = Modifier.padding(padding),
                        onOpen = { title -> openStopwatch(title) }
                    )
                }
            }

            BottomTab.SETTINGS -> SettingsPage(
                modifier = Modifier.padding(padding),
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                onLogout = {
                    nav.navigate("login") {
                        popUpTo("main") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/* -------------------- Recommend Page (Search + Quick + List) -------------------- */

@Composable
fun RecommendPage(
    modifier: Modifier = Modifier,
    onStart: (WorkoutMeta) -> Unit,
    onQuick: (QuickFeature) -> Unit,
    searchPool: List<WorkoutMeta>,
    onSearchPick: (WorkoutMeta) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val recommendList = listOf(
        WorkoutMeta("Abs Beginner", "K1", 9, TimerMode.COUNTDOWN),
        WorkoutMeta("Standing HIIT Fat Burn", "K3", 13, TimerMode.COUNTDOWN),
        WorkoutMeta("Full Body Stretch", "K1", 10, TimerMode.COUNTDOWN)
    )

    val quick = listOf(
        QuickFeature.FIND_COURSES,
        QuickFeature.RUNNING,
        QuickFeature.MOVES,
        QuickFeature.LIVE,
        QuickFeature.YOGA,
        QuickFeature.WALKING
    )

    val results = remember(query, searchPool) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else searchPool
            .distinctBy { it.title }
            .filter { it.title.contains(q, ignoreCase = true) }
            .take(6)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search courses / plans / routes / moves") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (results.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(8.dp)) {
                            results.forEachIndexed { idx, item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = ""
                                            onSearchPick(item)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val sub =
                                            if (item.mode == TimerMode.STOPWATCH) "Stopwatch"
                                            else "${item.minutes} min · ${item.level}"
                                        Text(sub, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Go", style = MaterialTheme.typography.bodySmall)
                                }
                                if (idx != results.lastIndex) Divider()
                            }
                        }
                    }
                } else if (query.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("No results", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Box(Modifier.padding(12.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(quick) { q ->
                            QuickEntryItem(title = q.label, onClick = { onQuick(q) })
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recommended for You", fontWeight = FontWeight.Bold)
                Text("See more", style = MaterialTheme.typography.bodySmall)
            }
        }

        items(recommendList) { item ->
            RecommendCard(item = item, onStart = { onStart(item) })
        }
    }
}

@Composable
fun QuickEntryItem(title: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text(title.take(1), fontWeight = FontWeight.Bold) }

        Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RecommendCard(item: WorkoutMeta, onStart: () -> Unit) {
    // ✅ 只给“推荐三项”配置图片；其它不匹配就不显示图片（不顶替）
    val imageRes: Int? = when (item.title) {
        "Abs Beginner" -> R.drawable.abs_beginner
        "Standing HIIT Fat Burn" -> R.drawable.hiit_fat_burn
        "Full Body Stretch" -> R.drawable.full_body_stretch
        else -> null
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.level} · ${item.minutes} min", style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = onStart) { Text("Start") }
        }
    }
}

/* -------------------- Tabs: Courses / Plans / Routes -------------------- */

@Composable
fun CoursesPage(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val courses = listOf("Beginner Fat Burn", "Core Strength", "HIIT Quick Session", "Stretch & Relax")
    SimpleListPage("Courses", courses, modifier, onOpen)
}

@Composable
fun PlansPage(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val plans = listOf("7-Day Beginner Plan", "14-Day Fat Burn Plan", "30-Day Strength Plan")
    SimpleListPage("Plans", plans, modifier, onOpen)
}

@Composable
fun RoutesPage(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val routes = listOf("2 km Easy Route", "5 km City Loop", "10 km Long Run")
    SimpleListPage("Routes", routes, modifier, onOpen)
}

@Composable
fun SimpleListPage(title: String, itemsList: List<String>, modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(itemsList) { name ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.clickable { onOpen(name) }) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Open", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/* -------------------- Training Detail -------------------- */

@Composable
fun TrainingScreen(
    title: String,
    minutes: Int,
    mode: TimerMode,
    nav: NavHostController,
    onBack: () -> Unit
) {
    BackHandler { onBack() } // ✅ 系统返回键回上一层

    // ✅ 只允许三张推荐图；Yoga 的三张图彻底不使用；其它标题全部不显示图
    val imageRes: Int? = when (title) {
        "Abs Beginner" -> R.drawable.abs_beginner
        "Standing HIIT Fat Burn" -> R.drawable.hiit_fat_burn
        "Full Body Stretch" -> R.drawable.full_body_stretch
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Detail") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (imageRes != null) {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick Info", fontWeight = FontWeight.SemiBold)
                    Text("• Mode: ${if (mode == TimerMode.COUNTDOWN) "Countdown" else "Stopwatch"}")
                    if (mode == TimerMode.COUNTDOWN) Text("• Duration: $minutes minutes")
                    else Text("• Duration: Free timing (stopwatch)")
                }
            }

            Button(
                onClick = { nav.navigate("session/${Uri.encode(title)}/${minutes}/${mode.name}") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start Workout") }
        }
    }
}

/* -------------------- Session (Countdown / Stopwatch) -------------------- */

@Composable
fun TrainingSessionScreen(
    title: String,
    minutes: Int,
    mode: TimerMode,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    BackHandler { onBack() }

    var running by remember { mutableStateOf(true) }

    val totalSeconds = (minutes.coerceAtLeast(0)) * 60
    var seconds by remember { mutableStateOf(if (mode == TimerMode.COUNTDOWN) totalSeconds else 0) }

    LaunchedEffect(running, seconds, mode) {
        if (!running) return@LaunchedEffect
        if (mode == TimerMode.COUNTDOWN && seconds <= 0) return@LaunchedEffect
        delay(1000)
        seconds = if (mode == TimerMode.COUNTDOWN) seconds - 1 else seconds + 1
    }

    fun format(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%02d:%02d".format(m, s)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Session") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { running = !running }) {
                        Text(if (running) "Pause" else "Resume")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(if (mode == TimerMode.COUNTDOWN) "Countdown (Target: $minutes min)" else "Stopwatch")

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (mode == TimerMode.COUNTDOWN) "Time Left" else "Time", fontWeight = FontWeight.SemiBold)
                    Text(format(seconds), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                }
            }

            if (mode == TimerMode.COUNTDOWN && seconds <= 0) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
            } else {
                OutlinedButton(
                    onClick = {
                        seconds = if (mode == TimerMode.COUNTDOWN) totalSeconds else 0
                        running = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restart") }

                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("End Session") }
            }
        }
    }
}

/* -------------------- Moves / Live / Category -------------------- */

@Composable
fun MovesScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val moves = listOf("Push-up Basics", "Squat Form", "Plank Core", "Burpee Intro")
    SimpleTopListScreen("Moves", moves, "Open", onOpen, onBack)
}

@Composable
fun LiveScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val live = listOf("Live Cardio Class", "Live Yoga Flow", "Live HIIT Session")
    SimpleTopListScreen("Live", live, "Join", onOpen, onBack)
}

@Composable
fun CategoryScreen(name: String, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val list = when (name.lowercase()) {
        "yoga" -> listOf("Yoga Beginner", "Yoga Stretch", "Yoga Balance")
        "walking" -> listOf("10-min Walk", "20-min Brisk Walk", "Outdoor Walk Plan")
        else -> listOf("$name Session A", "$name Session B", "$name Session C")
    }
    SimpleTopListScreen(name, list, "Open", onOpen, onBack)
}

@Composable
fun SimpleTopListScreen(
    title: String,
    itemsList: List<String>,
    actionText: String,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(itemsList) { name ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { onItemClick(name) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(actionText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/* -------------------- Settings -------------------- */

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var intensity by remember { mutableStateOf("Normal") }
    var soundOn by remember { mutableStateOf(true) }
    var vibrationOn by remember { mutableStateOf(true) }

    // ✅ Body Metrics (输入用 String 更稳定)
    var heightCmText by rememberSaveable { mutableStateOf("") }
    var weightKgText by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }
    var isMale by rememberSaveable { mutableStateOf(true) }

    fun parsePositiveFloat(text: String): Float? {
        val v = text.trim().toFloatOrNull() ?: return null
        return if (v > 0f) v else null
    }

    fun parsePositiveInt(text: String): Int? {
        val v = text.trim().toIntOrNull() ?: return null
        return if (v > 0) v else null
    }

    val heightCm = parsePositiveFloat(heightCmText)
    val weightKg = parsePositiveFloat(weightKgText)
    val age = parsePositiveInt(ageText)

    val bmi: Float? = if (heightCm != null && weightKg != null) {
        val hM = heightCm / 100f
        (weightKg / (hM * hM))
    } else null

    // Deurenberg estimate: BF% = 1.20*BMI + 0.23*Age - 10.8*Sex - 5.4
    val bodyFatPercent: Float? = if (bmi != null && age != null) {
        val sex = if (isMale) 1f else 0f
        (1.20f * bmi + 0.23f * age - 10.8f * sex - 5.4f)
    } else null

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Text("Training", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Intensity Level")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Easy", "Normal", "Hard").forEach { i ->
                            FilterChip(
                                selected = intensity == i,
                                onClick = { intensity = i },
                                label = { Text(i) }
                            )
                        }
                    }
                }
            }
        }

        item { SettingToggle("Countdown Sound", soundOn) { soundOn = it } }
        item { SettingToggle("Vibration Feedback", vibrationOn) { vibrationOn = it } }

        item {
            Spacer(Modifier.height(6.dp))
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = heightCmText,
                            onValueChange = { heightCmText = it },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Column(Modifier.weight(1f)) {
                            Text("Sex", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = isMale,
                                    onClick = { isMale = true },
                                    label = { Text("Male") }
                                )
                                FilterChip(
                                    selected = !isMale,
                                    onClick = { isMale = false },
                                    label = { Text("Female") }
                                )
                            }
                        }
                    }

                    Divider()

                    val bmiText = bmi?.let { String.format("%.1f", it) } ?: "--"
                    val bfText = bodyFatPercent?.let { String.format("%.1f%%", it.coerceIn(0f, 60f)) } ?: "--"

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("BMI", fontWeight = FontWeight.SemiBold)
                        Text(bmiText)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Body Fat (estimate)", fontWeight = FontWeight.SemiBold)
                        Text(bfText)
                    }

                    Text(
                        "Note: Body fat is an estimate based on BMI + age + sex.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item { SettingToggle("Dark Mode", darkMode) { onDarkModeChange(it) } }

        item {
            Spacer(Modifier.height(6.dp))
            Text("System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item { SettingItem("About Pulse", "Version 1.0.0") { } }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
fun SettingItem(title: String, value: String? = null, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { onClick() }) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            value?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
