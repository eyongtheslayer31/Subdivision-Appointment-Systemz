package com.example.myapplication

import android.util.Log

/**
 * UserRepository handles data operations.
 */
object UserRepository {
    // Current local data (Mock Database)
    private val users = mutableListOf(
        User("admin", "admin123", "System Administrator", "admin@subdivision.com", "Admin", "0000000000", "Admin Office"),
        User("test", "123", "Test User", "test@example.com", "Homeowner", "09123456789", "Blk 1 Lot 1")
    )

    /**
     * Authenticates the user.
     */
    fun authenticate(username: String, password: String): User? {
        Log.d("UserRepository", "Attempting login for: $username")
        return users.find { it.username == username && it.password == password }
    }
}
