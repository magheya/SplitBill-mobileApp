package com.example.myapplication.ui.scanner

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication.data.model.Expense
import com.example.myapplication.viewmodel.ReceiptScannerViewModel
import com.example.myapplication.viewmodel.ScanState


@Composable
fun ReceiptReviewCard(
    expense: Expense,
    onConfirm: () -> Unit,
    onEdit: (Expense) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Review Receipt",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display receipt information
            Text("Amount: ${expense.amount}", style = MaterialTheme.typography.bodyLarge)
            Text("Category: ${expense.category}", style = MaterialTheme.typography.bodyLarge)
            Text("Description: ${expense.description}", style = MaterialTheme.typography.bodyLarge)
            Text("Date: ${expense.createdAt}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onNavigateBack: () -> Unit,
    onExpenseCreated: (Expense) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ReceiptScannerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReceiptScannerViewModel(context.applicationContext as Application) as T
            }
        }
    )

    // Create a directory for receipts if it doesn't exist
    val receiptDir = File(context.filesDir, "Receipts")
    if (!receiptDir.exists()) {
        receiptDir.mkdirs()
    }

    val photoUri = remember { mutableStateOf<Uri?>(null) }

    // Define camera launcher first
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri.value != null) {
            Log.d("ReceiptScanner", "Photo captured successfully at: ${photoUri.value}")
            try {
                viewModel.processImage(photoUri.value!!)
            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Error processing image: ${e.message}", e)
                Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("ReceiptScanner", "Failed to capture photo, success=$success, uri=${photoUri.value}")
        }
    }

    // Check for camera permission
    val hasPermission = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission.value = isGranted
        if (isGranted) {
            // If permission is granted, try to launch the camera immediately
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(receiptDir, "receipt_${timestamp}.jpg")
                photoUri.value = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                cameraLauncher.launch(photoUri.value)
            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Error launching camera after permission granted: ${e.message}", e)
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to scan receipts", Toast.LENGTH_LONG).show()
        }
    }

    // Request permission check on first composition
    LaunchedEffect(Unit) {
        hasPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // UI implementation
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val currentState = viewModel.scanState.collectAsState().value) {
                ScanState.Initial -> {
                    Button(
                        onClick = {
                            try {
                                if (hasPermission.value) {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val file = File(receiptDir, "receipt_${timestamp}.jpg")
                                    photoUri.value = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    Log.d("ReceiptScanner", "Launching camera with URI: ${photoUri.value}")
                                    cameraLauncher.launch(photoUri.value)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } catch (e: Exception) {
                                Log.e("ReceiptScanner", "Error launching camera: ${e.message}", e)
                                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Receipt Photo")
                    }
                }
                ScanState.Processing -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Processing receipt...")
                }
                is ScanState.Success -> {
                    val expense = currentState.expense
                    ReceiptReviewCard(
                        expense = expense,
                        onConfirm = {
                            onExpenseCreated(expense)
                            onNavigateBack()
                        },
                        onEdit = { editedExpense: Expense ->
                            viewModel.updateExpense(editedExpense)
                        }
                    )
                }
                is ScanState.Error -> {
                    Text("Error: ${currentState.message}", color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}