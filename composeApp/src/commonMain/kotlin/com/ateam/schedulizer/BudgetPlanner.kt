package com.ateam.schedulizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.ateam.schedulizer.entities.Expense
import com.ateam.schedulizer.entities.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.round
import kotlin.random.Random

class ExpenseBudget(
    var id: Long? = null,
    var name: String = "Expense Name",
    var frequency: Frequency = Frequency.Weekly,
    var cost: String = "0"
) {
    private val localId: Long = Random.nextLong()
    val key: Any get() = id ?: localId

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Build(onDelete: () -> Unit) {
        var nameT by remember(name) { mutableStateOf(name) }
        var costT by remember(cost) { mutableStateOf(cost) }
        var expanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1.1f),
                    value = nameT,
                    onValueChange = { nameT = it; name = nameT },
                    label = { Text("Name") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = frequency.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Occurrence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Frequency.entries.forEach { payFre ->
                            DropdownMenuItem(
                                text = { Text(payFre.toString()) },
                                onClick = { expanded = false; frequency = payFre }
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(0.6f),
                    value = "$$costT",
                    onValueChange = { newVal ->
                        costT = newVal.filter { it.isDigit() }
                        cost = costT
                    },
                    label = { Text("Cost") }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
            }
        }
    }
}

enum class Frequency {
    Weekly, Fortnightly, Monthly, Yearly
}

@Composable
fun DeleteExpenseDialog(
    expenseName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete expense") },
        text = { Text("Delete \"$expenseName\"? This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
@Composable
@Preview
fun BudgetPlanner() {
    var paidFrequency by remember { mutableStateOf(Frequency.Weekly) }
    var expanded by remember { mutableStateOf(false) }

    val expenses = remember { mutableStateListOf(ExpenseBudget()) }
    var calculated by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseBudget?>(null) }

    expenseToDelete?.let { expense ->
        DeleteExpenseDialog(
            expenseName = expense.name,
            onConfirm = {
                expense.id?.let { removeExpense(it) }
                expenses.remove(expense)
                expenseToDelete = null
            },
            onDismiss = {
                expenseToDelete = null
            }
        )
    }

    LaunchedEffect(Unit) {
        databaseInstance.refresh()
        val dbExpenses = withContext(Dispatchers.IO) {
            databaseInstance.getAllExpenses()
        }
        val settings = databaseInstance.settings.value
        if (settings != null) {
            paidFrequency = settings.paidFrequency
        }
        if (dbExpenses.isNotEmpty()) {
            expenses.clear()
            dbExpenses.forEach { item ->
                expenses.add(
                    ExpenseBudget(
                        id = item.id,
                        name = item.name,
                        frequency = item.frequency,
                        cost = item.cost.toString()
                    )
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BackHandler(calculated, onBack = { calculated = false })

        if (!calculated) {
            ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth(0.5f).padding(bottom = 20.dp),
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = paidFrequency.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("How often are you paid?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Frequency.entries.forEach { payFre ->
                        DropdownMenuItem(
                            text = { Text(payFre.toString()) },
                            onClick = {
                                expanded = false
                                paidFrequency = payFre
                            }
                        )
                    }
                }
            }
            Text("Expenses:")
            LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                items(
                    items = expenses,
                    key = { it.key }
                ) { expense ->
                    expense.Build(
                        onDelete = {
                            expenseToDelete = expense
                        }
                    )
                }
                item {
                    OutlinedButton(onClick = {
                        expenses.add(ExpenseBudget())
                    }) {
                        Text("Add Expense")
                    }
                }

                item {
                    OutlinedButton(onClick = {
                        saveExpenses(expenses, paidFrequency)
                        calculated = true
                    }) {
                        Text("Calculate")
                    }
                }
            }
        } else {
            var totalExpenses = 0f

            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "Disclaimer: some results may have small inaccuracies, always verify the results",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(expenses.size) { index ->
                    val expense = expenses[index]
                    val timesBy = when (paidFrequency) {
                        Frequency.Weekly -> 1f
                        Frequency.Fortnightly -> 2f
                        Frequency.Monthly -> 4.33f
                        Frequency.Yearly -> 52f
                    }
                    val expenseCost = expense.cost.toFloatOrNull() ?: 0f
                    val rawCost = when (expense.frequency) {
                        Frequency.Weekly -> expenseCost * timesBy
                        Frequency.Fortnightly -> (expenseCost / 2f) * timesBy
                        Frequency.Monthly -> (expenseCost / 4.33f) * timesBy
                        Frequency.Yearly -> (expenseCost / 52f) * timesBy
                    }
                    val roundedCost = round(rawCost * 100f) / 100f
                    totalExpenses += roundedCost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expense.name,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "$$roundedCost",
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Total Cost: $$totalExpenses",
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )
                }
            }
        }
    }
}

fun saveExpenses(expenses: List<ExpenseBudget>, paidFrequency: Frequency) {
    val snapshot = expenses.toList()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        snapshot.forEach { item ->
            val expenseEntity = Expense(
                id = item.id ?: 0L,
                name = item.name,
                cost = item.cost.toIntOrNull() ?: 0,
                frequency = item.frequency
            )
            databaseInstance.upsertExpense(expenseEntity)
        }
        val currentSettings = databaseInstance.settings.value ?: Settings()
        databaseInstance.saveSettings(currentSettings.copy(paidFrequency = paidFrequency))
    }
}

fun removeExpense(id: Long) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        databaseInstance.deleteExpense(id)
    }
}