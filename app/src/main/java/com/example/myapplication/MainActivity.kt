@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// ✅ 只保留 Home + Settings（Workout/Community 已删除）
enum class BottomTab(val label: String) { HOME("Home"), SETTINGS("Settings") }

enum class QuickFeature(val label: String) {
    FIND_COURSES("Find Courses"),
    RUNNING("Running"),
    MOVES("Moves"),
    LIVE("Live"),
    YOGA("Yoga"),
    WALKING("Walking"),
    CHALLENGE("Challenge"),
    MORE("More")
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
    // ✅ 全局 Dark Mode 状态
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
                    onBack = { nav.popBackStack() }
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
                    onBack = { nav.popBackStack() },
                    onFinish = { nav.popBackStack("main", inclusive = false) }
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

            composable("challenge") { ChallengeScreen(onBack = { nav.popBackStack() }) }

            // ✅ More（可点）
            composable("more") { MoreScreen(nav = nav, onBack = { nav.popBackStack() }) }
            composable("nutrition") { NutritionDetailScreen(onBack = { nav.popBackStack() }) }
            composable("equipment") { EquipmentDetailScreen(onBack = { nav.popBackStack() }) }
            composable("help") { HelpDetailScreen(onBack = { nav.popBackStack() }) }

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
    var topTab by remember { mutableStateOf(TopTab.RECOMMEND) }
    var bottomTab by remember { mutableStateOf(BottomTab.HOME) }

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
            QuickFeature.CHALLENGE -> nav.navigate("challenge")
            QuickFeature.MORE -> nav.navigate("more")
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

/* -------------------- Recommend Page -------------------- */

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
        QuickFeature.WALKING,
        QuickFeature.CHALLENGE,
        QuickFeature.MORE
    )

    val results = remember(query, searchPool) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else searchPool.distinctBy { it.title }
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
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(180.dp),
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
        ) {
            Text(title.take(1), fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RecommendCard(item: WorkoutMeta, onStart: () -> Unit) {
    // drawable: abs_beginner / hiit_fat_burn / full_body_stretch
    val imageRes = when (item.title) {
        "Abs Beginner" -> R.drawable.abs_beginner
        "Standing HIIT Fat Burn" -> R.drawable.hiit_fat_burn
        "Full Body Stretch" -> R.drawable.full_body_stretch
        else -> R.drawable.abs_beginner
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = item.title,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

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

/* -------------------- Quick Feature Screens -------------------- */

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
fun ChallengeScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Challenge") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Weekly Challenges", fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("• 3-Day Plank Challenge") } }
            Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("• 7-Day Walking Streak") } }
            Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("• HIIT x 5 Sessions") } }
        }
    }
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

/* -------------------- More (Clickable + Detail Content) -------------------- */

@Composable
fun MoreScreen(nav: NavHostController, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoreItem("Nutrition Tips") { nav.navigate("nutrition") }
            MoreItem("Equipment") { nav.navigate("equipment") }
            MoreItem("Help & Support") { nav.navigate("help") }
        }
    }
}

@Composable
fun MoreItem(title: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("Open", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun NutritionDetailScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Nutrition Tips", onBack = onBack) {
        InfoCard("Drink water before and after training.")
        InfoCard("Eat protein after workouts (eggs, chicken, milk).")
        InfoCard("Avoid sugary drinks and fried food.")
        InfoCard("Keep a regular meal schedule.")
    }
}

@Composable
fun EquipmentDetailScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Equipment", onBack = onBack) {
        InfoCard("Yoga mat – for yoga and stretching.")
        InfoCard("Dumbbells – strength training.")
        InfoCard("Resistance band – warm-up & rehab.")
        InfoCard("Running shoes – protect knees and ankles.")
    }
}

@Composable
fun HelpDetailScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Help & Support", onBack = onBack) {
        InfoCard("FAQ: Training & timer issues.")
        InfoCard("Contact: support@pulse.demo")
        InfoCard("Feedback: Report bugs and suggestions.")
        InfoCard("Privacy: Your data is stored locally (demo).")
    }
}

@Composable
fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun InfoCard(text: String) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
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
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // ✅ 点了就全局变黑/变亮
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
