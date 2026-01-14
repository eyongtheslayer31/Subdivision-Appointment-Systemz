package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

// Helper to convert time string to minutes for comparison
fun timeToMinutes(time: String): Int {
    val cleanTime = time.replace("NN", "PM").trim()
    val parts = cleanTime.split(" ")
    if (parts.size < 2) return 0
    val hMinutes = parts[0].split(":")
    if (hMinutes.size < 2) return 0
    var hours = hMinutes[0].toInt()
    val minutes = hMinutes[1].toInt()
    val amPm = parts[1]

    if (amPm == "PM" && hours < 12) hours += 12
    if (amPm == "AM" && hours == 12) hours = 0

    return hours * 60 + minutes
}

@Composable
fun Reservation(user: User, viewModel: ReservationViewModel = viewModel()) {
    var viewMode by remember { mutableStateOf("month") }
    var selectedDate by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var showEventDetails by remember { mutableStateOf(false) }
    var selectedEvents by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    // Helper function to get events for a specific date
    fun getEventsForDate(year: Int, month: Int, day: Int): List<CalendarEvent> {
        val dateString = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        return calendarEvents.filter { it.date == dateString }
    }

    // Helper function to get days in month
    fun getDaysInMonth(month: Int, year: Int): Int {
        return when (month) {
            0, 2, 4, 6, 7, 9, 11 -> 31
            3, 5, 8, 10 -> 30
            1 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 30
        }
    }

    // Helper function to get first day of week (0 = Sunday)
    fun getFirstDayOfMonth(month: Int, year: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        return cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    }

    // Helper to check if a date is in the past
    fun isDateInPast(year: Int, month: Int, day: Int): Boolean {
        val today = Calendar.getInstance()
        val compareDate = Calendar.getInstance()
        compareDate.set(year, month, day, 23, 59, 59)
        return compareDate.before(today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightLavender)
    ) {
        // Top bar with year selector and view mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Year selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { selectedYear-- },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("<", color = DeepNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = selectedYear.toString(),
                    color = DeepNavy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = { selectedYear++ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(">", color = DeepNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // View mode selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewMode = "month" },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (viewMode == "month") DarkBlueGray else Color.White,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Month view",
                        tint = if (viewMode == "month") Color.White else DeepNavy
                    )
                }
            }
        }

        if (viewMode == "month") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightLavender)
            ) {
                // Month name
                Text(
                    text = monthNames[selectedMonth],
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                )

                // Days of week header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            color = MediumGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val daysInMonth = getDaysInMonth(selectedMonth, selectedYear)
                val firstDayOfWeek = getFirstDayOfMonth(selectedMonth, selectedYear)

                // Calendar grid
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (week in 0..5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (day in 0..6) {
                                val dayNumber = week * 7 + day - firstDayOfWeek + 1
                                val isValidDay = dayNumber in 1..daysInMonth
                                val isPast = if (isValidDay) isDateInPast(selectedYear, selectedMonth, dayNumber) else false
                                val hasEvents = if (isValidDay) {
                                    getEventsForDate(selectedYear, selectedMonth, dayNumber).isNotEmpty()
                                } else false

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                dayNumber == selectedDate && isValidDay -> DarkBlueGray
                                                isValidDay -> if (isPast) Color.White.copy(alpha = 0.5f) else Color.White
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = isValidDay) {
                                            selectedDate = dayNumber
                                            val events = getEventsForDate(selectedYear, selectedMonth, dayNumber)
                                            if (events.isNotEmpty()) {
                                                selectedEvents = events
                                                showEventDetails = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isValidDay) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                color = when {
                                                    dayNumber == selectedDate -> Color.White
                                                    isPast -> DeepNavy.copy(alpha = 0.3f)
                                                    else -> DeepNavy
                                                },
                                                fontSize = 14.sp,
                                                fontWeight = if (dayNumber == selectedDate) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasEvents) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (dayNumber == selectedDate) Color.White else if (isPast) DarkBlueGray.copy(alpha = 0.3f) else DarkBlueGray
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Month navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear--
                            } else {
                                selectedMonth--
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous", color = DeepNavy, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (selectedMonth == 11) {
                                selectedMonth = 0
                                selectedYear++
                            } else {
                                selectedMonth++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Schedule Button
                val isSelectedPast = isDateInPast(selectedYear, selectedMonth, selectedDate)
                Button(
                    onClick = { if (!isSelectedPast) showScheduleDialog = true },
                    enabled = !isSelectedPast,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedPast) Color.LightGray else DarkBlueGray,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = if (isSelectedPast) Color.Gray else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSelectedPast) "Cannot Schedule Past Date" else "Schedule Facility",
                        color = if (isSelectedPast) Color.Gray else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Event details dialog
        if (showEventDetails && selectedEvents.isNotEmpty()) {
            CalendarEventDialog(
                date = String.format(Locale.US, "%s %d, %d", monthNames[selectedMonth], selectedDate, selectedYear),
                events = selectedEvents,
                isAdmin = user.role == "Admin",
                onDismiss = { showEventDetails = false }
            )
        }

        // Schedule dialog
        if (showScheduleDialog) {
            ScheduleFacilityDialog(
                user = user,
                viewModel = viewModel,
                selectedDate = selectedDate,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                monthNames = monthNames,
                onDismiss = { showScheduleDialog = false }
            )
        }
    }
}

@Composable
fun CalendarEventDialog(
    date: String,
    events: List<CalendarEvent>,
    isAdmin: Boolean,
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
                    Column {
                        Text(
                            text = "Events",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = date,
                            fontSize = 14.sp,
                            color = MediumGray
                        )
                    }

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

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    events.forEach { event ->
                        CalendarEventItem(event, isAdmin)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventItem(event: CalendarEvent, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightLavender
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = DarkBlueGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.startTime} - ${event.endTime}",
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = event.venue,
                fontSize = 12.sp,
                color = DeepNavy.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.description,
                fontSize = 12.sp,
                color = DeepNavy.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )

            if (isAdmin && (event.reservedBy.isNotEmpty() || event.reserverPhone.isNotEmpty())) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DeepNavy.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Reserved by: ${event.reservedBy}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                Text(
                    text = "Contact: ${event.reserverPhone}",
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFacilityDialog(
    user: User,
    viewModel: ReservationViewModel,
    selectedDate: Int,
    selectedMonth: Int,
    selectedYear: Int,
    monthNames: List<String>,
    onDismiss: () -> Unit
) {
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var selectedTimeSlots by remember { mutableStateOf(setOf<String>()) }
    var purpose by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDate)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
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
                    Column {
                        Text(
                            text = "Book a Facility",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = String.format(Locale.US, "%s %d, %d", monthNames[selectedMonth], selectedDate, selectedYear),
                            fontSize = 14.sp,
                            color = MediumGray
                        )
                    }

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

                // Dropdown for Facility
                Text("Select Facility", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFacility?.name ?: "Choose a facility",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE8EBFA),
                            unfocusedContainerColor = Color(0xFFE8EBFA),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableFacilities.forEach { facility ->
                            DropdownMenuItem(
                                text = { Text(facility.name) },
                                onClick = {
                                    selectedFacility = facility
                                    expanded = false
                                    selectedTimeSlots = emptySet()
                                    errorMessage = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Phone Number", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        // Strict 11-digit limit
                        if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE8EBFA),
                        unfocusedContainerColor = Color(0xFFE8EBFA),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Note / Purpose", fontSize = 12.sp, color = DeepNavy, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE8EBFA),
                        unfocusedContainerColor = Color(0xFFE8EBFA),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    placeholder = { Text("Enter reason for reservation", fontSize = 14.sp) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Available Time Slots Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Available Time Slots",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(timeSlots) { slot ->
                                val isSelected = selectedTimeSlots.contains(slot)

                                // Precise overlap logic
                                val slotTimes = slot.split(" - ")
                                val sStart = timeToMinutes(slotTimes[0])
                                val sEnd = timeToMinutes(slotTimes[1])

                                val isTaken = if (selectedFacility != null) {
                                    calendarEvents.any { e ->
                                        e.venue == selectedFacility?.name &&
                                                e.date == formattedDate &&
                                                maxOf(sStart, timeToMinutes(e.startTime)) < minOf(sEnd, timeToMinutes(e.endTime))
                                    }
                                } else false

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isTaken -> Color.LightGray.copy(alpha = 0.5f)
                                                isSelected -> Color(0xFF1E88E5)
                                                else -> Color(0xFFE0F2F1)
                                            }
                                        )
                                        .clickable(enabled = !isTaken) {
                                            selectedTimeSlots = if (isSelected) {
                                                selectedTimeSlots - slot
                                            } else {
                                                selectedTimeSlots + slot
                                            }
                                            errorMessage = ""
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = slot,
                                        fontSize = 9.sp,
                                        color = when {
                                            isTaken -> Color.Gray
                                            isSelected -> Color.White
                                            else -> Color(0xFF2E7D32)
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", color = Color.DarkGray)
                    }

                    Button(
                        onClick = {
                            if (selectedFacility == null) {
                                errorMessage = "Please select a facility"
                            } else if (selectedTimeSlots.isEmpty()) {
                                errorMessage = "Please select at least one time slot"
                            } else if (phoneNumber.isEmpty()) {
                                errorMessage = "Please enter contact number"
                            } else if (phoneNumber.length < 11) {
                                errorMessage = "Phone number must be exactly 11 digits"
                            } else {
                                val sortedSlots = selectedTimeSlots.toList().sortedBy { timeToMinutes(it.split(" - ")[0]) }
                                val startT = sortedSlots.first().split(" - ").first()
                                val endT = sortedSlots.last().split(" - ").last()
                                val displayDate = String.format(Locale.US, "%s %d, %d", monthNames[selectedMonth], selectedDate, selectedYear)

                                // 1. Add to ViewModel (PENDING state) for Admin Approval
                                viewModel.addReservation(
                                    title = selectedFacility!!.name,
                                    date = displayDate,
                                    formattedDate = formattedDate, // Pass formatted date for sync
                                    time = "$startT - $endT",
                                    user = user.name,
                                    phone = phoneNumber,
                                    note = purpose
                                )

                                // Note: We do NOT add to calendarEvents here.
                                // It will only be added once the admin "Accepts" it in the Approval Screen.
                                
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm", color = Color.White)
                    }
                }
            }
        }
    }
}
