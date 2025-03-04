package com.example.myapplication.ui.dashboard

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.data.model.ExpenseCategory
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

@Composable
fun CategoryPieChart(
    categoryExpenses: Map<ExpenseCategory, Double>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val colors = listOf(
        Color.rgb(33, 150, 243),  // Blue
        Color.rgb(76, 175, 80),   // Green
        Color.rgb(255, 193, 7),   // Yellow
        Color.rgb(233, 30, 99),   // Pink
        Color.rgb(156, 39, 176),  // Purple
        Color.rgb(255, 87, 34)    // Deep Orange
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) // Increased height
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Expenses by Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // Added vertical padding
            ) {
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            isDrawHoleEnabled = true
                            setHoleColor(Color.TRANSPARENT)
                            legend.isEnabled = false
                            setEntryLabelColor(Color.WHITE)
                            setEntryLabelTextSize(14f) // Increased text size
                            setUsePercentValues(true) // Show percentage values
                            holeRadius = 45f // Adjusted hole size
                            transparentCircleRadius = 50f
                            setExtraOffsets(8f, 8f, 8f, 8f) // Added padding
                            minimumWidth = 300
                            minimumHeight = 300
                        }
                    },
                    update = { chart ->
                        val entries = categoryExpenses.map { (category, amount) ->
                            PieEntry(amount.toFloat(), category.name)
                        }

                        val dataSet = PieDataSet(entries, "").apply {
                            setColors(colors)
                            valueTextSize = 14f // Increased value text size
                            valueTextColor = Color.WHITE
                            valueFormatter = PercentFormatter(chart)
                            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                        }

                        val pieData = PieData(dataSet)
                        chart.data = pieData
                        chart.invalidate()

                        // Add animations
                        chart.animateY(1400)
                    },
                    modifier = Modifier.fillMaxSize() // Fill the box
                )
            }

            // Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                categoryExpenses.entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = androidx.compose.ui.graphics.Color(colors[index % colors.size]),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${entry.key.name}: ${"%.2f".format(entry.value)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}