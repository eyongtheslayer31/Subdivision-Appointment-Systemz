package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * UserRepository handles data operations with SharedPreferences persistence.
 */
object UserRepository {
    private const val PREFS_NAME = "user_prefs"
    
    // Initial static list of users
    private val initialUsers = listOf(
        User("admin", "admin123", "System Administrator", "admin@subdivision.com", "Admin", "0000000000", "Admin Office"),
        User("test", "123", "Test User", "test@example.com", "Homeowner", "09123456789", "Blk 1 Lot 1"),
        User("eyong", "123", "Eyong Gabriel", "eyong@example.com", "Homeowner", "09223334455", "Blk 5 Lot 12 Phase 2"),
        User("clifford08", "123", "Clifford", "clifford@example.com", "Homeowner", "09123456789", "Blk 10 Lot 5")
    )

    // Reactive list for the current session
    val users = mutableStateListOf<User>().apply { addAll(initialUsers) }

    /**
     * Loads saved data from memory and applies them to the current session.
     */
    fun loadPersistedData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        users.forEachIndexed { index, user ->
            val savedPassword = prefs.getString("pwd_${user.username}", null)
            val savedProfilePic = prefs.getString("pic_${user.username}", null)
            
            var updatedUser = user
            if (savedPassword != null) {
                updatedUser = updatedUser.copy(password = savedPassword)
            }
            if (savedProfilePic != null) {
                updatedUser = updatedUser.copy(profilePictureUri = savedProfilePic)
            }
            users[index] = updatedUser
        }
    }

    /**
     * Authenticates the user.
     */
    fun authenticate(context: Context, username: String, password: String): User? {
        loadPersistedData(context) // Ensure we have latest saved data
        return users.find { user -> user.username == username && user.password == password }
    }

    /**
     * Updates and permanently saves the password.
     */
    fun updatePassword(context: Context, username: String, newPassword: String): Boolean {
        val index = users.indexOfFirst { it.username == username }
        if (index != -1) {
            users[index] = users[index].copy(password = newPassword)
            
            // Permanently save to phone memory
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("pwd_$username", newPassword).apply()
            
            Log.d("UserRepository", "Password permanently saved for: $username")
            return true
        }
        return false
    }

    /**
     * Updates and permanently saves the profile picture URI.
     */
    fun updateProfilePicture(context: Context, username: String, uri: String?): Boolean {
        val index = users.indexOfFirst { it.username == username }
        if (index != -1) {
            users[index] = users[index].copy(profilePictureUri = uri)
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("pic_$username", uri).apply()
            
            Log.d("UserRepository", "Profile picture saved for: $username")
            return true
        }
        return false
    }

    /**
     * Copies the image from the selected URI to the app's internal storage
     * so it persists even after reboots or if the original is moved.
     */
    fun saveProfileImage(context: Context, username: String, uri: Uri): String? {
        return try {
            val fileName = "profile_${username}.jpg"
            val file = File(context.filesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            val localUri = Uri.fromFile(file).toString()
            updateProfilePicture(context, username, localUri)
            localUri
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving profile image", e)
            null
        }
    }

    /**
     * Saves a receipt image to internal storage.
     */
    fun saveReceiptImage(context: Context, uri: Uri): String? {
        return try {
            val fileName = "receipt_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving receipt image", e)
            null
        }
    }
}
