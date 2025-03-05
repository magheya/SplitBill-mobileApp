package com.example.myapplication.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Expense
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.myapplication.data.model.ExpenseCategory
import android.app.Application
import androidx.lifecycle.AndroidViewModel

sealed class ScanState {
    object Initial : ScanState()
    object Processing : ScanState()
    data class Success(val expense: Expense) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ReceiptScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Initial)
    val scanState: StateFlow<ScanState> = _scanState

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(imageUri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanState.Processing

            try {
                val image = InputImage.fromFilePath(getApplication<Application>(), imageUri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        val expense = parseReceiptText(text)
                        _scanState.value = ScanState.Success(expense)
                    }
                    .addOnFailureListener { e ->
                        _scanState.value = ScanState.Error(e.message ?: "Unknown error")
                    }
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateExpense(expense: Expense) {
        _scanState.value = ScanState.Success(expense)
    }

    fun resetState() {
        _scanState.value = ScanState.Initial
    }

    private fun parseReceiptText(text: String): Expense {
        // Look for store name (usually at the top)
        val lines = text.split("\n")
        val storeName = lines.firstOrNull()?.trim() ?: "Unknown Store"

        // Look for total amount (using regex to find patterns like "Total: $XX.XX")
        val totalPattern = Pattern.compile("(?i:total)[^\\d]*(\\d+[.,]\\d{2})")
        val totalMatcher = totalPattern.matcher(text)

        val amount = if (totalMatcher.find()) {
            totalMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        } else {
            // Fallback: look for the highest number which might be the total
            val amountPattern = Pattern.compile("(\\d+[.,]\\d{2})")
            val amountMatcher = amountPattern.matcher(text)
            var highestAmount = 0.0

            while (amountMatcher.find()) {
                val foundAmount = amountMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                if (foundAmount > highestAmount) {
                    highestAmount = foundAmount
                }
            }
            highestAmount
        }

        // Look for date
        val datePattern = Pattern.compile("(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})")
        val dateMatcher = datePattern.matcher(text)
        val date = if (dateMatcher.find()) {
            dateMatcher.group(1) ?: ""
        } else {
            ""
        }

        return Expense(
            id = UUID.randomUUID().toString(),
            description = "Receipt from $storeName",
            amount = amount,
            category = ExpenseCategory.OTHER,
            paidBy = "", // This will need to be set later
            createdAt = System.currentTimeMillis()
        )
    }
}