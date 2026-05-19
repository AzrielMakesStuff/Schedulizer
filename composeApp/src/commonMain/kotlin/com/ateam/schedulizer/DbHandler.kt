package com.ateam.schedulizer

import com.ateam.schedulizer.dao.TaskDao
import com.ateam.schedulizer.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

object DbHandler {
    val dao : TaskDao = getRoomDatabase(getDatabaseBuilder()).getDao();
    suspend fun storeTasks(vararg tasks : Task) {
        tasks.forEach {
            val entity : TaskEntity = encryptTask(it)
            dao.storeTasks(entity);
        }
    }

    private suspend fun encryptTask(task: Task): TaskEntity = TaskEntity(
        title = Encryption.encrypt(task.title),
        type = Encryption.encrypt(task.type.toString()),
        description = Encryption.encrypt(task.description),
        dateOne = Encryption.encrypt(task.dateOne.toString()),
        dateTwo = Encryption.encrypt(task.dateTwo.toString()),
    )

    suspend fun deleteTasks(vararg tasks : Task) {
        tasks.forEach {
                dao.delete(it.id)
        }
    }

    suspend fun getAllTasks() : MutableList<Task> {
        val listT : List<TaskEntity> = dao.getAll();
        val returnList = mutableListOf<Task>()
        listT.forEach {
            val task : Task = decryptTask(it);
            returnList.add(task)
        }

        return returnList;
    }

    suspend fun getTask(id: Long) : Task {
        val taskEntity : TaskEntity = dao.get(id);
        val task : Task = decryptTask(taskEntity)
        return task;
    }

    private suspend fun decryptTask(taskEntity: TaskEntity): Task = Task(
        taskEntity.id, TaskType.valueOf(Encryption.decrypt(taskEntity.type)),
        Encryption.decrypt(taskEntity.title), Encryption.decrypt(taskEntity.description),
        LocalDateTime.parse(Encryption.decrypt(taskEntity.dateOne)), LocalDateTime.parse(
            Encryption.decrypt(taskEntity.dateTwo)
        )
    )

}