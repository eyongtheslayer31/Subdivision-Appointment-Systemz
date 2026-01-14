package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.util.UUID

// --- 1. DATA MODELS & ENUMS ---

enum class ReservationStatus(val displayName: String, val color: Color) {
    PENDING("Pending", Color(0xFFF39C12)),
    ACTIVE("Active", Color(0xFF27AE60)),
    COMPLETED("Completed", Color(0xFF2980B9)),
    REJECTED("Rejected", Color(0xFFC0392B))
}

data class ReservationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val status: ReservationStatus,
    val location: String = "Clubhouse"
)

// --- 2. SHARED VIEWMODEL ---

class ReservationViewModel : ViewModel() {
    private val _reservations = mutableStateListOf<ReservationItem>(
        ReservationItem("1", "Basketball Court", "Oct 24, 2023", "02:00 PM", ReservationStatus.ACTIVE),
        ReservationItem("2", "Swimming Pool", "Oct 25, 2023", "10:00 AM", ReservationStatus.PENDING)
    )
    val reservations: List<ReservationItem> get() = _reservations

    fun addReservation(title: String, date: String, time: String) {
        val newItem = ReservationItem(
            title = title,
            date = date,
            time = time,
            status = ReservationStatus.PENDING
        )
        _reservations.add(0, newItem)
    }
}

// --- 3. MAIN ACTIVITY ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val resViewModel: ReservationViewModel = viewModel()

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DarkBlueGray,
                    background = LightLavender,
                    surface = Color.White
                )
            ) {
                var currentUser by remember { mutableStateOf<User?>(null) }

                if (currentUser == null) {
                    LoginScreen(onLoginSuccess = { user -> currentUser = user })
                } else {
                    ProfileSidebarApp(currentUser!!, resViewModel, onLogout = { currentUser = null })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSidebarApp(user: User, viewModel: ReservationViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("home") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerContent(
                user = user,
                onNavigate = { route ->
                    if (route == "logout") onLogout()
                    else {
                        currentRoute = route
                        navController.navigate(route)
                    }
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
                                when(currentRoute) {
                                    "home" -> "Home"
                                    "reservation" -> "New Reservation"
                                    "reservations" -> "My History"
                                    "account" -> "Profile"
                                    else -> "App"
                                },
                                color = DeepNavy,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, null, tint = DarkBlueGray)
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(user) }
                        composable("reservation") { Reservation(user, viewModel) }
                        composable("reservations") { Reservations(viewModel) }
                        composable("account") { Account(user) }
                    }
                }
            }
        }
    )
}

// --- 4. RESERVATIONS HISTORY SCREEN ---

@Composable
fun Reservations(viewModel: ReservationViewModel) {
    val allReservations = viewModel.reservations
    var selectedFilter by remember { mutableStateOf<ReservationStatus?>(null) }

    val filteredList = remember(selectedFilter, allReservations.size) {
        if (selectedFilter == null) allReservations
        else allReservations.filter { it.status == selectedFilter }
    }

    Column(modifier = Modifier.fillMaxSize().background(LightLavender).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChipItem("All", selectedFilter == null) { selectedFilter = null }
            ReservationStatus.entries.forEach { status ->
                FilterChipItem(status.displayName, selectedFilter == status) { selectedFilter = status }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(filteredList) { item ->
                ReservationCard(item)
            }
            if (filteredList.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records found", color = MediumGray)
                    }
                }
            }
        }
    }
}

// --- 5. UI COMPONENTS & HELPERS ---

@Composable
fun ReservationCard(reservation: ReservationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reservation.title, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("${reservation.date} • ${reservation.time}", fontSize = 13.sp, color = MediumGray)
            }
            Surface(color = reservation.status.color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(reservation.status.displayName, color = reservation.status.color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FilterChipItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) DarkBlueGray else Color.White,
        shadowElevation = 2.dp
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) Color.White else DeepNavy, fontSize = 12.sp)
    }
}

@Composable
fun ProfileDrawerContent(user: User, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = LightLavender) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
            DrawerMenuItem(Icons.Default.Home, "Home") { onNavigate("home") }
            DrawerMenuItem(Icons.Default.CalendarMonth, "Make a Reservation") { onNavigate("reservation") }
            // Role-based visibility for Reservations History tab
            if (user.role != "Admin") {
                DrawerMenuItem(Icons.Default.Event, "Reservations") { onNavigate("reservations") }
            }
            DrawerMenuItem(Icons.Default.Person, "My Account") { onNavigate("account") }
            Spacer(Modifier.weight(1f))
            DrawerMenuItem(Icons.Default.Close, "Logout") { onNavigate("logout") }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, Modifier.size(40.dp).clip(CircleShape), tint = DarkBlueGray)
                Spacer(Modifier.width(12.dp))
                Text(user.name, color = DeepNavy)
            }
        }
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DeepNavy)
        Spacer(Modifier.width(16.dp))
        Text(title, color = DeepNavy)
    }
}
