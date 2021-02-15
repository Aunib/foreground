package me.bgregos.foreground.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.bgregos.foreground.tasklist.LocalTasksRepository
import me.bgregos.foreground.util.NotificationService
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            NotificationService.ACTION -> {
                Log.i("notif", "Received notification intent")
                val uuid: String? = intent.extras?.get("uuid") as String?
                if (uuid == null) {
                    Log.e("notif", "Failed to display notification for task with null uuid")
                    return
                }
                if (context == null){
                    Log.e("notif", "Couldn't show notification - null context")
                }else{
                    LocalTasksRepository.load(context, true)
                    val task = LocalTasksRepository.getTaskByUUID(UUID.fromString(uuid))
                    if (task != null) {
                        NotificationService.showDueNotification(task, context)
                    } else {
                        Log.e("notif", "Couldn't show notification - null task")
                    }
                }
            }
        }

    }

}