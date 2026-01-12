package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// Color palettes
val DarkBlueGray = Color(0xFF5A5C71)
val LightLavender = Color(0xFFD8DBFA)
val DeepNavy = Color(0xFF34394E)
val MediumGray = Color(0xFF878DA5)

// Data classes
data class CommunityEvent(
    val id: Int,
    val title: String,
    val timeRange: String,
    val description: String,
    val schedules: List<EventSchedule>
)

data class EventSchedule(
    val date: String,
    val time: String,
    val venue: String
)

// Sample events data
val sampleEvents = listOf(
    CommunityEvent(
        id = 1,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 5, 2026", "9:00 AM - 12:00 PM", "Barangay Hall - Room 101"),
            EventSchedule("January 5, 2026", "1:00 PM - 5:00 PM", "Barangay Hall - Room 101"),
            EventSchedule("January 12, 2026", "9:00 AM - 5:00 PM", "Barangay Hall - Room 101")
        )
    ),
    CommunityEvent(
        id = 2,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 6, 2026", "9:00 AM - 12:00 PM", "Community Center"),
            EventSchedule("January 6, 2026", "1:00 PM - 5:00 PM", "Community Center")
        )
    ),
    CommunityEvent(
        id = 3,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 7, 2026", "10:00 AM - 4:00 PM", "Barangay Gym")
        )
    ),
    CommunityEvent(
        id = 4,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 8, 2026", "9:00 AM - 5:00 PM", "Main Office")
        )
    ),
    CommunityEvent(
        id = 5,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 9, 2026", "8:00 AM - 12:00 PM", "City Hall")
        )
    ),
    CommunityEvent(
        id = 6,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
        schedules = listOf(
            EventSchedule("January 10, 2026", "9:00 AM - 5:00 PM", "Plaza Area")
        )
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DarkBlueGray,
                    secondary = MediumGray,
                    background = LightLavender,
                    surface = Color.White,
                    onPrimary = Color.White,
                    onSecondary = DeepNavy,
                    onBackground = DeepNavy,
                    onSurface = DeepNavy
                )
            ) {
                ProfileSidebarApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSidebarApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerContent(
                onNavigate = { route ->
                    navController.navigate(route)
                    scope.launch { drawerState.close() }
                }
            )
        },
        content = {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Home",
                                color = DeepNavy,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(40.dp),
                                    tint = DarkBlueGray
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = DeepNavy,
                            navigationIconContentColor = DarkBlueGray
                        )
                    )
                },
                containerColor = LightLavender
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("home") { HomeScreen() }
                    composable("reservation") { Reservation() }
                    composable("reservations") { Reservations() }
                    composable("balance") { Balance() }
                    composable("account") { Account() }
                }
            }
        }
    )
}

@Composable
fun ProfileDrawerContent(onNavigate: (String) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = LightLavender,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
        ) {
            DrawerMenuItem(
                icon = Icons.Default.Home,
                title = "Home",
                onClick = { onNavigate("home") }
            )

            DrawerMenuItem(
                icon = Icons.Default.CalendarMonth,
                title = "Make a Reservation",
                onClick = { onNavigate("reservation") }
            )

            DrawerMenuItem(
                icon = Icons.Default.Event,
                title = "Reservations",
                onClick = { onNavigate("reservations") }
            )

            DrawerMenuItem(
                icon = Icons.Default.AccountBalance,
                title = "My Balance",
                onClick = { onNavigate("balance") }
            )

            DrawerMenuItem(
                icon = Icons.Default.Person,
                title = "My Account",
                onClick = { onNavigate("account") }
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    tint = DarkBlueGray
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Charles LeKirk",
                    fontSize = 14.sp,
                    color = DeepNavy
                )
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = DeepNavy,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            color = DeepNavy,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun HomeScreen() {
    var selectedEvent by remember { mutableStateOf<CommunityEvent?>(null) }
    val listState = rememberLazyListState()

    val scrollPercentage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo

            if (visibleItemsInfo.isEmpty()) {
                0f
            } else {
                val firstVisibleItem = visibleItemsInfo.first()
                val firstItemOffset = firstVisibleItem.offset
                val firstItemIndex = firstVisibleItem.index

                val totalItemsCount = layoutInfo.totalItemsCount
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

                val averageItemHeight = if (visibleItemsInfo.isNotEmpty()) {
                    visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size
                } else {
                    1
                }

                val totalContentHeight = totalItemsCount * averageItemHeight
                val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1)

                val currentScrollOffset = (firstItemIndex * averageItemHeight - firstItemOffset).coerceAtLeast(0)

                (currentScrollOffset.toFloat() / maxScrollOffset).coerceIn(0f, 1f)
            }
        }
    }

    val showScrollbar by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > 0 &&
                    (listState.canScrollForward || listState.canScrollBackward)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(LightLavender)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Home",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Community Events",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MediumGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            items(sampleEvents) { event ->
                EventCard(
                    event = event,
                    onClick = { selectedEvent = event },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }

        if (showScrollbar) {
            val trackHeight = 300f
            val thumbHeight = 80f

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .width(4.dp)
                    .height(trackHeight.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .height(thumbHeight.dp)
                        .fillMaxWidth()
                        .offset(y = ((trackHeight - thumbHeight) * scrollPercentage).dp)
                        .background(DarkBlueGray, RoundedCornerShape(2.dp))
                )
            }
        }

        selectedEvent?.let { event ->
            EventScheduleDialog(
                event = event,
                onDismiss = { selectedEvent = null }
            )
        }
    }
}

@Composable
fun EventCard(
    event: CommunityEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = event.timeRange,
                    fontSize = 11.sp,
                    color = MediumGray,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = event.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    maxLines = 1
                )
            }

            Text(
                text = event.description,
                fontSize = 13.sp,
                color = DeepNavy.copy(alpha = 0.6f),
                lineHeight = 18.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun EventScheduleDialog(
    event: CommunityEvent,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DeepNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Available Schedules",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    event.schedules.forEach { schedule ->
                        ScheduleItem(schedule)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(schedule: EventSchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightLavender
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = schedule.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )

                Text(
                    text = schedule.time,
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = schedule.venue,
                fontSize = 12.sp,
                color = DeepNavy.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun Reservation() {
    ScreenContent("Make a Reservation")
}

@Composable
fun Reservations() {
    ScreenContent("Reservations")
}

@Composable
fun Balance() {
    ScreenContent("My Balance")
}

@Composable
fun Account() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightLavender)
            .padding(20.dp)
    ) {
        Text(
            text = "My Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8EBFA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👤",
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Charles LeKirk",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = "Homeowner",
                            fontSize = 14.sp,
                            color = MediumGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFB5B9D8))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = DeepNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Login Credentials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy
                    )
                }

                AccountTextField(
                    label = "Username",
                    value = "charleslekirk"
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccountTextField(
                    label = "Email",
                    value = "charleslekirk@gmail.com"
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccountPasswordField(
                    label = "Password",
                    value = "••••••••••••••••"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFB5B9D8))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = DeepNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personal Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD8DBFA).copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        PersonalInfoRow("Name:", "Charles lekirk")
                        Spacer(modifier = Modifier.height(8.dp))
                        PersonalInfoRow("Contact Num:", "09587653211")
                        Spacer(modifier = Modifier.height(8.dp))
                        PersonalInfoRow("Address:", "Blk 20 Lot 21c Ginuntuang St.")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTextField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = DeepNavy.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFD8DBFA).copy(alpha = 0.5f)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = DeepNavy,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun AccountPasswordField(
    label: String,
    value: String
) {
    var showPassword by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = DeepNavy.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFD8DBFA).copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showPassword) "mypassword123" else value,
                    fontSize = 14.sp,
                    color = DeepNavy
                )
                IconButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = DeepNavy,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = DeepNavy.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = DeepNavy,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun ScreenContent(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightLavender),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavy
        )
    }
}