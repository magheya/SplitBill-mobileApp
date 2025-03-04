package com.example.myapplication.ui.dashboard

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun ExpenseDebtChart(
    totalExpenses: Double,
    totalDebts: Double,
    modifier: Modifier = Modifier
) {
    // Capture colors outside AndroidView
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Expenses vs Debts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawBarShadow(false)
                            setDrawValueAboveBar(true)
                            setPinchZoom(false)
                            setScaleEnabled(false)

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                valueFormatter = IndexAxisValueFormatter(listOf("Total Expenses", "Total Debts"))
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                spaceTop = 35f
                                axisMinimum = 0f
                            }

                            axisRight.isEnabled = false
                            legend.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val entries = listOf(
                            BarEntry(0f, totalExpenses.toFloat()),
                            BarEntry(1f, totalDebts.toFloat())
                        )

                        val dataSet = BarDataSet(entries, "").apply {
                            setColors(primaryColor, secondaryColor) // Use captured colors
                            valueTextSize = 12f
                            valueTextColor = Color.BLACK
                        }

                        val barData = BarData(dataSet).apply {
                            barWidth = 0.5f
                        }

                        chart.data = barData
                        chart.invalidate()
                        chart.animateY(1000)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}