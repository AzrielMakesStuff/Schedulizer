package com.ateam.schedulizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember

class WidgetTaskActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ACTION = "widget_action"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TYPE = "task_type"

        const val ACTION_CREATE = "create"
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
    }

    // Handle new intents when activity is reused (singleInstance)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val action = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_CREATE
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val taskType = intent.getStringExtra(EXTRA_TASK_TYPE)

        setContent {
            AppTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (action) {
                        ACTION_CREATE -> TaskDialog(onDismiss = { finishAndRemoveTask() })

                        ACTION_EDIT -> {
                            val task = remember(taskId, taskType) {
                                databaseInstance.tasks.value.findByIdAndType(taskId, taskType ?: "")
                            }
                            if (task != null) {
                                TaskDialog(task = task, onDismiss = { finishAndRemoveTask() })
                            } else {
                                finishAndRemoveTask()
                            }
                        }

                        ACTION_DELETE -> {
                            val task = remember(taskId, taskType) {
                                databaseInstance.tasks.value.findByIdAndType(taskId, taskType ?: "")
                            }
                            if (task == null) {
                                finishAndRemoveTask()
                                return@Surface
                            }
                            DeleteDialog(task) {
                                finishAndRemoveTask()
                            }
                        }
                    }
                }
            }
        }
    }
}