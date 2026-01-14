package com.example.myapplication

// Data classes for the App
data class User(
    val username: String,
    val password: String,
    val name: String,
    val email: String,
    val role: String,
    val contactNum: String,
    val address: String
)

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

data class CalendarEvent(
    val id: Int,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val description: String,
    val reservedBy: String = "",
    val reserverPhone: String = ""
)

data class Facility(
    val id: Int,
    val name: String,
    val icon: String
)
