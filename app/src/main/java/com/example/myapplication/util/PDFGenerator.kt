package com.example.myapplication.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import com.example.myapplication.data.model.Expense
import java.io.File
import java.io.FileOutputStream

class PDFGenerator {
    fun generateReport(context: Context, expenses: List<Expense>): File {
        val file = File(context.getExternalFilesDir(null), "ExpensesReport.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 12f
        }

        var yPosition = 50f
        expenses.forEach { expense ->
            canvas.drawText(
                "Expense: ${expense.description} - ${expense.amount} DA",
                50f,
                yPosition,
                paint
            )
            yPosition += 20f
        }

        document.finishPage(page)
        document.writeTo(FileOutputStream(file))
        document.close()

        return file
    }
}