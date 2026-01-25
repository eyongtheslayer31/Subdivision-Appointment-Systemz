package com.example.myapplication

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID

// --- 1. DATA MODELS & ENUMS ---

enum class ReservationStatus(val displayName: String, val color: Color) {
    PENDING("Pending", Color(0xFFF59E0B)),
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
    val formattedDate: String = "",
    val paymentProofUri: String? = null,
    val rejectionReason: String? = null
)

// --- 2. SHARED VIEWMODEL ---

class ReservationViewModel : ViewModel() {
    private val _reservations = mutableStateListOf<ReservationItem>()
    val reservations: List<ReservationItem> get() = _reservations

    fun loadData(context: Context) {
        val saved = ReservationRepository.loadReservations(context)
        _reservations.clear()
        _reservations.addAll(saved)
        refreshCalendarEvents()
    }

    fun refreshCalendarEvents() {
        calendarEvents.clear()
        _reservations.forEach { item ->
            if (item.status == ReservationStatus.ACTIVE || item.status == ReservationStatus.PENDING) {
                val times = item.time.split(" - ")
                val startT = times[0]
                val endT = if (times.size > 1) times[1] else ""
                
                val user = UserRepository.users.find { it.name == item.reservedBy }
                
                calendarEvents.add(
                    CalendarEvent(
                        id = calendarEvents.size + 1,
                        title = item.title,
                        date = item.formattedDate,
                        startTime = startT,
                        endTime = endT,
                        venue = item.title,
                        description = "Purpose: ${item.purpose}",
                        reservedBy = item.reservedBy,
                        reserverPhone = item.contact,
                        reserverRole = user?.role ?: "User",
                        status = item.status.name
                    )
                )
            }
        }
    }

    fun addReservation(context: Context, title: String, date: String, time: String, user: String, phone: String, note: String, formattedDate: String, paymentProof: String?) {
        val newItem = ReservationItem(
            title = title,
            date = date,
            time = time,
            status = ReservationStatus.PENDING,
            reservedBy = user,
            contact = phone,
            purpose = note,
            formattedDate = formattedDate,
            paymentProofUri = paymentProof
        )
        _reservations.add(0, newItem)
        ReservationRepository.saveReservations(context, _reservations)
        refreshCalendarEvents()
    }

    fun addReservationDirect(context: Context, item: ReservationItem) {
        _reservations.add(0, item)
        ReservationRepository.saveReservations(context, _reservations)
        refreshCalendarEvents()
    }

    fun updateStatus(context: Context, id: String, newStatus: ReservationStatus, reason: String? = null) {
        val index = _reservations.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _reservations[index]
            _reservations[index] = item.copy(status = newStatus, rejectionReason = reason)
            ReservationRepository.saveReservations(context, _reservations)
            refreshCalendarEvents()
        }
    }

    fun clearAll(context: Context) {
        _reservations.clear()
        calendarEvents.clear()
        ReservationRepository.clearAll(context)
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
                resViewModel.loadData(context)
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DarkBlueGray,
                    background = LightLavender,
                    surface = Color.White
                )
            ) {
                var currentUsername by remember { mutableStateOf<String?>(null) }
                val currentUser = UserRepository.users.find { it.username == currentUsername }

                if (currentUser == null) {
                    LoginScreen(onLoginSuccess = { user -> currentUsername = user.username })
                } else {
                    ProfileSidebarApp(currentUser, resViewModel, onLogout = { currentUsername = null })
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
                                    "admin_history" -> "History Logs"
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
                        composable("home") { HomeScreen(user, viewModel) }
                        composable("reservation") { Reservation(user, viewModel) }
                        composable("reservations") { Reservations(user, viewModel) }
                        composable("approval") { ApprovalScreen(viewModel) }
                        composable("admin_history") { AdminHistory(viewModel) }
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("account") }
                    .padding(horizontal = 20.dp, vertical = 16.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkBlueGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profilePictureUri != null) {
                        AsyncImage(
                            model = user.profilePictureUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(user.role, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

            DrawerMenuItem(Icons.Default.Home, "Home") { onNavigate("home") }
            DrawerMenuItem(Icons.Default.CalendarMonth, "Make a Reservation") { onNavigate("reservation") }
            
            if (user.role == "Admin") {
                DrawerMenuItem(Icons.AutoMirrored.Filled.Rule, "Approval Request") { onNavigate("approval") }
                DrawerMenuItem(Icons.Default.History, "History Logs") { onNavigate("admin_history") }
            } else {
                DrawerMenuItem(Icons.Default.Event, "Reservations") { onNavigate("reservations") }
            }
            
            DrawerMenuItem(Icons.Default.Person, "My Account") { onNavigate("account") }
            
            Spacer(Modifier.weight(1f))

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            DrawerMenuItem(Icons.AutoMirrored.Filled.Logout, "Logout") { onNavigate("logout") }
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
    val context = LocalContext.current
    val pendingReservations = viewModel.reservations.filter { it.status == ReservationStatus.PENDING }

    Column(modifier = Modifier.fillMaxSize().background(LightLavender).padding(16.dp)) {
        Text("Pending Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pendingReservations) { item ->
                ApprovalCard(item, onAccept = {
                    viewModel.updateStatus(context, item.id, ReservationStatus.ACTIVE)
                }, onReject = { reason ->
                    viewModel.updateStatus(context, item.id, ReservationStatus.REJECTED, reason)
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
fun ApprovalCard(reservation: ReservationItem, onAccept: () -> Unit, onReject: (String) -> Unit) {
    var showPaymentProof by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

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
                    Text("PENDING", color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))
            Text("Reserved by: ${reservation.reservedBy}", fontSize = 14.sp, color = DeepNavy)
            Text("Contact: ${reservation.contact}", fontSize = 14.sp, color = DeepNavy)
            Text("Purpose: ${reservation.purpose}", fontSize = 14.sp, color = DeepNavy)
            
            if (reservation.paymentProofUri != null) {
                Spacer(Modifier.height(12.dp))
                Text("Payment Proof:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(Modifier.height(4.dp))
                AsyncImage(
                    model = reservation.paymentProofUri,
                    contentDescription = "Payment Proof",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPaymentProof = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
                Button(onClick = { showRejectDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), shape = RoundedCornerShape(8.dp)) {
                    Text("Reject")
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(8.dp)) {
                    Text("Accept")
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Reservation") },
            text = {
                Column {
                    Text("Please provide a reason for rejecting this reservation.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectionReason)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPaymentProof && reservation.paymentProofUri != null) {
        Dialog(onDismissRequest = { showPaymentProof = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { showPaymentProof = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                    AsyncImage(
                        model = reservation.paymentProofUri,
                        contentDescription = "Full Payment Proof",
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

// --- 5. ADMIN HISTORY SCREEN ---

@Composable
fun AdminHistory(viewModel: ReservationViewModel) {
    val allReservations = viewModel.reservations.filter { 
        it.status != ReservationStatus.PENDING 
    }
    var selectedFilter by remember { mutableStateOf<ReservationStatus?>(null) }

    val filteredList = remember(selectedFilter, allReservations.size) {
        if (selectedFilter == null) allReservations
        else allReservations.filter { it.status == selectedFilter }
    }

    Column(modifier = Modifier.fillMaxSize().background(LightLavender).padding(16.dp)) {
        Text("Reservation Logs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem("All", selectedFilter == null) { selectedFilter = null }
            FilterChipItem("Active", selectedFilter == ReservationStatus.ACTIVE) { selectedFilter = ReservationStatus.ACTIVE }
            FilterChipItem("Rejected", selectedFilter == ReservationStatus.REJECTED) { selectedFilter = ReservationStatus.REJECTED }
            FilterChipItem("Completed", selectedFilter == ReservationStatus.COMPLETED) { selectedFilter = ReservationStatus.COMPLETED }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(filteredList) { item ->
                AdminLogCard(item)
            }
            if (filteredList.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No logs found", color = MediumGray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLogCard(reservation: ReservationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(reservation.title, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Text("Reserved by: ${reservation.reservedBy}", fontSize = 12.sp, color = DeepNavy.copy(alpha = 0.7f))
                    Text("${reservation.date} • ${reservation.time}", fontSize = 12.sp, color = MediumGray)
                }
                Surface(color = reservation.status.color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(reservation.status.displayName, color = reservation.status.color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (reservation.status == ReservationStatus.REJECTED && !reservation.rejectionReason.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text("Reason: ${reservation.rejectionReason}", color = Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --- 6. RESERVATIONS HISTORY SCREEN (USER) ---

@Composable
fun Reservations(user: User, viewModel: ReservationViewModel) {
    val allReservations = viewModel.reservations.filter { it.reservedBy == user.name }
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

// --- 7. UI COMPONENTS & HELPERS ---

@Composable
fun ReservationCard(reservation: ReservationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(reservation.title, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Text("${reservation.date} • ${reservation.time}", fontSize = 13.sp, color = MediumGray)
                }
                Surface(color = reservation.status.color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(reservation.status.displayName, color = reservation.status.color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (reservation.status == ReservationStatus.REJECTED && !reservation.rejectionReason.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text("Reason: ${reservation.rejectionReason}", color = Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
