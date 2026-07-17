package com.molei.fuelcost.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.molei.fuelcost.ui.display2dp
import com.molei.fuelcost.ui.formatMoney
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onTripClick: (Long) -> Unit,
    vm: TripHistoryViewModel = viewModel(factory = TripHistoryViewModel.Factory),
) {
    val rows by vm.rows.collectAsState()
    val monthTotal by vm.monthTotal.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Trip History") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("This month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(monthTotal.currencySymbol, monthTotal.cost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${monthTotal.tripCount} trip${if (monthTotal.tripCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (rows.isEmpty()) {
                Text("No trips yet. Save one from the New Trip tab.", Modifier.padding(24.dp))
            } else {
                LazyColumn {
                    items(rows, key = { it.trip.id }) { row ->
                        val trip = row.trip
                        val route =
                            if (!trip.startLocation.isNullOrBlank() && !trip.endLocation.isNullOrBlank())
                                "${trip.startLocation} → ${trip.endLocation}"
                            else "Manual entry"
                        val date = Instant.ofEpochMilli(trip.dateEpochMillis)
                            .atZone(ZoneId.systemDefault()).format(dateFormat)
                        val symbol = row.vehicle?.currencySymbol ?: "$"
                        ListItem(
                            headlineContent = { Text(route) },
                            supportingContent = {
                                Text("$date · ${trip.distanceKm.display2dp()} km · ${trip.fuelUsedLitres.display2dp()} L")
                            },
                            trailingContent = {
                                Text(
                                    formatMoney(symbol, trip.cost),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            modifier = Modifier.clickable { onTripClick(trip.id) },
                        )
                    }
                }
            }
        }
    }
}
