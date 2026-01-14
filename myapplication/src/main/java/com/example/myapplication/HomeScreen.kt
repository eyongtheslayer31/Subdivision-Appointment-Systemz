package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.util.Locale
import java.util.Calendar

@Composable
fun HomeScreen(user: User) {
    var selectedEvent by remember { mutableStateOf<CommunityEvent?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize().background(LightLavender)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
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
            
            // Extra space at bottom
            item { Spacer(modifier = Modifier.height(100.dp)) }
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

        // Admin-only Add Event Button
        if (user.role == "Admin") {
            LargeFloatingActionButton(
                onClick = { showAddEventDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = DarkBlueGray,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
            }
        }

        selectedEvent?.let { event ->
            EventScheduleDialog(
                event = event,
                onDismiss = { selectedEvent = null }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                onDismiss = { showAddEventDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(onDismiss: () -> Unit) {
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var selectedTimeSlots by remember { mutableStateOf(setOf<String>()) }
    var purpose by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var expandedVenue by remember { mutableStateOf(false) }
    var expandedDate by remember { mutableStateOf(false) }
    
    // Generate dates for the current month
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    
    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    
    val availableDates = (currentDay..daysInMonth).map { day ->
        String.format(Locale.US, "%s %d, %d", monthNames[currentMonth], day, currentYear)
    }
    
    var selectedDate by remember { mutableStateOf(availableDates.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
                        text = "Add New Event",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = DeepNavy)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection Dropdown
                Text("Select Date", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = expandedDate,
                    onExpandedChange = { expandedDate = !expandedDate },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE8EBFA),
                            unfocusedContainerColor = Color(0xFFE8EBFA),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedDate, onDismissRequest = { expandedDate = false }) {
                        availableDates.forEach { date ->
                            DropdownMenuItem(
                                text = { Text(date) },
                                onClick = {
                                    selectedDate = date
                                    expandedDate = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown for Facility (Venue)
                Text("Select Venue", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = expandedVenue,
                    onExpandedChange = { expandedVenue = !expandedVenue },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFacility?.name ?: "Choose a venue",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVenue) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE8EBFA),
                            unfocusedContainerColor = Color(0xFFE8EBFA),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedVenue, onDismissRequest = { expandedVenue = false }) {
                        availableFacilities.forEach { facility ->
                            DropdownMenuItem(
                                text = { Text(facility.name) },
                                onClick = {
                                    selectedFacility = facility
                                    expandedVenue = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Event Purpose / Title", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE8EBFA),
                        unfocusedContainerColor = Color(0xFFE8EBFA),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    placeholder = { Text("e.g. General Assembly", fontSize = 14.sp) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time Slots
                Text("Available Time Slots", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(timeSlots) { slot ->
                        val isSelected = selectedTimeSlots.contains(slot)
                        Box(
                            modifier = Modifier
                                .height(55.dp) // FIXED HEIGHT for all slots
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFFE0F2F1))
                                .clickable {
                                    selectedTimeSlots = if (isSelected) selectedTimeSlots - slot else selectedTimeSlots + slot
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val times = slot.split(" - ")
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = times[0],
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White else Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "to ${times[1]}",
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White else Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedFacility != null && selectedTimeSlots.isNotEmpty() && purpose.isNotEmpty()) {
                            val sortedSlots = selectedTimeSlots.toList().sortedBy { timeToMinutes(it.split(" - ")[0]) }
                            val startT = sortedSlots.first().split(" - ").first()
                            val endT = sortedSlots.last().split(" - ").last()
                            
                            val newEvent = CommunityEvent(
                                id = sampleEvents.size + 1,
                                title = purpose,
                                timeRange = "$selectedDate, $startT – $endT",
                                description = "Community event at ${selectedFacility!!.name}.",
                                schedules = listOf(EventSchedule(selectedDate, "$startT - $endT", selectedFacility!!.name))
                            )
                            sampleEvents.add(0, newEvent)
                            onDismiss()
                        } else {
                            errorMessage = "Please fill all fields"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Event", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
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
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.65f) 
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        modifier = Modifier.weight(1f)
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

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Available Schedules",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(event.schedules) { schedule ->
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightLavender.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // DATE and TIME now stacked vertically to prevent overlap
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = DarkBlueGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = schedule.date,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = schedule.time,
                        fontSize = 14.sp,
                        color = MediumGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = DarkBlueGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = schedule.venue,
                    fontSize = 14.sp,
                    color = DeepNavy.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
