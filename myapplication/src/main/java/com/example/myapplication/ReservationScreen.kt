package com.example.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    // Lock all calendar logic to Philippine Time
    val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
    val phCalendar = Calendar.getInstance(phTimeZone)

    var viewMode by remember { mutableStateOf("month") }
    var selectedDate by remember { mutableIntStateOf(phCalendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(phCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(phCalendar.get(Calendar.YEAR)) }
    var showEventDetails by remember { mutableStateOf(false) }
    var selectedEvents by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    
    // Facility Filter State
    var selectedFacilityFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Basketball Court", "Tennis Court", "Chapel", "Multipurpose Hall")

    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    // Helper function to get events for a specific date
    fun getEventsForDate(year: Int, month: Int, day: Int): List<CalendarEvent> {
        val dateString = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        // Filter events based on status and user authorization
        var rawEvents = calendarEvents.filter { it.date == dateString }
        
        // Apply Facility Filter
        if (selectedFacilityFilter != "All") {
            rawEvents = rawEvents.filter { it.venue == selectedFacilityFilter }
        }
        
        return rawEvents.filter { event ->
            event.status == "ACTIVE" || 
            (event.status == "PENDING" && (user.role == "Admin" || event.reservedBy == user.name))
        }.sortedWith(compareByDescending<CalendarEvent> { it.status == "PENDING" }.thenBy { timeToMinutes(it.startTime) })
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
        val cal = Calendar.getInstance(phTimeZone)
        cal.set(year, month, 1)
        return cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    }

    // Helper to check if a date is in the past
    fun isDateInPast(year: Int, month: Int, day: Int): Boolean {
        val today = Calendar.getInstance(phTimeZone)
        val compareDate = Calendar.getInstance(phTimeZone)
        // Check if the entire day is in the past
        compareDate.set(year, month, day, 23, 59, 59)
        return compareDate.before(today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightLavender)
    ) {
        // Top bar with year selector and filter
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

            // Facility Filter Dropdown
            var filterExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { filterExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (selectedFacilityFilter != "All") DarkBlueGray else Color.White,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Facility",
                        tint = if (selectedFacilityFilter != "All") Color.White else DeepNavy
                    )
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    filterOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    option, 
                                    color = if (selectedFacilityFilter == option) DarkBlueGray else DeepNavy,
                                    fontWeight = if (selectedFacilityFilter == option) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                selectedFacilityFilter = option
                                filterExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (viewMode == "month") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightLavender)
            ) {
                // Month name and active filter indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = monthNames[selectedMonth],
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    
                    if (selectedFacilityFilter != "All") {
                        Surface(
                            color = DarkBlueGray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = selectedFacilityFilter,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = DarkBlueGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                val eventsForDay = if (isValidDay) getEventsForDate(selectedYear, selectedMonth, dayNumber) else emptyList()
                                val hasEvents = eventsForDay.isNotEmpty()

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
                                            if (eventsForDay.isNotEmpty()) {
                                                selectedEvents = eventsForDay
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
                val isPastDate = isDateInPast(selectedYear, selectedMonth, selectedDate)
                val today = Calendar.getInstance(phTimeZone)
                val isToday = selectedYear == today.get(Calendar.YEAR) && 
                              selectedMonth == today.get(Calendar.MONTH) && 
                              selectedDate == today.get(Calendar.DAY_OF_MONTH)
                
                var anySlotsAvailable = true
                if (isToday) {
                    anySlotsAvailable = timeSlots.any { slot ->
                        val endTimeStr = slot.split(" - ")[1]
                        val endMinutes = timeToMinutes(endTimeStr)
                        val currentMinutes = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE)
                        endMinutes > currentMinutes
                    }
                }

                val canSchedule = !isPastDate && anySlotsAvailable
                
                Button(
                    onClick = { if (canSchedule) showScheduleDialog = true },
                    enabled = canSchedule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!canSchedule) Color.LightGray else DarkBlueGray,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = if (!canSchedule) Color.Gray else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!canSchedule) "No Available Slots" else "Schedule Facility",
                        color = if (!canSchedule) Color.Gray else Color.White,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                
                Surface(
                    color = if (event.status == "PENDING") Color(0xFFF59E0B).copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (event.status == "PENDING") "PENDING" else "APPROVED",
                        color = if (event.status == "PENDING") Color(0xFFF59E0B) else SuccessGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DeepNavy.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reserved by: ${event.reserverRole}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            
            if (isAdmin && event.reserverPhone.isNotEmpty()) {
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
    val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
    val context = LocalContext.current
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var selectedTimeSlots by remember { mutableStateOf(setOf<String>()) }
    var purpose by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDate)

    fun confirmReservation(proofUri: String? = null) {
        val sortedSlots = selectedTimeSlots.toList().sortedBy { timeToMinutes(it.split(" - ")[0]) }
        val startT = sortedSlots.first().split(" - ").first()
        val endT = sortedSlots.last().split(" - ").last()
        val displayDate = String.format(Locale.US, "%s %d, %d", monthNames[selectedMonth], selectedDate, selectedYear)
        
        // Calculate Cost
        val totalCost = selectedTimeSlots.size * 500.0

        if (user.role == "Admin") {
            // Admin reservation logic: Auto-active
            val newItem = ReservationItem(
                title = selectedFacility!!.name,
                date = displayDate,
                time = "$startT - $endT",
                status = ReservationStatus.ACTIVE,
                reservedBy = user.name,
                contact = phoneNumber,
                purpose = purpose,
                formattedDate = formattedDate,
                paymentProofUri = null,
                cost = totalCost
            )
            viewModel.addReservationDirect(context, newItem)
            onDismiss()
        } else {
            // Homeowner logic
            viewModel.addReservation(
                context = context,
                title = selectedFacility!!.name,
                date = displayDate,
                formattedDate = formattedDate,
                time = "$startT - $endT",
                user = user.name,
                phone = phoneNumber,
                note = purpose,
                paymentProof = proofUri,
                cost = totalCost
            )
            onDismiss()
        }
    }

    if (showPaymentDialog) {
        PaymentConfirmationDialog(
            totalCost = selectedTimeSlots.size * 500.0,
            onConfirm = { proofUri -> confirmReservation(proofUri) },
            onCancel = { showPaymentDialog = false }
        )
    }

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
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 11) {
                            phoneNumber = input
                            // Real-time validation message logic
                            errorMessage = if (input.isNotEmpty() && !input.startsWith("09")) {
                                "Invalid format: Must start with 09"
                            } else ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE8EBFA),
                        unfocusedContainerColor = Color(0xFFE8EBFA),
                        focusedBorderColor = if (errorMessage.isNotEmpty()) Color.Red else Color.Transparent,
                        unfocusedBorderColor = if (errorMessage.isNotEmpty()) Color.Red else Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("09XXXXXXXXX", fontSize = 14.sp) }
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
                                val slotTimes = slot.split(" - ")
                                val sStart = timeToMinutes(slotTimes[0])
                                val sEnd = timeToMinutes(slotTimes[1])

                                val today = Calendar.getInstance(phTimeZone)
                                val isToday = selectedYear == today.get(Calendar.YEAR) && 
                                              selectedMonth == today.get(Calendar.MONTH) && 
                                              selectedDate == today.get(Calendar.DAY_OF_MONTH)
                                
                                val isPastSlot = if (isToday) {
                                    val currentMinutes = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE)
                                    currentMinutes >= sEnd
                                } else false

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
                                                isTaken || isPastSlot -> Color.LightGray.copy(alpha = 0.5f)
                                                isSelected -> Color(0xFF1E88E5)
                                                else -> Color(0xFFE0F2F1)
                                            }
                                        )
                                        .clickable(enabled = !isTaken && !isPastSlot) {
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
                                            isTaken || isPastSlot -> Color.Gray
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Slots: ${selectedTimeSlots.size}", fontSize = 11.sp, color = MediumGray)
                        Text("Total: ₱${String.format("%.2f", selectedTimeSlots.size * 500.0)}", fontWeight = FontWeight.Bold, color = DeepNavy)
                    }

                    Button(
                        onClick = {
                            if (selectedFacility == null) {
                                errorMessage = "Please select a facility"
                            } else if (selectedTimeSlots.isEmpty()) {
                                errorMessage = "Please select at least one time slot"
                            } else if (phoneNumber.isEmpty()) {
                                errorMessage = "Please enter contact number"
                            } else if (!phoneNumber.startsWith("09") || phoneNumber.length < 11) {
                                errorMessage = "Invalid format: Must be 11 digits starting with 09"
                            } else {
                                if (user.role == "Admin") {
                                    confirmReservation()
                                } else {
                                    showPaymentDialog = true
                                }
                            }
                        },
                        modifier = Modifier.height(45.dp),
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

@Composable
fun PaymentConfirmationDialog(
    totalCost: Double,
    onConfirm: (String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF3EFFF), 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Payment Confirmation",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Please settle the payment to proceed:",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "GCash: 0912-345-6789",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount Due:", fontSize = 14.sp, color = MediumGray)
                        Text("₱${String.format("%.2f", totalCost)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Upload Receipt/Proof:",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE8E4F3))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Proof of Payment",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Tap to upload photo",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = DeepNavy, fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                val savedUri = UserRepository.saveReceiptImage(context, uri)
                                onConfirm(savedUri)
                            }
                        },
                        enabled = selectedImageUri != null,
                        modifier = Modifier.weight(2f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD1CBE9), 
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            "Submit Reservation", 
                            color = if (selectedImageUri != null) DeepNavy else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
