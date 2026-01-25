package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReservationRepository {
    private const val PREFS_NAME = "reservation_prefs"
    private const val KEY_RESERVATIONS = "all_reservations"

    fun saveReservations(context: Context, reservations: List<ReservationItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        reservations.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("date", item.date)
                put("time", item.time)
                put("status", item.status.name)
                put("reservedBy", item.reservedBy)
                put("contact", item.contact)
                put("purpose", item.purpose)
                put("formattedDate", item.formattedDate)
                put("paymentProofUri", item.paymentProofUri ?: "")
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString(KEY_RESERVATIONS, jsonArray.toString()).apply()
    }

    fun loadReservations(context: Context): List<ReservationItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RESERVATIONS, null) ?: return emptyList()
        val reservations = mutableListOf<ReservationItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val item = ReservationItem(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    date = jsonObject.getString("date"),
                    time = jsonObject.getString("time"),
                    status = ReservationStatus.valueOf(jsonObject.getString("status")),
                    reservedBy = jsonObject.getString("reservedBy"),
                    contact = jsonObject.getString("contact"),
                    purpose = jsonObject.getString("purpose"),
                    formattedDate = jsonObject.getString("formattedDate"),
                    paymentProofUri = jsonObject.getString("paymentProofUri").takeIf { it.isNotEmpty() }
                )
                reservations.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return reservations
    }
}
