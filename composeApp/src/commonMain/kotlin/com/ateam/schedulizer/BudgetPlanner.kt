package com.ateam.schedulizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class Expense() {
    var name = "Expense Name"
    var frequency = Frequency.Weekly
    var cost = "0"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Build() {
        var nameT by remember { mutableStateOf("0") }
        var costT by remember { mutableStateOf("0") }
        var expanded by remember { mutableStateOf(false) }
        Row (Modifier.fillMaxWidth()) {
            OutlinedTextField(modifier = Modifier.weight(1.2f), value = nameT, onValueChange = {nameT = it; name = nameT}, label = { Text("Name")})
            ExposedDropdownMenuBox(modifier = Modifier.weight(1f), expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = frequency.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cost") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                
                
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                    Frequency.entries.forEach { payFre ->
                        DropdownMenuItem(
                            text = { Text(payFre.toString()) },
                            onClick = {  expanded = false; frequency = payFre }
                        )
                    }

                }
            }
            OutlinedTextField(modifier = Modifier.weight(0.4f), value = "$$costT", onValueChange = { newVal -> costT = newVal.filter { it.isDigit() }; cost = costT}, label = { Text("Cost")})
        }
    }
}

enum class Frequency {
    Weekly, Fortnightly,Monthly
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun BudgetPlanner() {
    var paidFrequency by remember { mutableStateOf(Frequency.Weekly) }
    var expanded by remember { mutableStateOf(false) }

    val expenses = remember { mutableStateListOf(Expense()) }
    var calculated by remember { mutableStateOf(false) }
    Column (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        var amountEarned by remember { mutableStateOf("0") }
     if (!calculated) {

         ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth(0.8f) ,expanded = expanded, onExpandedChange = { expanded = it }) {
             OutlinedTextField(
                 value = paidFrequency.toString(),
                 onValueChange = {},
                 readOnly = true,
                 label = { Text("How often are you paid?") },
                 trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                 modifier = Modifier
                     .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
             )
             ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                 Frequency.entries.forEach { payFre ->
                     DropdownMenuItem(
                         text = { Text(payFre.toString()) },
                         onClick = {  expanded = false; paidFrequency = payFre }
                     )
                 }

             }
         }
         Text("Expenses:")
         LazyColumn (modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
             items(expenses.size) { index ->
                 expenses[index].Build()
             }
             item {
                 OutlinedButton(onClick = {
                     expenses.add(Expense())
                 }) {
                     Text("Add Expense")
                 }
             }

             item {
                 OutlinedButton(onClick = {
                     calculated = true
                 }) {
                     Text("Calculate")
                 }
             }
         }
     }
      else {
          var totalExpenses = 0f

          LazyColumn (horizontalAlignment = Alignment.CenterHorizontally){
              items(expenses.size) { index ->
                  val expense = expenses[index]
                  val totalEarned = amountEarned.toFloat()
                  var cost = "0";
                  var timesBy = when (paidFrequency) {
                      Frequency.Weekly -> 1f
                      Frequency.Fortnightly -> 2f
                      Frequency.Monthly -> 4f
                  }
                  cost = when (expense.frequency) {

                      Frequency.Weekly -> (expense.cost.toFloat() * timesBy).toString()
                      Frequency.Fortnightly -> (expense.cost.toFloat() / 2f * timesBy).toString()
                      Frequency.Monthly -> (expense.cost.toFloat() / 4f * timesBy).toString()
                  }
                  totalExpenses += cost.toFloat()
                  Row(Modifier.fillMaxWidth()) {
                      OutlinedCard(modifier = Modifier.weight(1.2f)) {

                          Box(
                              modifier = Modifier.fillMaxSize(),
                              contentAlignment = Alignment.Center
                          ) {
                              Text(expense.name)
                          }
                      }


                          OutlinedCard(modifier = Modifier.weight(0.8f)) {
                              Box(
                                  modifier = Modifier.fillMaxSize(),
                                  contentAlignment = Alignment.Center
                              ) {
                                  Text("$$cost")
                              }
                          }

                  }
              }
              item {
                  Text("Total Cost: $$totalExpenses")
              }
          }
      }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCalculator() {
    var calculated by remember {  mutableStateOf(false)}
    val rows = remember {
        mutableStateListOf(
            InputRow("Enter Wage",false, InputType.int),
            InputRow("Hours Worked", false,InputType.int),
            InputRow("Kiwisaver", true,InputType.percent),
            InputRow("Student Loan", true,InputType.int,InputType.percent),
            InputRow("Holiday Pay", true, InputType.percent),
            InputRow("Overtime", true,InputType.int,InputType.int),
        )
    }
    if (!calculated) {
        Column  (Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
            rows.forEach { inputRow ->
                inputRow.BuildRow()
            }
            var expanded by remember { mutableStateOf(false) }
            var selectedVal by remember { mutableStateOf(WeekOptions.Weekly) }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedVal.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Display pay for this..") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                    WeekOptions.entries.forEach { weekOptions ->
                        DropdownMenuItem(
                            text = { Text(weekOptions.toString()) },
                            onClick = {  expanded = false; selectedVal = weekOptions }
                        )
                    }

                }
            }

            OutlinedButton(modifier = Modifier.fillMaxWidth(0.8f), onClick = {
                calculated = true
            }) {
                Text("Calculate")
            }
        }
        }

    if (calculated) {
        val totalPay = rows[0].getResult()[0].toFloat() * rows[1].getResult()[0].toFloat() * (1f + (rows[4].getResult()[0].toFloat()/100f))
        val studentLoan = 0f;
        DisplayTaxResult(totalPay,studentLoan,rows[1].getResult()[0].toInt(),rows[2].getResult()[0].toFloat())
    }





}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayTaxResult(totalPay : Float, studentLoan : Float, hours: Int, kiwisaverPerc: Float) {
    val kiwiSaverCost = totalPay * (kiwisaverPerc/100f)
    val acc = totalPay * 1.75/100
    val takeHomeAmount = totalPay - kiwiSaverCost
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(modifier = Modifier.fillMaxWidth().fillMaxHeight(),colors = CardDefaults.cardColors()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Hours: $hours", textAlign = TextAlign.Center)
                    }                }

            }
            Box(modifier = Modifier.weight(0.2f))
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(modifier = Modifier.fillMaxWidth().fillMaxHeight(),colors = CardDefaults.cardColors()) {
Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(text = "Weekly ", textAlign = TextAlign.Center)
}
                }
            }
        }

        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Gross Pay:", textAlign = TextAlign.Center)
                }
            }
                OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "$$totalPay", textAlign = TextAlign.Center)
                    }                }
        }
        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight().fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "PAYE:", textAlign = TextAlign.Center)
                }
            }
                OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "", textAlign = TextAlign.Center)
                    }                }
        }

//        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
//            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text(text = "ACC:", textAlign = TextAlign.Center)
//                }
//            }
//            OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text(text = "$$acc", textAlign = TextAlign.Center)
//                }                }
//        }

        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Kiwisaver:", textAlign = TextAlign.Center)
                }
            }
            OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "$$kiwiSaverCost", textAlign = TextAlign.Center)
                }                }
        }

        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Student Loan:", textAlign = TextAlign.Center)
                }
            }
            OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "", textAlign = TextAlign.Center)
                }                }
        }

        Row (modifier = Modifier.fillMaxWidth().weight(1f)) {
            OutlinedCard(modifier = Modifier.weight(0.65f).fillMaxHeight(),colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Take Home Pay:", textAlign = TextAlign.Center)
                }
            }
            OutlinedCard(modifier = Modifier.weight(0.35f).fillMaxHeight(), colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "$$takeHomeAmount", textAlign = TextAlign.Center)
                }                }
        }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick ={}, colors = ButtonDefaults.buttonColors()) {
            Text("Done")
        }
    }
}

enum class WeekOptions {
Weekly
}