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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Rule
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
import androidx.compose.ui.platform.LocalContext
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
    PENDING("Pending", PendingOrange),
    ACTIVE("Active", SuccessGreen),
    COMPLETED("Completed", Color(0xFF2980B9)),
    REJECTED("Rejected", Color(0xFFC0392B))
}

data class ReservationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val status: ReservationStatus,
    val reservedBy: String = "",
    val contact: String = "",
    val purpose: String = "",
    val formattedDate: String = ""
)

// --- 2. SHARED VIEWMODEL ---

class ReservationViewModel : ViewModel() {
    private val _reservations = mutableStateListOf<ReservationItem>()
    val reservations: List<ReservationItem> get() = _reservations

    fun addReservation(title: String, date: String, time: String, user: String, phone: String, note: String, formattedDate: String) {
        val newItem = ReservationItem(
            title = title,
            date = date,
            time = time,
            status = ReservationStatus.PENDING,
            reservedBy = user,
            contact = phone,
            purpose = note,
            formattedDate = formattedDate
        )
        _reservations.add(0, newItem)
    }

    fun updateStatus(id: String, newStatus: ReservationStatus) {
        val index = _reservations.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _reservations[index]
            _reservations[index] = item.copy(status = newStatus)
            
            if (newStatus == ReservationStatus.ACTIVE) {
                val times = item.time.split(" - ")
                calendarEvents.add(
                    CalendarEvent(
                        id = calendarEvents.size + 1,
                        title = item.title,
                        date = item.formattedDate,
                        startTime = times[0],
                        endTime = if (times.size > 1) times[1] else "",
                        venue = item.title,
                        description = "Purpose: ${item.purpose}",
                        reservedBy = item.reservedBy,
                        reserverPhone = item.contact
                    )
                )
            }
        }
    }
}

// --- 3. MAIN ACTIVITY ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val resViewModel: ReservationViewModel = viewModel()
            
            LaunchedEffect(Unit) {
                UserRepository.loadPersistedData(context)
            }

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
                                    "approval" -> "Approval Request"
                                    "account" -> "Profile"
                                    else -> "App"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, null, tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = DeepNavy
                        )
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(user) }
                        composable("reservation") { Reservation(user, viewModel) }
                        composable("reservations") { Reservations(viewModel) }
                        composable("approval") { ApprovalScreen(viewModel) }
                        composable("account") { Account(user) }
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileDrawerContent(user: User, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = DeepNavy,
        drawerContentColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
            DrawerMenuItem(Icons.Default.Home, "Home") { onNavigate("home") }
            DrawerMenuItem(Icons.Default.CalendarMonth, "Make a Reservation") { onNavigate("reservation") }
            
            if (user.role == "Admin") {
                DrawerMenuItem(Icons.AutoMirrored.Filled.Rule, "Approval Request") { onNavigate("approval") }
            } else {
                DrawerMenuItem(Icons.Default.Event, "Reservations") { onNavigate("reservations") }
            }
            
            DrawerMenuItem(Icons.Default.Person, "My Account") { onNavigate("account") }
            Spacer(Modifier.weight(1f))
            DrawerMenuItem(Icons.AutoMirrored.Filled.Logout, "Logout") { onNavigate("logout") }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkBlueGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(user.role, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// --- 4. ADMIN APPROVAL SCREEN ---

@Composable
fun ApprovalScreen(viewModel: ReservationViewModel) {
    val pendingReservations = viewModel.reservations.filter { it.status == ReservationStatus.PENDING }

    Column(modifier = Modifier.fillMaxSize().background(LightLavender).padding(16.dp)) {
        Text("Pending Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pendingReservations) { item ->
                ApprovalCard(item, onAccept = {
                    viewModel.updateStatus(item.id, ReservationStatus.ACTIVE)
                }, onReject = {
                    viewModel.updateStatus(item.id, ReservationStatus.REJECTED)
                })
            }
            if (pendingReservations.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending requests", color = MediumGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovalCard(reservation: ReservationItem, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(reservation.title, fontWeight = FontWeight.Bold, color = DeepNavy, fontSize = 18.sp)
                    Text(reservation.date, color = MediumGray, fontSize = 14.sp)
                    Text(reservation.time, color = MediumGray, fontSize = 14.sp)
                }
                Surface(color = SoftBlue, shape = RoundedCornerShape(8.dp)) {
                    Text("PENDING", color = PendingOrange, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Text("Reserved by: ${reservation.reservedBy}", fontSize = 14.sp, color = DeepNavy)
            Text("Contact: ${reservation.contact}", fontSize = 14.sp, color = DeepNavy)
            Text("Purpose: ${reservation.purpose}", fontSize = 14.sp, color = DeepNavy)
            
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
                Button(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), shape = RoundedCornerShape(8.dp)) {
                    Text("Reject")
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(8.dp)) {
                    Text("Accept")
                }
            }
        }
    }
}

// --- 5. RESERVATIONS HISTORY SCREEN ---

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

// --- 6. UI COMPONENTS & HELPERS ---

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
