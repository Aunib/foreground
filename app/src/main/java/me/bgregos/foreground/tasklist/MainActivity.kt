package me.bgregos.foreground.tasklist

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentTransaction
import me.bgregos.foreground.ForegroundListWidgetProvider
import me.bgregos.foreground.ForegroundListWidgetUpdater
import me.bgregos.foreground.R
import me.bgregos.foreground.getApplicationComponent
import me.bgregos.foreground.util.NotificationRepository
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    private lateinit var fragment: TaskListFragment

    @Inject lateinit var notificationRepository: NotificationRepository

    @Inject lateinit var taskViewModel: TaskViewModel

    @Inject lateinit var widgetUpdater: ForegroundListWidgetUpdater

    @Inject lateinit var sharedPrefs: SharedPreferences

    companion object {
        //intent action for opening a task
        const val BRIGHTTASK_OPEN_TASK = "me.bgregos.brighttask.OPEN_TASK"

        //intent extra property for opening a task
        const val BRIGHTTASK_OPEN_TASK_PARAM_UUID = "OPEN_TASK_PARAM_UUID"

        const val USER_DENIED_ALARMS_KEY = "deniedAlarms"
    }

    var permissionDialog: Dialog? = null

    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getApplicationComponent().inject(this)
        setContentView(R.layout.activity_main)
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        val taskUUIDToOpen = intent.getStringExtra(BRIGHTTASK_OPEN_TASK_PARAM_UUID)
        fragment = TaskListFragment.newInstance(taskUUIDToOpen)
        transaction.replace(R.id.task_list_container, fragment)
        transaction.commit()
        getApplicationComponent().inject(taskViewModel)
        val i = Intent(this.applicationContext, ForegroundListWidgetProvider::class.java)
        this.applicationContext.sendBroadcast(i)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                notificationRepository.createNotificationChannel()
                startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } else {
                sharedPrefs.edit().putBoolean(USER_DENIED_ALARMS_KEY, true).apply()
            }
            permissionDialog?.dismiss()
        }
        requestAlarmPermission()
    }

    private fun requestAlarmPermission() {
        val alarmManager: AlarmManager = getSystemService()!!
        Log.d("alarms", "checking alarm permission")
        val userDenied = sharedPrefs.getBoolean(USER_DENIED_ALARMS_KEY, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (!alarmManager.canScheduleExactAlarms() && !userDenied) {
                Log.d("alarms", "requesting alarm permission")
                permissionDialog = AlertDialog.Builder(this).run {
                    setPositiveButton(R.string.ok) { dialog, id ->
                        requestPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                    setNegativeButton(R.string.cancel) { dialog, id ->
                        sharedPrefs.edit().putBoolean(USER_DENIED_ALARMS_KEY, true).apply()
                        dialog.dismiss()
                    }
                    setCancelable(false)
                    setTitle(getString(R.string.notification_permission_dialog_title))
                    setMessage("Foreground uses Notification and Reminder permissions to send timely task due reminders.\n\nDenying notifications will prevent notifications, while denying reminders may cause late notifications.")
                    create()
                }
                permissionDialog?.show()

            } else {
                Log.d("alarms", "alarm permission already granted")
                notificationRepository.createNotificationChannel()
            }
        } else {
            notificationRepository.createNotificationChannel()
        }
    }

    override fun onResume() {
        super.onResume()
        widgetUpdater.updateWidget()
    }

    override fun onPause() {
        notificationRepository.save()
        widgetUpdater.updateWidget()
        super.onPause()
    }
}
