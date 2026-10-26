package dev.aliziwa.timesheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aliziwa.timesheet.data.local.ShiftEntity
import dev.aliziwa.timesheet.util.Utils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesheetScreen() {
    val vm: TimesheetViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    var workOrder by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Utils.today()) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("16:30") }
    var breakMin by remember { mutableStateOf("30") }
    var note by remember { mutableStateOf("") }
    var billable by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timesheet") },
                actions = {
                    TextButton(onClick = { vm.submitAll() }) { Text("Submit all") }
                    TextButton(onClick = { showForm = !showForm }) { Text(if (showForm) "Close" else "Add") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Week of " + Utils.displayDate(uiState.selectedWeek), style = MaterialTheme.typography.titleMedium)
                Text(
                    "Worked " + Utils.fmtDuration(uiState.workedMinutes) +
                        ", overtime " + Utils.fmtDuration(uiState.overtimeMinutes) +
                        ", pay " + Utils.fmtMoney(uiState.totalPay),
                    style = MaterialTheme.typography.bodyMedium,
                )
                uiState.message?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { vm.clearMessage() }) { Text("Dismiss") }
                }
            }
            HorizontalDivider()

            if (showForm) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(value = workOrder, onValueChange = { workOrder = it }, label = { Text("Work order") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                    Row {
                        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = breakMin, onValueChange = { breakMin = it }, label = { Text("Break") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = billable, onCheckedChange = { billable = it })
                        Text("Billable")
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = {
                            val sp = start.split(":")
                            val ep = end.split(":")
                            if (sp.size == 2 && ep.size == 2) {
                                vm.createShift(
                                    workOrder,
                                    date,
                                    sp[0].toIntOrNull() ?: 0,
                                    sp[1].toIntOrNull() ?: 0,
                                    ep[0].toIntOrNull() ?: 0,
                                    ep[1].toIntOrNull() ?: 0,
                                    breakMin.toIntOrNull() ?: 0,
                                    if (note.isBlank()) null else note,
                                    billable,
                                    null,
                                )
                                showForm = false
                                workOrder = ""
                                note = ""
                            }
                        }) { Text("Save") }
                    }
                }
                HorizontalDivider()
            }

            Box(Modifier.fillMaxSize()) {
                if (uiState.loading && uiState.shifts.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.shifts) { shift ->
                        ShiftRow(shift, vm)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftRow(shift: ShiftEntity, vm: TimesheetViewModel) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val display = SimpleDateFormat("EEEE, d MMMM", Locale.US)
    val dateText = try {
        display.format(fmt.parse(shift.date)!!)
    } catch (e: Exception) {
        shift.date
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(shift.workOrderId + "  " + Utils.statusLabel(shift.status), style = MaterialTheme.typography.titleMedium)
        Text(dateText, style = MaterialTheme.typography.labelSmall)
        Text(vm.formatRow(shift), style = MaterialTheme.typography.bodyMedium)
        Text(Utils.fmtMoney(Utils.calcPay(shift, Utils.rate())), style = MaterialTheme.typography.bodyMedium)
        Row {
            if (!Utils.isSubmitted(shift)) {
                Button(onClick = { vm.submit(shift) }) { Text("Submit") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { vm.delete(shift) }) { Text("Delete") }
            } else {
                Text("Server id: " + (shift.serverId ?: "?"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
