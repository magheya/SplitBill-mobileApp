package com.example.myapplication.util

import android.content.Context
import android.os.Environment
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Balance
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter {
    fun exportGroupDetails(
        context: Context,
        group: Group,
        balances: Map<String, Balance>,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Group_${group.name}_$timestamp.pdf"
            val filePath = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )

            PdfWriter(filePath).use { writer ->
                val pdfDoc = PdfDocument(writer)
                Document(pdfDoc).use { document ->
                    // Add Title
                    document.add(
                        Paragraph(group.name)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(24f)
                    )

                    // Add Total Amount
                    document.add(
                        Paragraph("Total Amount: $${String.format("%.2f", group.totalAmount)}")
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontSize(16f)
                    )

                    // Add Member Balances
                    document.add(
                        Paragraph("Member Balances")
                            .setFontSize(18f)
                            .setMarginTop(20f)
                    )

                    // Create table for balances
                    val balanceTable = Table(floatArrayOf(3f, 2f, 2f, 2f))
                        .useAllAvailableWidth()

                    // Add header row
                    balanceTable.addHeaderCell("Member")
                    balanceTable.addHeaderCell("Paid")
                    balanceTable.addHeaderCell("Owes")
                    balanceTable.addHeaderCell("Net Balance")

                    // Add member rows with fixed owes logic
                    group.members.forEach { (memberId, member) ->
                        val memberBalance = balances[memberId]
                        val netBalance = memberBalance?.netBalance ?: 0.0

                        balanceTable.addCell(member.name)
                        balanceTable.addCell("$${String.format("%.2f", memberBalance?.totalPaid ?: 0.0)}")
                        balanceTable.addCell(
                            if (netBalance < 0) "$${String.format("%.2f", -netBalance)}" else "$0.00"
                        )
                        balanceTable.addCell("$${String.format("%.2f", netBalance)}")
                    }

                    document.add(balanceTable)

                    // Add Expenses
                    document.add(
                        Paragraph("Expenses")
                            .setFontSize(18f)
                            .setMarginTop(20f)
                    )

                    val expenseTable = Table(floatArrayOf(4f, 2f, 3f, 2f))
                        .useAllAvailableWidth()

                    expenseTable.addHeaderCell("Description")
                    expenseTable.addHeaderCell("Amount")
                    expenseTable.addHeaderCell("Paid By")
                    expenseTable.addHeaderCell("Category")

                    group.expenses.values.forEach { expense ->
                        expenseTable.addCell(expense.description)
                        expenseTable.addCell("$${String.format("%.2f", expense.amount)}")
                        expenseTable.addCell(group.members[expense.paidBy]?.name ?: expense.paidBy)
                        expenseTable.addCell(expense.category.name)
                    }

                    document.add(expenseTable)
                }
            }

            onComplete(filePath.absolutePath)
        } catch (e: Exception) {
            onError(e)
        }
    }
}