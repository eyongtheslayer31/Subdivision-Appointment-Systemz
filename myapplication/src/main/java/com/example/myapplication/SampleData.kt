package com.example.myapplication

import androidx.compose.runtime.mutableStateListOf

// Facilities available for booking
val availableFacilities = listOf(
    Facility(1, "Chapel", "⛪"),
    Facility(2, "Basketball Court", "🏀"),
    Facility(3, "Multipurpose Hall", "🏛️"),
    Facility(4, "Tennis Court", "🎾")
)
// paghiwalayin start time and end time
// Time slots for the picker
val timeSlots = listOf(
    "8:00 AM - 9:00 AM", "9:00 AM - 10:00 AM", "10:00 AM - 11:00 AM", "11:00 AM - 12:00 NN",
    "12:00 NN - 1:00 PM", "1:00 PM - 2:00 PM", "2:00 PM - 3:00 PM", "3:00 PM - 4:00 PM",
    "4:00 PM - 5:00 PM", "5:00 PM - 6:00 PM", "6:00 PM - 7:00 PM", "7:00 PM - 8:00 PM",
    "8:00 PM - 9:00 PM", "9:00 PM - 10:00 PM", "10:00 PM - 11:00 PM"
)

// The reactive list that stores reservations during the session
val calendarEvents = mutableStateListOf<CalendarEvent>(
    CalendarEvent(1, "Community Meeting", "2026-01-05", "9:00 AM", "12:00 PM", "Barangay Hall", "Monthly community gathering"),
    CalendarEvent(2, "Health Seminar", "2026-01-05", "2:00 PM", "5:00 PM", "Community Center", "Health and wellness discussion"),
    CalendarEvent(3, "Sports Event", "2026-01-06", "10:00 AM", "3:00 PM", "Barangay Gym", "Basketball tournament")
)

// Static community events for the home screen
val sampleEvents = listOf(
    CommunityEvent(
        id = 1,
        title = "Event",
        timeRange = "Fri, Jan 5 9:00 AM – 5:00 PM",
        description = "Lorem ipsum dolor sit amet.",
        schedules = listOf(EventSchedule("January 5, 2026", "9:00 AM - 12:00 PM", "Barangay Hall"))
    )
)
