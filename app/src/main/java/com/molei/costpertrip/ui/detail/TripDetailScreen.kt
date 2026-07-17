package com.molei.costpertrip.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.molei.costpertrip.data.DistanceSource
import com.molei.costpertrip.ui.display2dp
import com.molei.costpertrip.ui.formatMoney
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    vm: TripDetailViewModel = viewModel(factory = TripDetailViewModel.factory(tripId)),
) {
    val detail by vm.detail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val d = detail ?: return@Scaffold
        val trip = d.trip
        val symbol = d.vehicle?.currencySymbol ?: "$"

        // Values as applied at save time, recovered from the stored outputs (the vehicle
        // profile may have been edited since; the stored trip numbers are authoritative).
        val rateUsed = if (trip.distanceKm > 0) trip.fuelUsedLitres * 100 / trip.distanceKm else 0.0
        val pricePerLitre = if (trip.fuelUsedLitres > 0) trip.cost / trip.fuelUsedLitres else 0.0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inputs", style = MaterialTheme.typography.titleMedium)
                    DetailRow("Vehicle", d.vehicle?.name ?: "(deleted vehicle)")
                    DetailRow(
                        "Date",
                        Instant.ofEpochMilli(trip.dateEpochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat),
                    )
                    DetailRow("Mode", trip.mode.name)
                    DetailRow("Distance", "${trip.distanceKm.display2dp()} km")
                    DetailRow(
                        "Distance source",
                        when (trip.distanceSource) {
                            DistanceSource.Manual -> "Manual entry"
                            DistanceSource.DirectionsAPI -> "Google Directions API"
                        },
                    )
                    if (!trip.startLocation.isNullOrBlank()) DetailRow("From", trip.startLocation)
                    if (!trip.endLocation.isNullOrBlank()) DetailRow("To", trip.endLocation)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculation", style = MaterialTheme.typography.titleMedium)
                    FormulaLine(
                        "Price per litre",
                        "${formatMoney(symbol, pricePerLitre)} " +
                            if (d.vehicle?.fuelPriceMode?.name == "PerTank") "(tank price ÷ tank capacity)" else "(entered per litre)",
                    )
                    FormulaLine("Consumption rate", "${rateUsed.display2dp()} L/100km (${trip.mode} mode)")
                    FormulaLine(
                        "Fuel used",
                        "${trip.distanceKm.display2dp()} km × ${rateUsed.display2dp()} ÷ 100 = ${trip.fuelUsedLitres.display2dp()} L",
                    )
                    FormulaLine(
                        "Cost",
                        "${trip.fuelUsedLitres.display2dp()} L × ${formatMoney(symbol, pricePerLitre)} = ${formatMoney(symbol, trip.cost)}",
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatMoney(symbol, trip.cost),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FormulaLine(label: String, formula: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formula, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}
