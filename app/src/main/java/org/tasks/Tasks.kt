package org.tasks

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.todoroo.astrid.service.Upgrader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavSynchronizer
import org.tasks.files.FileHelper
import org.tasks.injection.InjectingJobIntentService
import org.tasks.jobs.WorkManager
import org.tasks.locale.Locale
import org.tasks.location.GeofenceApi
import org.tasks.opentasks.OpenTaskContentObserver
import org.tasks.preferences.Preferences
import org.tasks.receivers.RefreshReceiver
import org.tasks.scheduling.CalendarNotificationIntentService
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.scheduling.RefreshScheduler
import org.tasks.themes.ThemeBase
import org.tasks.widget.AppWidgetManager
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Tasks : Application(), Configuration.Provider {

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var buildSetup: BuildSetup
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var upgrader: Lazy<Upgrader>       // TODO Q：Lazy<T> ??? 这是什么用法
    @Inject lateinit var workManager: Lazy<WorkManager>
    @Inject lateinit var refreshScheduler: Lazy<RefreshScheduler>
    @Inject lateinit var geofenceApi: Lazy<GeofenceApi>
    @Inject lateinit var appWidgetManager: Lazy<AppWidgetManager>
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var contentObserver: Lazy<OpenTaskContentObserver>

    override fun onCreate() {
        super.onCreate()
        //build相关的一些配置
        //TODO Q: 构造函数怎么初始的？Preferences?
        buildSetup.setup()
        //升级处理 See @Upgrader
        upgrade()
        //See @Preferences
        preferences.isSyncOngoing = false
        preferences.setBoolean(R.string.p_sync_ongoing_opentasks, false)
        //Theme
        ThemeBase.getThemeBase(preferences, inventory, null).setDefaultNightMode()
        //Broadcast
        localBroadcastManager.registerRefreshReceiver(RefreshBroadcastReceiver())
        //locale configuration
        Locale.getInstance(this).createConfigurationContext(applicationContext)
        //coroutine
        backgroundWork()
    }

    private fun upgrade() {
        val lastVersion = preferences.lastSetVersion
        val currentVersion = BuildConfig.VERSION_CODE
        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion)

        // invoke upgrade service
        if (lastVersion != currentVersion) {
            upgrader.get().upgrade(lastVersion, currentVersion)
            preferences.setDefaults()
        }
    }

    private fun backgroundWork() = CoroutineScope(Dispatchers.Default).launch {
        inventory.updateTasksAccount()
        NotificationSchedulerIntentService.enqueueWork(context, false)
        CalendarNotificationIntentService.enqueueWork(context)
        refreshScheduler.get().scheduleAll()
        workManager.get().apply {
            updateBackgroundSync()
            scheduleMidnightRefresh()
            scheduleBackup()
            scheduleConfigRefresh()
            OpenTaskContentObserver.registerObserver(context, contentObserver.get())
            updatePurchases()
        }
        geofenceApi.get().registerAll()
        FileHelper.delete(context, preferences.cacheDirectory)
        appWidgetManager.get().reconfigureWidgets()
        CaldavSynchronizer.registerFactories()
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    private class RefreshBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            JobIntentService.enqueueWork(
                    context,
                    RefreshReceiver::class.java,
                    InjectingJobIntentService.JOB_ID_REFRESH_RECEIVER,
                    intent)
        }
    }

    companion object {
        const val IS_GOOGLE_PLAY = BuildConfig.FLAVOR == "googleplay"
        const val IS_GENERIC = BuildConfig.FLAVOR == "generic"
    }
}