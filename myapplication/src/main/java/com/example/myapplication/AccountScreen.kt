package com.example.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun Account(user: User) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Local state for the newly selected image
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // The Save button only appears if a new image is selected
    val hasPictureChange = selectedImageUri != null

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightLavender)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            
            if (hasPictureChange) {
                Button(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            UserRepository.saveProfileImage(context, user.username, uri)
                            selectedImageUri = null // Button disappears immediately
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Save Photo", fontSize = 12.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Profile Picture Section
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(DarkBlueGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (user.profilePictureUri != null) {
                            AsyncImage(
                                model = user.profilePictureUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(50.dp))
                        }
                    }
                    
                    // Camera Button overlay
                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, "Change Picture", tint = DeepNavy, modifier = Modifier.size(18.dp))
                    }
                }

                Text("Login Credentials", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(Modifier.height(16.dp))

                AccountReadOnlyField(label = "Username", value = user.username)
                Spacer(Modifier.height(12.dp))
                AccountReadOnlyField(label = "Email Address", value = user.email)
                Spacer(Modifier.height(12.dp))
                
                // Password row
                Column {
                    Text("Password", fontSize = 12.sp, color = MediumGray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("••••••••", modifier = Modifier.weight(1f), color = DeepNavy)
                        TextButton(onClick = { showPasswordDialog = true }) {
                            Text("Change", color = DarkBlueGray, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(24.dp))

                Text("Personal Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(Modifier.height(16.dp))

                AccountReadOnlyField(label = "Full Name", value = user.name)
                Spacer(Modifier.height(12.dp))
                AccountReadOnlyField(label = "Contact Number", value = user.contactNum)
                Spacer(Modifier.height(12.dp))
                AccountReadOnlyField(label = "Address", value = user.address)
                
                Spacer(Modifier.height(20.dp))
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(user = user, onDismiss = { showPasswordDialog = false })
    }
}

@Composable
fun AccountReadOnlyField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = MediumGray, modifier = Modifier.padding(bottom = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F5F5),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = MediumGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(user: User, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    if (successMessage.isNotEmpty()) {
        LaunchedEffect(Unit) {
            delay(1500)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Change Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (successMessage.isNotEmpty()) {
                    Text(successMessage, color = Color(0xFF2ECC71), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("Cancel", color = Color.DarkGray)
                    }
                    Button(
                        onClick = {
                            when {
                                currentPassword != user.password -> errorMessage = "Incorrect current password"
                                newPassword.isEmpty() -> errorMessage = "New password cannot be empty"
                                newPassword == user.password -> errorMessage = "New password cannot be the same as current"
                                user.passwordHistory.contains(newPassword) -> errorMessage = "New password cannot be one of your last 3 passwords"
                                newPassword != confirmPassword -> errorMessage = "Passwords do not match"
                                else -> {
                                    UserRepository.updatePassword(context, user.username, newPassword)
                                    errorMessage = ""
                                    successMessage = "Password updated successfully!"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray)
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
