package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*
import java.text.SimpleDateFormat

@Composable
fun HomeScreen(user: User, reservationViewModel: ReservationViewModel = viewModel()) {
    var selectedEvent by remember { mutableStateOf<CommunityEvent?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val myRecentUpdates = reservationViewModel.reservations.filter { 
        it.reservedBy == user.name && (it.status == ReservationStatus.ACTIVE || it.status == ReservationStatus.REJECTED)
    }

    val isAdmin = user.role == "Admin"
    val weeklyStats = if (isAdmin) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.time
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        val weeklyReservations = reservationViewModel.reservations.filter {
            try {
                val date = sdf.parse(it.formattedDate)
                date != null && !date.before(weekStart)
            } catch (_: Exception) {
                false
            }
        }
        
        object {
            val active = weeklyReservations.count { it.status == ReservationStatus.ACTIVE }
            val rejected = weeklyReservations.count { it.status == ReservationStatus.REJECTED }
            val completed = weeklyReservations.count { it.status == ReservationStatus.COMPLETED }
            val totalAccounts = UserRepository.users.size
        }
    } else null

    val scrollPercentage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) 0f else {
                val firstVisibleItem = visibleItemsInfo.first()
                val firstItemOffset = firstVisibleItem.offset
                val firstItemIndex = firstVisibleItem.index
                val totalItemsCount = layoutInfo.totalItemsCount
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val averageItemHeight = if (visibleItemsInfo.isNotEmpty()) visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size else 1
                val totalContentHeight = totalItemsCount * averageItemHeight
                val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1)
                val currentScrollOffset = (firstItemIndex * averageItemHeight - firstItemOffset).coerceAtLeast(0)
                (currentScrollOffset.toFloat() / maxScrollOffset).coerceIn(0f, 1f)
            }
        }
    }

    val showScrollbar by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > 0 && (listState.canScrollForward || listState.canScrollBackward)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LightLavender)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Home", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(text = if (isAdmin) "Admin Dashboard" else "Community Events", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MediumGray)
                    }
                    if (!isAdmin) {
                        BadgedBox(badge = { if (myRecentUpdates.isNotEmpty()) Badge { Text(myRecentUpdates.size.toString()) } }) {
                            IconButton(onClick = { showNotifications = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = DeepNavy)
                            }
                        }
                    }
                }
            }

            if (isAdmin && weeklyStats != null) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(text = "Weekly Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(bottom = 12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Approved", weeklyStats.active.toString(), SuccessGreen, Modifier.weight(1f))
                            StatCard("Accounts", weeklyStats.totalAccounts.toString(), Color(0xFF2980B9), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Completed", weeklyStats.completed.toString(), Color(0xFF2980B9), Modifier.weight(1f))
                            StatCard("Rejected", weeklyStats.rejected.toString(), Color(0xFFC0392B), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Events", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }

            items(sampleEvents) { event ->
                EventCard(event = event, onClick = { selectedEvent = event }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp) )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (showScrollbar) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).width(4.dp).height(300.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))) {
                Box(modifier = Modifier.height(80.dp).fillMaxWidth().offset(y = ((300 - 80) * scrollPercentage).dp).background(DarkBlueGray, RoundedCornerShape(2.dp)))
            }
        }

        if (isAdmin) {
            LargeFloatingActionButton(onClick = { showAddEventDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), containerColor = DarkBlueGray, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
            }
        }

        selectedEvent?.let { event -> EventScheduleDialog(event = event, onDismiss = { selectedEvent = null }) }
        if (showAddEventDialog) { AddEventDialog(reservationViewModel, onDismiss = { showAddEventDialog = false }) }
        if (showNotifications) { NotificationsDialog(updates = myRecentUpdates, onDismiss = { showNotifications = false }) }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(text = label, fontSize = 12.sp, color = MediumGray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 24.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun NotificationsDialog(updates: List<ReservationItem>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (updates.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No new notifications", color = MediumGray) }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(updates) { NotificationItem(it) } }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(item: ReservationItem) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (item.status == ReservationStatus.ACTIVE) SuccessGreen.copy(alpha = 0.1f) else Color(0xFFC0392B).copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = if (item.status == ReservationStatus.ACTIVE) "Reservation Approved!" else "Reservation Rejected", fontWeight = FontWeight.Bold, color = if (item.status == ReservationStatus.ACTIVE) SuccessGreen else Color(0xFFC0392B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Your request for ${item.title} on ${item.date} has been ${item.status.displayName.lowercase()}.", fontSize = 13.sp, color = DeepNavy.copy(alpha = 0.8f) )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(reservationViewModel: ReservationViewModel, onDismiss: () -> Unit) {
    val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
    val now = remember { Calendar.getInstance(phTimeZone) }
    val currentYear = now.get(Calendar.YEAR)
    val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var selectedTimeSlots by remember { mutableStateOf(setOf<String>()) }
    var purpose by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var expandedVenue by remember { mutableStateOf(false) }
    
    val currentMonth = now.get(Calendar.MONTH)
    val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val availableDatesInfo = remember(now) {
        (1..daysInMonth).map { day ->
            val dateCal = Calendar.getInstance(phTimeZone).apply {
                set(currentYear, currentMonth, day, 23, 59, 59)
            }
            val isPast = dateCal.before(now)
            
            val displayCal = Calendar.getInstance(phTimeZone).apply {
                set(currentYear, currentMonth, day)
            }
            val dayName = displayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
            val fullDate = "${monthNames[currentMonth]} $day, $currentYear"
            
            Triple(day, dayName, fullDate) to isPast
        }
    }
    
    var selectedDate by remember { 
        val firstValid = availableDatesInfo.find { !it.second }?.first?.third ?: availableDatesInfo.first().first.third
        mutableStateOf(firstValid) 
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.98f).padding(vertical = 16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Create Event", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = DeepNavy) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Select Date", fontSize = 13.sp, color = DeepNavy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    items(availableDatesInfo) { info ->
                        val data = info.first
                        val isPast = info.second
                        val isSelected = selectedDate == data.third
                        
                        Card(
                            onClick = { if (!isPast) selectedDate = data.third }, 
                            shape = RoundedCornerShape(12.dp), 
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPast) Color(0xFFE0E0E0).copy(alpha = 0.5f)
                                               else if (isSelected) DarkBlueGray 
                                               else Color(0xFFF0F2FA)
                            ), 
                            modifier = Modifier.width(60.dp).height(75.dp),
                            enabled = !isPast
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = data.second.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isPast) Color.Gray else if (isSelected) Color.White else MediumGray)
                                Text(text = data.first.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (isPast) Color.Gray else if (isSelected) Color.White else DeepNavy)
                                Text(text = monthNames[currentMonth].substring(0, 3), fontSize = 10.sp, color = if (isPast) Color.Gray else if (isSelected) Color.White.copy(alpha = 0.8f) else MediumGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Venue", fontSize = 13.sp, color = DeepNavy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expandedVenue, onExpandedChange = { expandedVenue = !expandedVenue }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedFacility?.name ?: "Choose a venue", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVenue) }, colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFE8EBFA), unfocusedContainerColor = Color(0xFFE8EBFA), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = expandedVenue, onDismissRequest = { expandedVenue = false }) {
                        availableFacilities.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { selectedFacility = it; expandedVenue = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Event Title", fontSize = 13.sp, color = DeepNavy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = purpose, onValueChange = { purpose = it }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFE8EBFA), unfocusedContainerColor = Color(0xFFE8EBFA), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), singleLine = true, placeholder = { Text("e.g. General Assembly", fontSize = 14.sp) })
                Spacer(modifier = Modifier.height(20.dp))
                Text("Time Slots", fontSize = 13.sp, color = DeepNavy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(timeSlots) { slot ->
                        val isSelected = selectedTimeSlots.contains(slot)
                        
                        // Check if past or ALREADY OCCUPIED BY A RESERVATION
                        val slotTimes = slot.split(" - ")
                        val sStart = timeToMinutes(slotTimes[0])
                        val sEnd = timeToMinutes(slotTimes[1])
                        
                        val isPastSlot = try {
                            val dayNum = selectedDate.split(" ")[1].replace(",", "").toInt()
                            val slotEndStr = slot.split(" - ")[1]
                            val slotCal = Calendar.getInstance(phTimeZone).apply {
                                set(currentYear, currentMonth, dayNum)
                                val timeParts = slotEndStr.replace("NN", "PM").trim().split(" ")
                                val hm = timeParts[0].split(":")
                                var h = hm[0].toInt()
                                val m = hm[1].toInt()
                                if (timeParts[1] == "PM" && h < 12) h += 12
                                if (timeParts[1] == "AM" && h == 12) h = 0
                                set(Calendar.HOUR_OF_DAY, h)
                                set(Calendar.MINUTE, m)
                                set(Calendar.SECOND, 0)
                            }
                            slotCal.before(now)
                        } catch(_: Exception) { false }

                        val formattedDate = try {
                            val dayNum = selectedDate.split(" ")[1].replace(",", "").toInt()
                            String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth + 1, dayNum)
                        } catch (_: Exception) { "" }

                        val isTaken = if (selectedFacility != null) {
                            reservationViewModel.reservations.any { e ->
                                e.title == selectedFacility?.name &&
                                e.formattedDate == formattedDate &&
                                e.status != ReservationStatus.REJECTED &&
                                !e.isDeletedByAdmin &&
                                maxOf(sStart, timeToMinutes(e.time.split(" - ")[0])) < minOf(sEnd, timeToMinutes(e.time.split(" - ")[1]))
                            }
                        } else false
                        
                        Box(modifier = Modifier.height(50.dp).clip(RoundedCornerShape(8.dp)).background(if (isPastSlot || isTaken) Color(0xFFE0E0E0).copy(alpha = 0.5f) else if (isSelected) Color(0xFF1E88E5) else Color(0xFFE8EBFA)).clickable(enabled = !isPastSlot && !isTaken) { selectedTimeSlots = if (isSelected) selectedTimeSlots - slot else selectedTimeSlots + slot }.padding(4.dp), contentAlignment = Alignment.Center) {
                            val times = slot.split(" - ")
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(times[0], fontSize = 8.sp, color = if (isPastSlot || isTaken) Color.Gray else if (isSelected) Color.White else DeepNavy, textAlign = TextAlign.Center)
                                Text("to ${times[1]}", fontSize = 8.sp, color = if (isPastSlot || isTaken) Color.Gray else if (isSelected) Color.White else DeepNavy, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                if (errorMessage.isNotEmpty()) { Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { if (selectedFacility != null && selectedTimeSlots.isNotEmpty() && purpose.isNotEmpty()) { val sortedSlots = selectedTimeSlots.toList().sortedBy { timeToMinutes(it.split(" - ")[0]) }; val startT = sortedSlots.first().split(" - ").first(); val endT = sortedSlots.last().split(" - ").last(); val newEvent = CommunityEvent(id = sampleEvents.size + 1, title = purpose, timeRange = "$selectedDate, $startT - $endT", description = "Community event at ${selectedFacility!!.name}.", schedules = listOf(EventSchedule(selectedDate, "$startT - $endT", selectedFacility!!.name))); sampleEvents.add(0, newEvent); reservationViewModel.refreshCalendarEvents(); onDismiss() } else { errorMessage = "Please fill all fields" } }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray), shape = RoundedCornerShape(14.dp) ) { Text("Broadcast Event", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun EventCard(event: CommunityEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().height(130.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = event.timeRange, fontSize = 11.sp, color = MediumGray, maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = event.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy, maxLines = 1)
            }
            Text(text = event.description, fontSize = 13.sp, color = DeepNavy.copy(alpha = 0.6f), lineHeight = 18.sp, maxLines = 2)
        }
    }
}

@Composable
fun EventScheduleDialog(event: CommunityEvent, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.65f)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = event.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = DeepNavy) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Available Schedules", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DeepNavy)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) { items(event.schedules) { ScheduleItem(it) } }
            }
        }
    }
}

@Composable
fun ScheduleItem(schedule: EventSchedule) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = LightLavender.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBlueGray)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = schedule.date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Text(text = schedule.time, fontSize = 14.sp, color = MediumGray, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBlueGray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = schedule.venue, fontSize = 14.sp, color = DeepNavy.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            }
        }
    }
}
